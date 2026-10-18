package com.leveledworld.mob;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.network.MobLevelPayload;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.RpgAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Sorteio, aplicação e herança dos tiers de raridade dos mobs. */
public final class MobTiers {
    private static final ResourceLocation HEALTH_ID = LeveledWorld.id("tier_health");
    private static final ResourceLocation DAMAGE_ID = LeveledWorld.id("tier_damage");
    private static final ResourceLocation SPEED_ID = LeveledWorld.id("tier_speed");
    private static final ResourceLocation JUMP_ID = LeveledWorld.id("tier_jump");

    /** Agressivos (que atacam o jogador) e animais (passivos, pets e montarias) têm tier; chefes não. */
    public static boolean canHaveTier(Entity entity) {
        if (!(entity instanceof Mob mob)) return false;
        if (mob instanceof WitherBoss || mob instanceof EnderDragon || mob instanceof Warden || mob instanceof ElderGuardian) {
            return false;
        }
        return mob instanceof Enemy || mob instanceof Animal;
    }

    public static boolean isAggressive(Entity entity) {
        return entity instanceof Enemy;
    }

    public static MobTier get(Entity entity) {
        return entity.hasData(RpgAttachments.MOB_TIER) ? MobTier.byIndex(entity.getData(RpgAttachments.MOB_TIER)) : MobTier.COMMON;
    }

    /** Sorteia o tier: pesos da config, com os tiers altos mais comuns conforme o level e a dimensão. */
    public static MobTier roll(ServerLevel level, Mob mob) {
        if (!LWConfig.TIERS_ENABLED.get()) return MobTier.COMMON;
        if (mob.getData(RpgAttachments.FROM_SPAWNER) && !LWConfig.TIER_SPAWNER_MOBS.get()) return MobTier.COMMON;
        DimensionRules.Rule rule = DimensionRules.of(level);
        int reference = MobScaling.getLevel(mob);
        if (reference <= 0) reference = nearestPlayerLevel(level, mob) + rule.levelBonus();
        reference += rule.lootLevelBonus();
        double boost = 1 + Math.max(0, reference) * LWConfig.TIER_LEVEL_BOOST.get();

        List<? extends Double> base = LWConfig.TIER_WEIGHTS.get();
        MobTier[] tiers = MobTier.values();
        double[] weights = new double[tiers.length];
        double total = 0;
        for (int i = 0; i < tiers.length; i++) {
            double w = i < base.size() ? base.get(i) : tiers[i].weight;
            weights[i] = w * Math.pow(boost, i);
            total += weights[i];
        }
        double r = mob.getRandom().nextDouble() * total;
        for (int i = 0; i < tiers.length; i++) {
            r -= weights[i];
            if (r < 0) return tiers[i];
        }
        return MobTier.COMMON;
    }

    private static int nearestPlayerLevel(ServerLevel level, Mob mob) {
        Player nearest = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(),
                LWConfig.PLAYER_SEARCH_RADIUS.get(), p -> !p.isSpectator());
        return nearest instanceof ServerPlayer sp ? LevelingService.get(sp).level() : 1;
    }

    /**
     * Grava o tier e aplica os atributos: agressivos ficam um pouco mais fortes; animais ganham vida (lobos também
     * dano) e montarias ganham velocidade e pulo. {@code fresh} = mob novo, que nasce com a vida cheia.
     */
    public static void apply(Mob mob, MobTier tier, boolean fresh) {
        mob.setData(RpgAttachments.MOB_TIER, tier.ordinal());
        float healthRatio = mob.getMaxHealth() > 0 ? mob.getHealth() / mob.getMaxHealth() : 1f;
        boolean aggressive = isAggressive(mob);
        double health = aggressive ? tier.strength : tier.pet;
        double damage = aggressive ? tier.strength : mob instanceof Wolf ? tier.pet : 0;
        set(mob, Attributes.MAX_HEALTH, HEALTH_ID, health);
        set(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, damage);
        if (mob instanceof AbstractHorse) {
            set(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, tier.mount);
            set(mob, Attributes.JUMP_STRENGTH, JUMP_ID, tier.mount);
        }
        mob.setHealth(fresh ? mob.getMaxHealth() : Math.max(1f, mob.getMaxHealth() * healthRatio));
    }

    private static void set(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (amount == 0) {
            instance.removeModifier(id);
        } else {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /**
     * Tier do filhote: sorteia um dos pais como base; 90% fica no tier dele, 9% sobe um e 1% desce um.
     * Ex.: dois pais raros → 90% raro, 9% épico, 1% incomum.
     */
    public static MobTier inherit(Entity parentA, Entity parentB, RandomSource random) {
        MobTier a = get(parentA);
        MobTier b = parentB != null ? get(parentB) : a;
        MobTier base = random.nextBoolean() ? a : b;
        double r = random.nextDouble();
        if (r < 0.90) return base;
        if (r < 0.99) return base.up();
        return base.down();
    }

    /** Reenvia level e tier aos jogadores que enxergam o mob (após mudança por comando). */
    public static void resync(Mob mob) {
        PacketDistributor.sendToPlayersTrackingEntity(mob,
                new MobLevelPayload(mob.getId(), MobScaling.getLevel(mob), get(mob).ordinal()));
    }

    private MobTiers() {}
}
