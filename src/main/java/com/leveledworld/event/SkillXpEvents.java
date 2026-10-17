package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.mob.MobScaling;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.PlacedBlocks;
import com.leveledworld.player.RpgAttachments;
import com.leveledworld.skill.Gear;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Skill;
import com.leveledworld.skill.Stat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;

/** Concede XP às skills conforme o jogador as usa, com regras de balanceamento e anti-farm. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class SkillXpEvents {

    /** XP por minério (base). Toque de Seda e blocos colocados por jogadores não dão XP. */
    private static final Map<Block, Double> ORE_XP = Map.ofEntries(
            Map.entry(Blocks.COAL_ORE, 2.0), Map.entry(Blocks.DEEPSLATE_COAL_ORE, 2.5),
            Map.entry(Blocks.COPPER_ORE, 2.0), Map.entry(Blocks.DEEPSLATE_COPPER_ORE, 2.5),
            Map.entry(Blocks.IRON_ORE, 4.0), Map.entry(Blocks.DEEPSLATE_IRON_ORE, 5.0),
            Map.entry(Blocks.LAPIS_ORE, 4.0), Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, 5.0),
            Map.entry(Blocks.REDSTONE_ORE, 4.0), Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, 5.0),
            Map.entry(Blocks.GOLD_ORE, 6.0), Map.entry(Blocks.DEEPSLATE_GOLD_ORE, 7.5), Map.entry(Blocks.NETHER_GOLD_ORE, 3.0),
            Map.entry(Blocks.NETHER_QUARTZ_ORE, 3.0),
            Map.entry(Blocks.EMERALD_ORE, 12.0), Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, 14.0),
            Map.entry(Blocks.DIAMOND_ORE, 15.0), Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, 18.0),
            Map.entry(Blocks.ANCIENT_DEBRIS, 25.0));

    // ---------- Combate ----------

    /** Fator de dificuldade: mobs acima do seu level dão mais XP, abaixo dão menos. */
    private static double difficulty(ServerPlayer player, LivingEntity victim) {
        double factor = 1.0;
        if (victim instanceof Mob mob) {
            int mobLevel = MobScaling.getLevel(mob);
            if (mobLevel > 0) {
                factor = Math.max(0.5, Math.min(2.0, (double) mobLevel / Math.max(1, LevelingService.get(player).level())));
            }
            if (mob.getData(RpgAttachments.FROM_SPAWNER)) factor *= LWConfig.SPAWNER_XP_MULTIPLIER.get();
        }
        return factor;
    }

    /** Skill treinada pelo golpe: espada/mão vazia → Uma Mão, machado/maça/tridente → Duas Mãos, projétil → Arquearia. */
    public static Skill weaponSkill(ServerPlayer attacker, DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile) return Skill.ARCHERY;
        if (direct != attacker) return null;
        return Gear.isTwoHanded(attacker.getMainHandItem()) ? Skill.TWO_HANDED : Skill.ONE_HANDED;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        // Dano excedente (ex.: golpe de 1000 num zumbi) não vira XP.
        float damage = Math.min(event.getNewDamage(), victim.getMaxHealth());
        if (damage <= 0) return;
        DamageSource source = event.getSource();

        if (source.getEntity() instanceof ServerPlayer attacker && victim != attacker) {
            Skill skill = weaponSkill(attacker, source);
            if (skill != null) {
                double mult = skill == Skill.ONE_HANDED && attacker.getMainHandItem().isEmpty() ? 0.5 : 1.0;
                if (skill == Skill.ARCHERY) mult = 1.2;
                LevelingService.addSkillXp(attacker, skill, damage * mult * difficulty(attacker, victim));
            }
        }

        // Armaduras: XP dividido entre leve e pesada conforme as peças usadas.
        if (victim instanceof ServerPlayer player && source.getEntity() != null && source.getEntity() != player) {
            int heavy = Gear.count(player, Gear.Weight.HEAVY);
            int light = Gear.count(player, Gear.Weight.LIGHT);
            if (heavy > 0) LevelingService.addSkillXp(player, Skill.HEAVY_ARMOR, damage * 0.5 * heavy);
            if (light > 0) LevelingService.addSkillXp(player, Skill.LIGHT_ARMOR, damage * 0.5 * light);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim == player) return;
        Skill skill = weaponSkill(player, event.getSource());
        if (skill != null) {
            LevelingService.addSkillXp(player, skill, victim.getMaxHealth() * 0.10 * difficulty(player, victim));
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && event.getEntity() instanceof ServerPlayer player
                && event.getDamageSource().getEntity() instanceof LivingEntity attacker) {
            double blocked = Math.min(Math.max(1.0, event.getBlockedDamage()), attacker.getMaxHealth());
            LevelingService.addSkillXp(player, Skill.BLOCK, blocked * 1.5 * difficulty(player, attacker));
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.SPAWNER || type == MobSpawnType.TRIAL_SPAWNER) {
            event.getEntity().setData(RpgAttachments.FROM_SPAWNER, true);
        }
    }

    // ---------- Mineração, Lenhador, Agricultura ----------

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!LWConfig.TRACK_PLACED_BLOCKS.get() || !(event.getEntity() instanceof ServerPlayer)) return;
        BlockState state = event.getPlacedBlock();
        if (isTrackable(state)) PlacedBlocks.mark((net.minecraft.world.level.Level) event.getLevel(), event.getPos());
    }

    public static boolean isTrackable(BlockState state) {
        return state.is(Tags.Blocks.ORES) || state.is(BlockTags.LOGS) || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK);
    }

    /** Posição do último bloco colocado por jogador que foi quebrado (os drops vêm logo depois e não ganham perks). */
    public static long lastPlacedBreak = Long.MIN_VALUE;

    /** Roda depois dos efeitos de perk (que precisam saber se o bloco foi colocado) e limpa a marcação. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.isCanceled()) return;
        net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) event.getLevel();
        BlockState state = event.getState();
        if (isTrackable(state) && PlacedBlocks.consume(level, event.getPos())) {
            lastPlacedBreak = event.getPos().asLong();
            return;
        }
        lastPlacedBreak = Long.MIN_VALUE;

        Double ore = ORE_XP.get(state.getBlock());
        if (ore == null && state.is(Tags.Blocks.ORES)) ore = 4.0;  // minérios de outros mods
        if (ore != null) {
            if (!silkTouch(player, state)) LevelingService.addSkillXp(player, Skill.MINING, ore);
        } else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            LevelingService.addSkillXp(player, Skill.MINING, 0.3);
        } else if (state.is(BlockTags.LOGS)) {
            LevelingService.addSkillXp(player, Skill.WOODCUTTING, 2.0 * fellMultiplier(player));
        } else if ((state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK))
                && PerkStats.has(player, Stat.MUSHROOM_LOGS)) {
            LevelingService.addSkillXp(player, Skill.WOODCUTTING, 1.0);
        } else if (isMatureCrop(state)) {
            LevelingService.addSkillXp(player, Skill.FARMING, 1.5 * farmMultiplier(player));
        } else if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            LevelingService.addSkillXp(player, Skill.FARMING, 2.0 * farmMultiplier(player));
        }
    }

    static double fellMultiplier(ServerPlayer player) {
        return PerkStats.has(player, Stat.FELL_XP) ? 2.0 : 1.0;
    }

    static double farmMultiplier(ServerPlayer player) {
        return PerkStats.has(player, Stat.FARM_MASTER) ? 2.0 : 1.0;
    }

    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= 2;
        return false;
    }

    private static boolean silkTouch(ServerPlayer player, BlockState state) {
        var lookup = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                lookup.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), player.getMainHandItem()) > 0;
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            LevelingService.addSkillXp(player, Skill.FARMING, 6.0 * farmMultiplier(player));
        }
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LevelingService.addSkillXp(player, Skill.FARMING, 4.0 * farmMultiplier(player));
        }
    }

    // ---------- Ferraria ----------

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getCrafting();
        double xp = smithingValue(stack);
        if (xp > 0) LevelingService.addSkillXp(player, Skill.SMITHING, xp * stack.getCount());
    }

    @SubscribeEvent
    public static void onAnvil(AnvilRepairEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LevelingService.addSkillXp(player, Skill.SMITHING, Math.max(4.0, smithingValue(event.getOutput()) * 0.5));
        }
    }

    /** XP de ferraria pelo material do item fabricado. */
    public static double smithingValue(ItemStack stack) {
        var item = stack.getItem();
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        boolean gear = item instanceof ArmorItem || item instanceof TieredItem || item instanceof ShieldItem
                || item instanceof BowItem || item instanceof CrossbowItem || stack.is(Items.MACE);
        if (!gear) return 0;
        if (name.startsWith("netherite")) return 25;
        if (name.startsWith("diamond")) return 15;
        if (name.startsWith("iron") || name.startsWith("chainmail") || stack.is(Items.MACE)) return 8;
        if (name.startsWith("golden")) return 6;
        if (item instanceof ShieldItem || item instanceof BowItem || item instanceof CrossbowItem) return 5;
        if (name.startsWith("leather") || name.startsWith("turtle")) return 3;
        return 2;  // madeira e pedra
    }

    private SkillXpEvents() {}
}
