package com.leveledworld.mob;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import java.util.ArrayList;
import java.util.List;

/** Mobs de level alto dão mais XP, mais drops e têm chance de loot bônus. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class MobRewardEvents {
    private static final ResourceKey<LootTable> COMMON = table("common");
    private static final ResourceKey<LootTable> UNCOMMON = table("uncommon");
    private static final ResourceKey<LootTable> RARE = table("rare");

    private static ResourceKey<LootTable> table(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, LeveledWorld.id("bonus/" + name));
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        int level = MobScaling.getLevel(mob);
        if (level <= 1) return;
        double multiplier = (1.0 + (level - 1) * LWConfig.XP_PER_LEVEL.get()) * DimensionRules.of(mob.level()).rewardMultiplier();
        event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * multiplier));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        int mobLevel = MobScaling.getLevel(mob);
        if (mobLevel <= 1) return;

        // Mais quantidade nos drops empilháveis (equipamento do mob fica como está).
        double bonus = (mobLevel - 1) * LWConfig.DROPS_PER_LEVEL.get();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.getMaxStackSize() <= 1) continue;
            double extra = stack.getCount() * bonus;
            int add = (int) extra + (mob.getRandom().nextDouble() < extra - (int) extra ? 1 : 0);
            if (add > 0) {
                ItemStack grown = stack.copy();
                grown.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() + add));
                drop.setItem(grown);
            }
        }

        // Loot bônus: só quando o jogador participou da morte.
        Player killer = mob.getLastHurtByMob() instanceof Player p ? p : null;
        if (killer == null && event.getSource().getEntity() instanceof Player p) killer = p;
        if (killer == null) return;

        DimensionRules.Rule rule = DimensionRules.of(level);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .withLuck(killer.getLuck())
                .create(LootContextParamSets.ENTITY);

        // Loot bônus por raridade; dimensões perigosas aumentam a chance e o "level de loot".
        int lootLevel = mobLevel + rule.lootLevelBonus();
        double chance = Math.min(LWConfig.BONUS_LOOT_MAX_CHANCE.get(),
                lootLevel * LWConfig.BONUS_LOOT_CHANCE_PER_LEVEL.get() * rule.rewardMultiplier());
        if (mob.getRandom().nextDouble() < chance) {
            Tier tier = lootLevel >= LWConfig.RARE_LOOT_LEVEL.get() ? Tier.RARE
                    : lootLevel >= LWConfig.UNCOMMON_LOOT_LEVEL.get() ? Tier.UNCOMMON : Tier.COMMON;
            boolean dropped = drop(event, level, mob, level.getServer().reloadableRegistries().getLootTable(tier.table),
                    params, tier != Tier.COMMON);
            if (dropped && tier == Tier.RARE) {
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, 1.2F);
            }
        }

        // Loot exclusivo da dimensão (Nether, End ou de outros mods, se houver tabela).
        if (rule.exclusiveChance() > 0 && mob.getRandom().nextDouble() < rule.exclusiveChance()) {
            LootTable exclusive = level.getServer().reloadableRegistries()
                    .getLootTable(DimensionRules.exclusiveTable(level.dimension().location()));
            if (drop(event, level, mob, exclusive, params, true)) {
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2F, 0.8F);
            }
        }
    }

    /** Rola a tabela e adiciona os itens aos drops; retorna se caiu algo. */
    private static boolean drop(LivingDropsEvent event, ServerLevel level, Mob mob, LootTable table,
                                LootParams params, boolean glow) {
        List<ItemStack> loot = new ArrayList<>(table.getRandomItems(params));
        for (ItemStack stack : loot) {
            ItemEntity entity = new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), stack);
            entity.setDefaultPickUpDelay();
            if (glow) entity.setGlowingTag(true);  // destaca loot incomum/raro/exclusivo
            event.getDrops().add(entity);
        }
        return !loot.isEmpty();
    }

    private enum Tier {
        COMMON(MobRewardEvents.COMMON), UNCOMMON(MobRewardEvents.UNCOMMON), RARE(MobRewardEvents.RARE);
        final ResourceKey<LootTable> table;
        Tier(ResourceKey<LootTable> table) { this.table = table; }
    }

    private MobRewardEvents() {}
}
