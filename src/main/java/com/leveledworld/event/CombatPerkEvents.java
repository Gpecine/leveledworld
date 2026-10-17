package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.RpgEffects;
import com.leveledworld.player.AttributeService;
import com.leveledworld.player.CombatState;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.Gear;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Skill;
import com.leveledworld.skill.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.List;
import java.util.UUID;

/** Perks ofensivos: Uma Mão, Duas Mãos, Arquearia e ataques furtivos. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class CombatPerkEvents {
    private static final String SNEAK_KILL_TAG = "leveledworld:sneak_hit";
    private static final String EXTRA_ARROW_TAG = "leveledworld:extra_arrow";
    private static boolean cleaving;

    private static long now(Player p) {
        return p.level().getGameTime();
    }

    /** O alvo não está mirando no jogador: ataque furtivo (bosses nunca contam). */
    public static boolean isSneakAttack(Player attacker, LivingEntity target) {
        if (!(target instanceof Mob mob) || target instanceof EnderDragon || target instanceof WitherBoss) return false;
        return mob.getTarget() != attacker;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        CombatState.of(event.getEntity()).lastAttackStrength = event.getEntity().getAttackStrengthScale(0.5F);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        LivingEntity target = event.getEntity();
        if (target == attacker) return;
        Entity direct = source.getDirectEntity();
        boolean melee = direct == attacker;
        boolean projectile = direct instanceof Projectile;
        if (!melee && !projectile) return;

        CombatState state = CombatState.of(attacker);
        ItemStack weapon = attacker.getMainHandItem();
        double bonus = 0;
        double critChance = 0;
        double critMult = 1.5;
        double armorPen = 0;
        boolean sneak = isSneakAttack(attacker, target);
        double sneakMult = 1;
        float hpFraction = target.getHealth() / target.getMaxHealth();

        if (melee) {
            if (Gear.isSword(weapon)) {
                bonus += PerkStats.get(attacker, Stat.SWORD_DAMAGE);
                if (attacker.getHealth() / attacker.getMaxHealth() < 0.3f) bonus += PerkStats.get(attacker, Stat.SWORD_RAGE);
                if (hpFraction < 0.25f) bonus += PerkStats.get(attacker, Stat.SWORD_EXECUTE);
                if (attacker.isSprinting()) bonus += PerkStats.get(attacker, Stat.SWORD_CHARGE);
                double combo = PerkStats.get(attacker, Stat.SWORD_COMBO);
                if (combo > 0) {
                    UUID id = target.getUUID();
                    if (id.equals(state.comboTarget) && now(attacker) - state.lastComboTick < 60) {
                        state.comboCount = Math.min(5, state.comboCount + 1);
                    } else {
                        state.comboCount = 0;
                    }
                    state.comboTarget = id;
                    state.lastComboTick = now(attacker);
                    bonus += combo * state.comboCount;
                }
                critChance = PerkStats.get(attacker, Stat.SWORD_CRIT);
                critMult = Math.max(1.5, PerkStats.get(attacker, Stat.SWORD_CRIT_MULT));
            } else if (Gear.isTwoHanded(weapon)) {
                bonus += PerkStats.get(attacker, Stat.HEAVY_DAMAGE);
                if (weapon.is(ItemTags.AXES)) bonus += PerkStats.get(attacker, Stat.AXE_COMBAT);
                armorPen = PerkStats.get(attacker, Stat.HEAVY_ARMOR_PEN);
                critChance = PerkStats.get(attacker, Stat.HEAVY_CRIT);
                critMult = Math.max(1.5, PerkStats.get(attacker, Stat.HEAVY_CRIT_MULT));
            }
            if (now(attacker) - state.lastBlockTick <= 40) bonus += PerkStats.get(attacker, Stat.COUNTER);
            if (now(attacker) - state.lastDodgeTick <= 60) bonus += PerkStats.get(attacker, Stat.DODGE_COUNTER);
            if (sneak) {
                sneakMult = Math.max(2.0, PerkStats.get(attacker, Stat.SNEAK_MELEE_MULT));
                if (Gear.isSword(weapon)) sneakMult = Math.max(sneakMult, PerkStats.get(attacker, Stat.SNEAK_SWORD_MULT));
            }
        } else {
            bonus += PerkStats.get(attacker, Stat.RANGED_DAMAGE);
            if (direct instanceof ThrownTrident) bonus += PerkStats.get(attacker, Stat.TRIDENT_THROWN);
            if (direct instanceof AbstractArrow arrow && arrow.getWeaponItem() != null && arrow.getWeaponItem().is(Items.CROSSBOW)) {
                bonus += PerkStats.get(attacker, Stat.CROSSBOW_DAMAGE);
            }
            if (isBeast(target)) bonus += PerkStats.get(attacker, Stat.RANGED_VS_BEASTS);
            if (direct.getY() >= target.getEyeY() - 0.3) bonus += PerkStats.get(attacker, Stat.HEADSHOT);
            if (attacker.distanceTo(target) > 20) bonus += PerkStats.get(attacker, Stat.LONG_SHOT);
            armorPen = PerkStats.get(attacker, Stat.RANGED_ARMOR_PEN);
            critChance = PerkStats.get(attacker, Stat.RANGED_CRIT);
            if (sneak) sneakMult = Math.max(1.5, PerkStats.get(attacker, Stat.SNEAK_RANGED_MULT));
        }

        float amount = (float) (event.getAmount() * (1 + bonus));
        boolean crit = critChance > 0 && attacker.getRandom().nextDouble() < critChance;
        if (crit) {
            amount *= (float) critMult;
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 14, 0.3, 0.3, 0.3, 0.25);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f);
            }
        }
        if (melee && Gear.isSword(weapon) && attacker.getRandom().nextDouble() < PerkStats.get(attacker, Stat.SWORD_DOUBLE)) {
            amount *= 2;  // golpe duplo
        }
        if (sneakMult > 1) {
            amount *= (float) sneakMult;
            target.getPersistentData().putBoolean(SNEAK_KILL_TAG, true);
            attacker.displayClientMessage(Component.translatable("message.leveledworld.sneak_attack",
                    String.format(java.util.Locale.ROOT, "%.1f", sneakMult)).withStyle(ChatFormatting.DARK_PURPLE), true);
            LevelingService.addSkillXp(attacker, Skill.SNEAK, Math.min(amount, target.getMaxHealth()) * 0.8);
            if (PerkStats.has(attacker, Stat.SNEAK_POISON)) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), attacker);
        }
        event.setAmount(amount);

        if (armorPen > 0) {
            float keep = (float) (1 - armorPen);
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
        }
    }

    private static boolean isBeast(LivingEntity e) {
        return e instanceof Animal || e instanceof FlyingMob || e instanceof Blaze || e instanceof Vex || e instanceof Bat;
    }

    /** Efeitos depois do acerto: sangramento, veneno, lentidão, empurrão, golpe em área, stamina. */
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker) || event.getNewDamage() <= 0) return;
        LivingEntity target = event.getEntity();
        if (target == attacker) return;
        Entity direct = source.getDirectEntity();
        var random = attacker.getRandom();
        ItemStack weapon = attacker.getMainHandItem();

        if (direct == attacker) {
            if (Gear.isSword(weapon)) {
                if (random.nextDouble() < PerkStats.get(attacker, Stat.SWORD_BLEED)) {
                    bleed(target, PerkStats.has(attacker, Stat.SWORD_BLEED_STRONG), attacker);
                }
                if (random.nextDouble() < PerkStats.get(attacker, Stat.SWORD_POISON)) {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), attacker);
                }
                if (target instanceof Mob mob && random.nextDouble() < PerkStats.get(attacker, Stat.SWORD_DISARM)) {
                    ItemStack held = mob.getMainHandItem();
                    if (!held.isEmpty()) {
                        mob.spawnAtLocation(held.copy());
                        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                        attacker.displayClientMessage(Component.translatable("message.leveledworld.disarmed").withStyle(ChatFormatting.GOLD), true);
                    }
                }
            } else if (Gear.isTwoHanded(weapon)) {
                if (random.nextDouble() < PerkStats.get(attacker, Stat.HEAVY_BLEED)) {
                    bleed(target, PerkStats.has(attacker, Stat.HEAVY_BLEED_STRONG), attacker);
                }
                AttributeService.restoreStamina(attacker, PerkStats.get(attacker, Stat.HEAVY_HIT_STAMINA));
                boolean fullCharge = CombatState.of(attacker).lastAttackStrength >= 0.9f;
                if (fullCharge && PerkStats.has(attacker, Stat.HEAVY_KNOCKBACK)) {
                    target.knockback(PerkStats.get(attacker, Stat.HEAVY_KNOCKBACK),
                            attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                }
                double cleave = PerkStats.get(attacker, Stat.HEAVY_CLEAVE);
                if (fullCharge && cleave > 0 && !cleaving) cleave(attacker, target, event.getNewDamage() * (float) cleave);
            }
            if (attacker.isSprinting() && Gear.isSword(weapon) && PerkStats.has(attacker, Stat.SWORD_CHARGE)) {
                target.knockback(0.6, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            }
        } else if (direct instanceof Projectile) {
            if (random.nextDouble() < PerkStats.get(attacker, Stat.ARROW_SLOW)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), attacker);
            }
            if (direct instanceof AbstractArrow arrow && arrow.pickup == AbstractArrow.Pickup.ALLOWED
                    && random.nextDouble() < PerkStats.get(attacker, Stat.ARROW_RECOVER)) {
                ItemStack back = arrow.getPickupItemStackOrigin().copyWithCount(1);
                if (!attacker.getInventory().add(back)) attacker.drop(back, false);
            }
        }
    }

    private static void bleed(LivingEntity target, boolean strong, Player source) {
        int amp = 0;
        MobEffectInstance current = target.getEffect(RpgEffects.BLEEDING);
        if (strong && current != null) amp = Math.min(1, current.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(RpgEffects.BLEEDING, strong ? 100 : 60, amp), source);
    }

    private static void cleave(ServerPlayer attacker, LivingEntity target, float damage) {
        cleaving = true;
        try {
            List<LivingEntity> near = target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5),
                    e -> e != attacker && e != target && e.isAlive() && !(e instanceof Player) && !e.isAlliedTo(attacker));
            for (LivingEntity e : near) e.hurt(attacker.damageSources().playerAttack(attacker), damage);
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 3, 1.0, 0.1, 1.0, 0);
            }
        } finally {
            cleaving = false;
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player) return;
        ItemStack weapon = player.getMainHandItem();
        if (Gear.isSword(weapon)) {
            double heal = PerkStats.get(player, Stat.SWORD_LIFESTEAL);
            if (heal > 0) player.heal((float) heal);
        } else if (Gear.isTwoHanded(weapon) && PerkStats.has(player, Stat.HEAVY_KILL_STRENGTH)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
        }
    }

    /** Perk Ladrão: inimigos mortos por ataque furtivo podem dropar um item extra. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.getPersistentData().getBoolean(SNEAK_KILL_TAG) || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        double chance = PerkStats.get(player, Stat.SNEAK_LOOT);
        if (chance <= 0 || event.getDrops().isEmpty() || player.getRandom().nextDouble() >= chance) return;
        var pick = event.getDrops().stream().skip(player.getRandom().nextInt(event.getDrops().size())).findFirst().orElseThrow();
        var copy = new net.minecraft.world.entity.item.ItemEntity(victim.level(), pick.getX(), pick.getY(), pick.getZ(), pick.getItem().copy());
        copy.setDefaultPickUpDelay();
        event.getDrops().add(copy);
    }

    /** Flechas: velocidade extra, disparo duplo e leque de flechas (agachado). */
    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow) || arrow instanceof ThrownTrident) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player) || arrow.getPersistentData().getBoolean(EXTRA_ARROW_TAG)) return;
        if (arrow.tickCount > 0) return;
        double velocity = PerkStats.get(player, Stat.ARROW_VELOCITY);
        if (velocity > 0) arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1 + velocity));

        if (PerkStats.has(player, Stat.ARROW_FAN) && player.isCrouching() && consumeArrows(player, 2)) {
            spawnCopy(arrow, -8);
            spawnCopy(arrow, 8);
        } else if (player.getRandom().nextDouble() < PerkStats.get(player, Stat.DOUBLE_SHOT)) {
            spawnCopy(arrow, player.getRandom().nextBoolean() ? 3 : -3);
        }
    }

    private static boolean consumeArrows(ServerPlayer player, int count) {
        if (player.getAbilities().instabuild) return true;
        int found = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ItemTags.ARROWS)) found += inv.getItem(i).getCount();
        if (found < count) return false;
        int left = count;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ItemTags.ARROWS)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    private static void spawnCopy(AbstractArrow arrow, float yawOffset) {
        Entity created = arrow.getType().create(arrow.level());
        if (!(created instanceof AbstractArrow copy)) return;
        CompoundTag tag = arrow.saveWithoutId(new CompoundTag());
        copy.load(tag);
        copy.setUUID(UUID.randomUUID());
        copy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        copy.getPersistentData().putBoolean(EXTRA_ARROW_TAG, true);
        Vec3 v = arrow.getDeltaMovement().yRot(yawOffset * Mth.DEG_TO_RAD);
        copy.setDeltaMovement(v);
        copy.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
        arrow.level().addFreshEntity(copy);
    }

    private CombatPerkEvents() {}
}
