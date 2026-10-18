package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.player.CombatState;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Skill;
import com.leveledworld.skill.Stat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/** Perks de Agricultura. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class FarmingPerkEvents {

    // ---------- colheita ----------

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || event.getDrops().isEmpty()) return;
        if (!SkillXpEvents.isMatureCrop(event.getState())) return;
        boostHarvest(player, event.getDrops(), event.getLevel(), event.getPos());
    }

    /** Colheita extra, safra dourada e bônus do Senhor da Terra sobre a lista de drops de uma planta madura. */
    private static void boostHarvest(ServerPlayer player, List<ItemEntity> drops, ServerLevel level, BlockPos pos) {
        var random = player.getRandom();
        ItemEntity produce = drops.stream().filter(d -> !(d.getItem().getItem() instanceof BlockItem) || drops.size() == 1)
                .findFirst().orElse(drops.get(0));
        int extra = 0;
        if (random.nextDouble() < PerkStats.get(player, Stat.HARVEST_EXTRA)) extra++;
        if (PerkStats.has(player, Stat.FARM_MASTER)) extra++;
        if (extra > 0) {
            ItemStack more = produce.getItem().copyWithCount(extra);
            drops.add(item(level, pos, more));
        }
        if (random.nextDouble() < PerkStats.get(player, Stat.GOLDEN_HARVEST)) {
            drops.add(item(level, pos, new ItemStack(random.nextBoolean() ? Items.GOLDEN_CARROT : Items.GOLDEN_APPLE)));
        }
    }

    private static ItemEntity item(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity e = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        e.setDefaultPickUpDelay();
        return e;
    }

    /** Replantio: clique direito numa planta madura colhe e replanta (agachado: área 3×3). */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!PerkStats.has(player, Stat.RIGHT_CLICK_HARVEST) || player.getMainHandItem().is(Items.BONE_MEAL)) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = event.getPos();
        if (!SkillXpEvents.isMatureCrop(level.getBlockState(center))) return;
        int r = CombatState.areaAction(player) && PerkStats.has(player, Stat.AREA_HARVEST) ? 1 : 0;
        int harvested = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, 0, -r), center.offset(r, 0, r))) {
            BlockState state = level.getBlockState(p);
            if (!SkillXpEvents.isMatureCrop(state)) continue;
            harvest(player, level, p.immutable(), state);
            harvested++;
        }
        if (harvested > 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            player.swing(InteractionHand.MAIN_HAND, true);
            level.playSound(null, center, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1f, 1f);
            LevelingService.addSkillXp(player, Skill.FARMING, 1.5 * harvested * (PerkStats.has(player, Stat.FARM_MASTER) ? 2 : 1));
        }
    }

    private static void harvest(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> stacks = Block.getDrops(state, level, pos, null, player, player.getMainHandItem());
        // Gasta uma "semente" dos drops para replantar.
        net.minecraft.world.item.Item seed = state.getBlock().asItem();
        for (ItemStack s : stacks) {
            if (s.is(seed)) { s.shrink(1); break; }
        }
        List<ItemEntity> drops = new java.util.ArrayList<>();
        for (ItemStack s : stacks) if (!s.isEmpty()) drops.add(item(level, pos, s));
        if (!drops.isEmpty()) boostHarvest(player, drops, level, pos);
        drops.forEach(level::addFreshEntity);
        level.setBlockAndUpdate(pos, replanted(state));
    }

    private static BlockState replanted(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.getStateForAge(0);
        if (block instanceof NetherWartBlock) return state.setValue(NetherWartBlock.AGE, 0);
        if (block instanceof CocoaBlock) return state.setValue(CocoaBlock.AGE, 0);
        return state;
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.isValidBonemealTarget()
                && player.getRandom().nextDouble() < PerkStats.get(player, Stat.BONEMEAL_SAVE)) {
            ItemStack back = new ItemStack(Items.BONE_MEAL);
            if (!player.getInventory().add(back)) player.drop(back, false);
        }
    }

    // ---------- aura de crescimento e filhotes ----------

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ServerLevel level = (ServerLevel) player.level();
        int radius = (int) PerkStats.get(player, Stat.CROP_AURA);
        if (radius > 0) {
            double speed = PerkStats.get(player, Stat.CROP_AURA_SPEED);
            var random = player.getRandom();
            int tries = radius * radius;
            for (int i = 0; i < tries; i++) {
                BlockPos p = player.blockPosition().offset(random.nextInt(radius * 2 + 1) - radius,
                        random.nextInt(5) - 2, random.nextInt(radius * 2 + 1) - radius);
                BlockState state = level.getBlockState(p);
                if (state.is(net.minecraft.tags.BlockTags.CROPS) && state.isRandomlyTicking() && random.nextDouble() < speed) {
                    state.randomTick(level, p, random);
                }
            }
        }
        if (PerkStats.has(player, Stat.BABY_GROWTH) && player.tickCount % 40 == 0) {
            for (AgeableMob baby : level.getEntitiesOfClass(AgeableMob.class, player.getBoundingBox().inflate(12), AgeableMob::isBaby)) {
                baby.ageUp(1, false);
            }
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || event.getChild() == null) return;
        if (player.getRandom().nextDouble() >= PerkStats.get(player, Stat.TWINS)) return;
        if (!(event.getParentA() instanceof Animal parent) || !(event.getParentB() instanceof AgeableMob other)) return;
        ServerLevel level = (ServerLevel) player.level();
        AgeableMob twin = parent.getBreedOffspring(level, other);
        if (twin == null) return;
        twin.setBaby(true);
        twin.moveTo(parent.getX(), parent.getY(), parent.getZ(), 0, 0);
        twin.finalizeSpawn(level, level.getCurrentDifficultyAt(parent.blockPosition()), MobSpawnType.BREEDING, null);
        // O gêmeo segue a mesma regra de herança de tier do filhote principal.
        if (com.leveledworld.mob.MobTiers.canHaveTier(twin)) {
            com.leveledworld.mob.MobTiers.apply(twin, com.leveledworld.mob.MobTiers.inherit(parent, other, twin.getRandom()), true);
        }
        level.addFreshEntity(twin);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.leveledworld.twins")
                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
    }

    // ---------- animais ----------

    @SubscribeEvent
    public static void onAnimalDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        int extra = (int) PerkStats.get(player, Stat.MEAT_EXTRA);
        if (extra <= 0) return;
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().has(net.minecraft.core.component.DataComponents.FOOD)) {
                drop.getItem().grow(extra);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onShear(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof Sheep sheep)) return;
        if (!event.getItemStack().is(Tags.Items.TOOLS_SHEAR) || !sheep.readyForShearing()) return;
        LevelingService.addSkillXp(player, Skill.FARMING, 2.0);
        int extra = (int) PerkStats.get(player, Stat.WOOL_EXTRA);
        if (extra > 0) {
            ItemStack wool = new ItemStack(net.minecraft.world.level.block.Blocks.WHITE_WOOL.asItem(), extra);
            var woolBlock = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace(sheep.getColor().getName() + "_wool"));
            if (woolBlock != Items.AIR) wool = new ItemStack(woolBlock, extra);
            sheep.spawnAtLocation(wool, 1);
        }
    }

    // ---------- comida e pesca ----------

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null) return;
        double bonus = PerkStats.get(player, Stat.FOOD_BONUS);
        int extra = (int) Math.round(food.nutrition() * bonus);
        if (extra > 0) player.getFoodData().eat(extra, 0.1f);
        if (PerkStats.has(player, Stat.FOOD_REGEN)) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDrops().isEmpty()) return;
        if (player.getRandom().nextDouble() < PerkStats.get(player, Stat.FISHING_EXTRA)) {
            ItemStack extra = event.getDrops().get(0).copy();
            if (!player.getInventory().add(extra)) player.drop(extra, false);
        }
    }

    private FarmingPerkEvents() {}
}
