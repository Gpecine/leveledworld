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
        double multiplier = 1.0 + (level - 1) * LWConfig.XP_PER_LEVEL.get();
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

        double chance = Math.min(LWConfig.BONUS_LOOT_MAX_CHANCE.get(), mobLevel * LWConfig.BONUS_LOOT_CHANCE_PER_LEVEL.get());
        if (mob.getRandom().nextDouble() >= chance) return;

        Tier tier = mobLevel >= LWConfig.RARE_LOOT_LEVEL.get() ? Tier.RARE
                : mobLevel >= LWConfig.UNCOMMON_LOOT_LEVEL.get() ? Tier.UNCOMMON : Tier.COMMON;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(tier.table);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .withLuck(killer.getLuck())
                .create(LootContextParamSets.ENTITY);

        List<ItemStack> loot = new ArrayList<>(table.getRandomItems(params));
        for (ItemStack stack : loot) {
            ItemEntity entity = new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), stack);
            entity.setDefaultPickUpDelay();
            if (tier != Tier.COMMON) entity.setGlowingTag(true);  // destaca loot incomum/raro
            event.getDrops().add(entity);
        }
        if (!loot.isEmpty() && tier == Tier.RARE) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, 1.2F);
        }
    }

    private enum Tier {
        COMMON(MobRewardEvents.COMMON), UNCOMMON(MobRewardEvents.UNCOMMON), RARE(MobRewardEvents.RARE);
        final ResourceKey<LootTable> table;
        Tier(ResourceKey<LootTable> table) { this.table = table; }
    }

    private MobRewardEvents() {}
}
