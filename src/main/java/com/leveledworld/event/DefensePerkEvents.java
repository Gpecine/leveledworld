package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.player.AttributeService;
import com.leveledworld.player.CombatState;
import com.leveledworld.skill.Gear;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Stat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Perks defensivos: Bloqueio, Armadura Pesada e Leve, quedas e resistências. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class DefensePerkEvents {
    /** Evita que reaplicar o efeito encurtado dispare a Resiliência de novo. */
    private static boolean resizing;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        boolean projectile = direct instanceof Projectile;
        boolean melee = attacker instanceof LivingEntity && direct == attacker;
        var random = player.getRandom();

        // Esquiva (Armadura Leve): precisa de 2+ peças leves.
        if ((melee || projectile) && Gear.count(player, Gear.Weight.LIGHT) >= 2) {
            double dodge = PerkStats.get(player, Stat.DODGE) + (projectile ? PerkStats.get(player, Stat.DODGE_PROJECTILE) : 0);
            if (dodge > 0 && random.nextDouble() < dodge) {
                event.setCanceled(true);
                CombatState.of(player).lastDodgeTick = player.level().getGameTime();
                AttributeService.restoreStamina(player, PerkStats.get(player, Stat.DODGE_STAMINA));
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(0.5), player.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
                    level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1f, 1.4f);
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.leveledworld.dodged")
                        .withStyle(net.minecraft.ChatFormatting.AQUA), true);
                return;
            }
        }

        // Fortaleza Imortal: segurando escudo (mesmo abaixado), chance de aparar golpes frontais.
        if ((melee || projectile) && !player.isBlocking() && Gear.holdingShield(player)
                && random.nextDouble() < PerkStats.get(player, Stat.BLOCK_PASSIVE) && isFrontal(player, direct)) {
            event.setCanceled(true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 1f);
            return;
        }

        float amount = event.getAmount();
        int heavy = Gear.count(player, Gear.Weight.HEAVY);
        if (player.isBlocking() && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION))) {
            amount *= (float) (1 - PerkStats.get(player, Stat.SHIELD_ELEMENTAL));
        }
        if (projectile) {
            amount *= (float) (1 - PerkStats.get(player, Stat.HEAVY_PROJ_DR) * heavy / 4.0);
            if (player.isCrouching()) amount *= (float) (1 - PerkStats.get(player, Stat.SNEAK_PROJ_DR));
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= (float) (1 - PerkStats.get(player, Stat.HEAVY_EXPLOSION_DR) * heavy / 4.0);
        if (melee) amount *= (float) (1 - PerkStats.get(player, Stat.HEAVY_MELEE_DR) * heavy / 4.0);
        if (heavy == 4 && attacker != null) amount *= (float) (1 - PerkStats.get(player, Stat.HEAVY_SET_DR));
        if (source.is(DamageTypeTags.IS_FALL)) {
            double fallDr = PerkStats.get(player, Stat.FALL_DR);
            if (player.isCrouching() && Gear.count(player, Gear.Weight.LIGHT) >= 2) fallDr += PerkStats.get(player, Stat.SNEAK_FALL_DR);
            amount *= (float) (1 - Math.min(0.9, fallDr));
        }
        event.setAmount(amount);
    }

    private static boolean isFrontal(ServerPlayer player, Entity from) {
        if (from == null) return false;
        Vec3 look = player.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 to = from.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        return look.dot(to) > 0.2;
    }

    /** Refletir (Armadura Pesada): chance de devolver metade do dano corpo a corpo. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0) return;
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != player
                && Gear.count(player, Gear.Weight.HEAVY) >= 2
                && player.getRandom().nextDouble() < PerkStats.get(player, Stat.HEAVY_REFLECT)) {
            attacker.hurt(player.damageSources().thorns(player), event.getNewDamage() * 0.5f);
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player)) return;
        CombatState state = CombatState.of(player);
        state.lastBlockTick = player.level().getGameTime();
        var random = player.getRandom();
        AttributeService.restoreStamina(player, PerkStats.get(player, Stat.BLOCK_STAMINA));
        if (random.nextDouble() < PerkStats.get(player, Stat.SHIELD_DURABILITY)) event.setShieldDamage(0);
        if (PerkStats.has(player, Stat.BLOCK_RESISTANCE)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, true));
        }
        long now = player.level().getGameTime();
        if (PerkStats.has(player, Stat.BLOCK_LAST_STAND) && player.getHealth() / player.getMaxHealth() < 0.25f
                && now - state.lastLastStandTick > 200) {
            state.lastLastStandTick = now;
            player.heal(2f);
        }

        DamageSource source = event.getDamageSource();
        Entity direct = source.getDirectEntity();
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player) return;
        float blocked = event.getBlockedDamage();
        if (direct instanceof Projectile) {
            if (PerkStats.has(player, Stat.BLOCK_DEFLECT) && blocked > 0) {
                attacker.hurt(player.damageSources().thorns(player), blocked);
            }
            return;
        }
        double reflect = PerkStats.get(player, Stat.BLOCK_REFLECT);
        if (reflect > 0 && blocked > 0) attacker.hurt(player.damageSources().thorns(player), (float) (blocked * reflect));
        if (PerkStats.has(player, Stat.BLOCK_KNOCKBACK)) {
            attacker.knockback(0.8, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
        }
        if (PerkStats.has(player, Stat.BLOCK_STUN)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), player);
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), player);
        }
    }

    /** Inquebrável: armadura pesada gasta menos durabilidade. */
    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double save = PerkStats.get(player, Stat.HEAVY_DURABILITY);
        if (save <= 0) return;
        for (EquipmentSlot slot : event.getArmorMap().keySet()) {
            ItemStack stack = event.getArmorItemStack(slot);
            if (Gear.weight(stack) == Gear.Weight.HEAVY) {
                event.setNewDamage(slot, (float) (event.getNewDamage(slot) * (1 - save)));
            }
        }
    }

    /** Resiliência: efeitos negativos duram menos (encurtados no próximo tick). */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (resizing || !(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance instance = event.getEffectInstance();
        if (instance.getEffect().value().getCategory() != net.minecraft.world.effect.MobEffectCategory.HARMFUL) return;
        if (PerkStats.has(player, Stat.HEAVY_DEBUFF_RES) && Gear.count(player, Gear.Weight.HEAVY) >= 2 && !instance.isInfiniteDuration()) {
            CombatState.of(player).pendingDebuffs.add(instance.getEffect());
        }
    }

    /** Chamado no tick do jogador para aplicar a redução da Resiliência. */
    public static void tickDebuffs(ServerPlayer player) {
        CombatState state = CombatState.of(player);
        if (state.pendingDebuffs.isEmpty()) return;
        double keep = 1 - PerkStats.get(player, Stat.HEAVY_DEBUFF_RES);
        for (var effect : state.pendingDebuffs) {
            MobEffectInstance current = player.getEffect(effect);
            if (current == null) continue;
            int duration = (int) (current.getDuration() * keep);
            MobEffectInstance shorter = new MobEffectInstance(effect, duration, current.getAmplifier(),
                    current.isAmbient(), current.isVisible(), current.showIcon());
            resizing = true;
            try {
                player.removeEffect(effect);
                player.addEffect(shorter);
            } finally {
                resizing = false;
            }
        }
        state.pendingDebuffs.clear();
    }

    private DefensePerkEvents() {}
}
