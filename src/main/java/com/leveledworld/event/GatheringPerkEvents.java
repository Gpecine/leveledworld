package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.player.CombatState;
import com.leveledworld.player.PlacedBlocks;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Stat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Perks de Mineração e Lenhador. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class GatheringPerkEvents {
    private static final Map<Item, Item> SMELT = Map.of(Items.RAW_IRON, Items.IRON_INGOT, Items.RAW_GOLD, Items.GOLD_INGOT,
            Items.RAW_COPPER, Items.COPPER_INGOT);
    private static boolean chaining;
    /** Contornos brilhantes do Faro de Minério, removidos depois de 5s. */
    private static final List<Marker> MARKERS = new ArrayList<>();

    private record Marker(Entity entity, long expires) {}

    /** Tarefas para o próximo tick do servidor (ex.: replantar depois que o tronco sumiu). */
    private static final List<Runnable> NEXT_TICK = new ArrayList<>();

    // ---------- velocidade (roda nos dois lados para a previsão do cliente bater) ----------

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        double bonus = 0;
        if (tool.is(ItemTags.PICKAXES) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            bonus += PerkStats.get(player, Stat.MINING_SPEED);
            if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE)) {
                bonus += PerkStats.get(player, Stat.STONE_SPEED);
            }
        } else if (tool.is(ItemTags.AXES) && (state.is(BlockTags.LOGS) || isMushroomLog(player, state))) {
            bonus += PerkStats.get(player, Stat.WOOD_SPEED);
        }
        if (bonus > 0) event.setNewSpeed((float) (event.getNewSpeed() * (1 + bonus)));
    }

    private static boolean isMushroomLog(Player player, BlockState state) {
        return PerkStats.has(player, Stat.MUSHROOM_LOGS)
                && (state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK));
    }

    // ---------- quebrar blocos ----------

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || chaining) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        boolean placed = PlacedBlocks.isPlaced(level, pos);
        var random = player.getRandom();

        if (tool.is(ItemTags.PICKAXES) && tool.isDamaged() && random.nextDouble() < PerkStats.get(player, Stat.PICK_DURABILITY)) {
            tool.setDamageValue(tool.getDamageValue() - 1);
        }
        if (tool.is(ItemTags.AXES) && tool.isDamaged() && random.nextDouble() < PerkStats.get(player, Stat.AXE_DURABILITY)) {
            tool.setDamageValue(tool.getDamageValue() - 1);
        }

        boolean ore = state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS);
        if (ore && !placed) {
            int radius = (int) PerkStats.get(player, Stat.ORE_SENSE);
            if (radius > 0) senseOres(level, pos, state, radius, radius >= 8);
        }
        // Veio inteiro também em minérios colocados: cada bloco da corrente passa pelo anti-farm (sem XP nem perks
        // de drop nos colocados), então não há o que explorar.
        if (ore) {
            int vein = (int) PerkStats.get(player, Stat.VEIN_MINE);
            if (vein > 0 && CombatState.areaAction(player) && tool.is(ItemTags.PICKAXES)) {
                breakConnected(player, level, pos, vein, s -> s.getBlock() == state.getBlock(), false, true);
            }
        }
        if (state.is(BlockTags.LOGS) && !placed) {
            if (PerkStats.has(player, Stat.CHOP_HASTE)) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false, true));
            if (PerkStats.has(player, Stat.TREE_FELL) && CombatState.areaAction(player) && tool.is(ItemTags.AXES) && isTree(level, pos)) {
                int beforeDamage = tool.getDamageValue();
                List<BlockPos> broken = breakConnected(player, level, pos, 64, s -> s.is(BlockTags.LOGS), true, false);
                if (PerkStats.has(player, Stat.FELL_FREE) && !tool.isEmpty()) tool.setDamageValue(beforeDamage);
                if (PerkStats.has(player, Stat.AUTO_REPLANT)) NEXT_TICK.add(() -> replant(level, pos, state, broken));
            }
        }
    }

    /**
     * Quebra blocos conectados (veio de minério ou árvore) como se o jogador os quebrasse. Sem
     * {@code includePlaced}, pula blocos colocados por jogadores (troncos de construções ao lado da árvore).
     */
    private static List<BlockPos> breakConnected(ServerPlayer player, ServerLevel level, BlockPos origin, int max,
                                                 java.util.function.Predicate<BlockState> match, boolean diagonal,
                                                 boolean includePlaced) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && found.size() < max) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (!diagonal && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                BlockPos n = p.offset(dx, dy, dz);
                if (seen.add(n) && match.test(level.getBlockState(n)) && (includePlaced || !PlacedBlocks.isPlaced(level, n))) {
                    found.add(n);
                    queue.add(n);
                    if (found.size() >= max) break;
                }
            }
        }
        chaining = true;
        try {
            for (BlockPos p : found) {
                if (player.getMainHandItem().isEmpty()) break;  // ferramenta quebrou
                player.gameMode.destroyBlock(p);
            }
        } finally {
            chaining = false;
        }
        return found;
    }

    /** Só derruba se houver folhas perto (evita derrubar construções de madeira). */
    private static boolean isTree(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 12, 3))) {
            if (level.getBlockState(p).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

    private static void replant(ServerLevel level, BlockPos origin, BlockState log, List<BlockPos> broken) {
        BlockPos base = origin;
        for (BlockPos p : broken) if (p.getY() < base.getY()) base = p;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String path = id.getPath().replace("stripped_", "");
        String sapling = path.endsWith("_stem") ? path.replace("_stem", "_fungus")
                : path.equals("mangrove_log") ? "mangrove_propagule" : path.replace("_log", "_sapling").replace("_wood", "_sapling");
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), sapling));
        if (block == Blocks.AIR) return;
        BlockState plant = block.defaultBlockState();
        if (level.getBlockState(base).isAir() && plant.canSurvive(level, base)) level.setBlockAndUpdate(base, plant);
    }

    private static void senseOres(ServerLevel level, BlockPos center, BlockState state, int radius, boolean deep) {
        int shown = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (p.equals(center)) continue;
            BlockState s = level.getBlockState(p);
            boolean match = s.getBlock() == state.getBlock()
                    || deep && (s.is(Tags.Blocks.ORES_DIAMOND) || s.is(Blocks.ANCIENT_DEBRIS));
            if (!match) continue;
            CompoundTag tag = new CompoundTag();
            tag.putString("id", "minecraft:block_display");
            tag.put("block_state", NbtUtils.writeBlockState(s));
            tag.putBoolean("Glowing", true);
            EntityType.create(tag, level).ifPresent(e -> {
                e.setPos(p.getX(), p.getY(), p.getZ());
                level.addFreshEntity(e);
                MARKERS.add(new Marker(e, level.getGameTime() + 100));
            });
            if (++shown >= 16) break;
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!NEXT_TICK.isEmpty()) {
            List<Runnable> tasks = new ArrayList<>(NEXT_TICK);
            NEXT_TICK.clear();
            tasks.forEach(Runnable::run);
        }
        if (MARKERS.isEmpty()) return;
        Iterator<Marker> it = MARKERS.iterator();
        while (it.hasNext()) {
            Marker m = it.next();
            if (!m.entity().isAlive() || m.entity().level().getGameTime() >= m.expires()) {
                m.entity().discard();
                it.remove();
            }
        }
    }

    // ---------- drops ----------

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || event.getDrops().isEmpty()) return;
        BlockState state = event.getState();
        if (SkillXpEvents.isTrackable(state) && event.getPos().asLong() == SkillXpEvents.lastPlacedBreak) return;
        var random = player.getRandom();
        ServerLevel level = event.getLevel();

        if (state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS)) {
            boolean silk = event.getDrops().stream().anyMatch(d -> d.getItem().is(state.getBlock().asItem()));
            if (!silk) {
                if (random.nextDouble() < PerkStats.get(player, Stat.ORE_DOUBLE)) duplicate(event);
                double smelt = PerkStats.get(player, Stat.AUTO_SMELT);
                for (ItemEntity drop : event.getDrops()) {
                    Item result = SMELT.get(drop.getItem().getItem());
                    if (result != null && random.nextDouble() < smelt) drop.setItem(new ItemStack(result, drop.getItem().getCount()));
                }
                double xp = PerkStats.get(player, Stat.ORE_XP);
                if (xp > 0) event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * (1 + xp)));
            }
            if (PerkStats.has(player, Stat.MINING_NO_HUNGER)) refundExhaustion(player);
        } else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            if (random.nextDouble() < PerkStats.get(player, Stat.GEM_FIND)) {
                add(event, level, new ItemStack(random.nextFloat() < 0.2f ? Items.DIAMOND : Items.EMERALD));
            }
            if (PerkStats.has(player, Stat.MINING_NO_HUNGER)) refundExhaustion(player);
        } else if (state.is(BlockTags.LOGS) || isMushroomLog(player, state)) {
            if (random.nextDouble() < PerkStats.get(player, Stat.LOG_EXTRA)) duplicate(event);
        } else if (state.is(BlockTags.LEAVES)) {
            if (random.nextDouble() < PerkStats.get(player, Stat.LEAF_DROPS)) duplicate(event);
            if (random.nextDouble() < PerkStats.get(player, Stat.GOLDEN_APPLE)) add(event, level, new ItemStack(Items.GOLDEN_APPLE));
        }
    }

    private static void refundExhaustion(ServerPlayer player) {
        var food = player.getFoodData();
        food.setExhaustion(Math.max(0, food.getExhaustionLevel() - 0.005f));
    }

    static void duplicate(BlockDropsEvent event) {
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemEntity copy = new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
            copy.setDefaultPickUpDelay();
            extra.add(copy);
        }
        event.getDrops().addAll(extra);
    }

    static void add(BlockDropsEvent event, ServerLevel level, ItemStack stack) {
        BlockPos p = event.getPos();
        ItemEntity e = new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, stack);
        e.setDefaultPickUpDelay();
        event.getDrops().add(e);
    }

    // ---------- outros ----------

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (PerkStats.has(player, Stat.CAVE_VISION) && player.getY() < 0) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
            }
        }
        if (PerkStats.has(player, Stat.HEAVY_REPAIR) && player.tickCount % 200 == 0) {
            for (ItemStack armor : player.getArmorSlots()) {
                if (com.leveledworld.skill.Gear.weight(armor) == com.leveledworld.skill.Gear.Weight.HEAVY && armor.isDamaged()) {
                    armor.setDamageValue(armor.getDamageValue() - 1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getCrafting();
        if (result.is(ItemTags.PLANKS) && PerkStats.has(player, Stat.PLANKS_BONUS)) {
            ItemStack bonus = new ItemStack(result.getItem(), Math.max(1, result.getCount() / 4));
            if (!player.getInventory().add(bonus)) player.drop(bonus, false);
        }
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.APPLE)) {
            double heal = PerkStats.get(player, Stat.APPLE_HEAL);
            if (heal > 0) player.heal((float) heal);
        }
    }

    private GatheringPerkEvents() {}
}
