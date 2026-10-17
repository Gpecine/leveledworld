package com.leveledworld.player;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.network.RpgNetwork;
import com.leveledworld.skill.Gear;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Stat;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Pontos de atributo (Vida/Stamina/Mana), recursos stamina/mana e os modificadores vindos dos perks. */
public final class AttributeService {
    private static final ResourceLocation HEALTH_MODIFIER = LeveledWorld.id("health_points");
    private static final ResourceLocation SPRINT_MODIFIER = LeveledWorld.id("stamina_sprint_speed");
    private static final ResourceLocation PERK_ARMOR = LeveledWorld.id("perk_armor");
    private static final ResourceLocation PERK_KB = LeveledWorld.id("perk_knockback_resistance");
    private static final ResourceLocation PERK_EXPLOSION_KB = LeveledWorld.id("perk_explosion_knockback");
    private static final ResourceLocation PERK_HEALTH = LeveledWorld.id("perk_health");
    private static final ResourceLocation PERK_SPEED = LeveledWorld.id("perk_speed");
    private static final ResourceLocation PERK_JUMP = LeveledWorld.id("perk_jump");
    private static final ResourceLocation PERK_SAFE_FALL = LeveledWorld.id("perk_safe_fall");
    private static final ResourceLocation PERK_OXYGEN = LeveledWorld.id("perk_oxygen");
    private static final ResourceLocation PERK_SWIM = LeveledWorld.id("perk_swim");
    private static final ResourceLocation PERK_SNEAK_SPEED = LeveledWorld.id("perk_sneak_speed");
    private static final ResourceLocation PERK_ATTACK_SPEED = LeveledWorld.id("perk_attack_speed");

    public static float maxStamina(PlayerRpgData data) {
        return (float) (LWConfig.BASE_STAMINA.get() + LWConfig.STAMINA_PER_POINT.get() * data.points(RpgAttribute.STAMINA)
                + PerkStats.of(data)[Stat.STAMINA_MAX.ordinal()]);
    }

    public static float maxMagicka(PlayerRpgData data) {
        return (float) (LWConfig.BASE_MAGICKA.get() + LWConfig.MAGICKA_PER_POINT.get() * data.points(RpgAttribute.MAGICKA));
    }

    private static double stat(PlayerRpgData data, Stat stat) {
        return PerkStats.of(data)[stat.ordinal()];
    }

    /** Gasta um ponto no atributo escolhido. Retorna false se não houver pontos. */
    public static boolean spendPoint(ServerPlayer player, RpgAttribute attribute) {
        PlayerRpgData data = LevelingService.get(player);
        if (data.unspentPoints() <= 0) return false;
        data.setUnspentPoints(data.unspentPoints() - 1);
        data.setPoints(attribute, data.points(attribute) + 1);
        applyModifiers(player);
        // Skyrim: ao investir em um atributo, ele é restaurado.
        switch (attribute) {
            case HEALTH -> player.setHealth(player.getMaxHealth());
            case STAMINA -> data.setStamina(maxStamina(data));
            case MAGICKA -> data.setMagicka(maxMagicka(data));
        }
        RpgNetwork.sync(player);
        return true;
    }

    /** Devolve todos os pontos gastos para redistribuir. */
    public static void resetPoints(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        data.setUnspentPoints(data.unspentPoints() + data.spentPoints());
        for (RpgAttribute a : RpgAttribute.values()) data.setPoints(a, 0);
        applyModifiers(player);
        clampResources(data);
        RpgNetwork.sync(player);
    }

    /** Reaplica os modificadores de atributo vanilla (login, respawn, pontos e perks). */
    public static void applyModifiers(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        set(player, Attributes.MAX_HEALTH, HEALTH_MODIFIER,
                LWConfig.HEALTH_PER_POINT.get() * data.points(RpgAttribute.HEALTH), AttributeModifier.Operation.ADD_VALUE, true);
        applyPerkModifiers(player, data);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /** Modificadores que dependem do equipamento e do estado; reaplicados a cada meio segundo. */
    private static void applyPerkModifiers(ServerPlayer player, PlayerRpgData data) {
        int heavy = Gear.count(player, Gear.Weight.HEAVY);
        int light = Gear.count(player, Gear.Weight.LIGHT);
        boolean lowHp = player.getHealth() / player.getMaxHealth() < 0.3f;

        double heavyBonus = stat(data, Stat.HEAVY_ARMOR_BONUS) + (heavy == 4 ? stat(data, Stat.HEAVY_SET_ARMOR) : 0)
                + (lowHp ? stat(data, Stat.HEAVY_LOW_HP_ARMOR) : 0);
        double lightBonus = stat(data, Stat.LIGHT_ARMOR_BONUS) + (light == 4 ? stat(data, Stat.LIGHT_SET_ARMOR) : 0);
        double armor = Gear.armorFrom(player, Gear.Weight.HEAVY) * heavyBonus + Gear.armorFrom(player, Gear.Weight.LIGHT) * lightBonus;
        if (Gear.holdingShield(player)) armor += stat(data, Stat.SHIELD_HOLD_ARMOR);
        if (Gear.isTwoHanded(player.getMainHandItem())) armor += stat(data, Stat.HEAVY_HOLD_ARMOR);
        set(player, Attributes.ARMOR, PERK_ARMOR, armor, AttributeModifier.Operation.ADD_VALUE, false);

        boolean colossus = heavy == 4 && stat(data, Stat.HEAVY_COLOSSUS) > 0;
        double kb = (heavy == 4 ? stat(data, Stat.HEAVY_SET_KB) : 0)
                + (Gear.isTwoHanded(player.getMainHandItem()) ? stat(data, Stat.HEAVY_KB_RES) : 0)
                + (player.isBlocking() && stat(data, Stat.BLOCK_KB_IMMUNE) > 0 ? 1.0 : 0)
                + (colossus ? 1.0 : 0);
        set(player, Attributes.KNOCKBACK_RESISTANCE, PERK_KB, Math.min(1.0, kb), AttributeModifier.Operation.ADD_VALUE, false);
        set(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, PERK_EXPLOSION_KB,
                heavy >= 2 && stat(data, Stat.HEAVY_EXPLOSION_KB) > 0 ? 1.0 : 0, AttributeModifier.Operation.ADD_VALUE, false);
        set(player, Attributes.MAX_HEALTH, PERK_HEALTH, colossus ? 4.0 : 0, AttributeModifier.Operation.ADD_VALUE, false);

        set(player, Attributes.MOVEMENT_SPEED, PERK_SPEED, light >= 2 ? stat(data, Stat.LIGHT_SPEED) : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        set(player, Attributes.JUMP_STRENGTH, PERK_JUMP,
                stat(data, Stat.JUMP_BOOST) + (light == 4 && stat(data, Stat.LIGHT_JUMP) > 0 ? 0.1 : 0),
                AttributeModifier.Operation.ADD_VALUE, false);
        set(player, Attributes.SAFE_FALL_DISTANCE, PERK_SAFE_FALL, stat(data, Stat.SAFE_FALL), AttributeModifier.Operation.ADD_VALUE, false);
        set(player, Attributes.OXYGEN_BONUS, PERK_OXYGEN, stat(data, Stat.OXYGEN), AttributeModifier.Operation.ADD_VALUE, false);
        set(player, NeoForgeMod.SWIM_SPEED, PERK_SWIM, stat(data, Stat.SWIM_SPEED), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
        set(player, Attributes.SNEAKING_SPEED, PERK_SNEAK_SPEED, stat(data, Stat.SNEAK_SPEED), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
        set(player, Attributes.ATTACK_SPEED, PERK_ATTACK_SPEED,
                Gear.isSword(player.getMainHandItem()) ? stat(data, Stat.SWORD_ATTACK_SPEED) : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
    }

    /** Adiciona/atualiza/remove um modificador (só mexe se o valor mudou, para não gerar sync à toa). */
    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                            AttributeModifier.Operation op, boolean permanent) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) {
            if (current != null) instance.removeModifier(id);
            return;
        }
        if (current != null && current.amount() == amount && current.operation() == op) return;
        instance.removeModifier(id);
        AttributeModifier modifier = new AttributeModifier(id, amount, op);
        if (permanent) instance.addPermanentModifier(modifier); else instance.addTransientModifier(modifier);
    }

    public static void refill(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        data.setStamina(maxStamina(data));
        data.setMagicka(maxMagicka(data));
    }

    /** Tenta gastar stamina; consome o que houver e retorna se havia o suficiente. */
    public static boolean useStamina(ServerPlayer player, double amount) {
        if (amount <= 0 || player.isCreative() || player.isSpectator()) return true;
        PlayerRpgData data = LevelingService.get(player);
        boolean enough = data.stamina() >= amount;
        data.setStamina((float) Math.max(0, data.stamina() - amount));
        data.staminaRegenDelay = LWConfig.STAMINA_REGEN_DELAY_TICKS.get();
        if (data.stamina() <= 0 && stat(data, Stat.SECOND_WIND) > 0) {
            CombatState state = CombatState.of(player);
            long now = player.level().getGameTime();
            if (now - state.lastSecondWindTick > 1200) {  // Segundo Fôlego: recarga de 60s
                state.lastSecondWindTick = now;
                data.setStamina(maxStamina(data) * 0.5f);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.leveledworld.second_wind")
                        .withStyle(net.minecraft.ChatFormatting.GREEN), true);
            }
        }
        return enough;
    }

    /** Recupera stamina (ex.: perks de bloqueio). */
    public static void restoreStamina(ServerPlayer player, double amount) {
        if (amount <= 0) return;
        PlayerRpgData data = LevelingService.get(player);
        data.setStamina((float) Math.min(maxStamina(data), data.stamina() + amount));
    }

    /** Custo de stamina de pular, com o perk Saltador. */
    public static double jumpCost(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        return LWConfig.JUMP_STAMINA_COST.get() * (1 - stat(data, Stat.JUMP_COST));
    }

    /** Custo de stamina de um ataque com a arma na mão. */
    public static double attackCost(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        double mult = 1;
        if (Gear.isSword(player.getMainHandItem())) mult -= stat(data, Stat.SWORD_STAMINA);
        else if (Gear.isTwoHanded(player.getMainHandItem())) mult -= stat(data, Stat.HEAVY_STAMINA);
        return LWConfig.ATTACK_STAMINA_COST.get() * Math.max(0, mult);
    }

    /** Bônus de velocidade de corrida: pontos em Stamina (com teto) + perks de Atletismo. */
    public static double sprintSpeedBonus(PlayerRpgData data) {
        return Math.min(LWConfig.MAX_SPRINT_SPEED_BONUS.get(),
                LWConfig.SPRINT_SPEED_PER_STAMINA_POINT.get() * data.points(RpgAttribute.STAMINA)) + stat(data, Stat.SPRINT_SPEED);
    }

    private static void updateSprintModifier(ServerPlayer player, PlayerRpgData data) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double bonus = player.isSprinting() ? sprintSpeedBonus(data) : 0;
        AttributeModifier current = speed.getModifier(SPRINT_MODIFIER);
        if (bonus <= 0) {
            if (current != null) speed.removeModifier(SPRINT_MODIFIER);
        } else if (current == null || current.amount() != bonus) {
            speed.removeModifier(SPRINT_MODIFIER);
            speed.addTransientModifier(new AttributeModifier(SPRINT_MODIFIER, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Chamado a cada tick do jogador no servidor: gasto por corrida, regeneração, perks e sync. */
    public static void tick(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        updateSprintModifier(player, data);
        if (player.tickCount % 10 == 0) applyPerkModifiers(player, data);
        float maxStamina = maxStamina(data);
        float maxMagicka = maxMagicka(data);
        if (data.stamina() < 0) data.setStamina(maxStamina);
        if (data.magicka() < 0) data.setMagicka(maxMagicka);

        double regenMult = 1 + stat(data, Stat.STAMINA_REGEN)
                + (Gear.count(player, Gear.Weight.LIGHT) >= 2 ? stat(data, Stat.LIGHT_STAMINA_REGEN) : 0);
        double regen = LWConfig.STAMINA_REGEN_PER_SECOND.get() / 20.0 * regenMult;
        boolean tireless = stat(data, Stat.TIRELESS) > 0;
        boolean swimmingFree = player.isInWater() && stat(data, Stat.SWIM_FREE) > 0;

        if (player.isSprinting() && !player.isCreative() && !player.isSpectator() && !swimmingFree) {
            double cost = LWConfig.SPRINT_STAMINA_PER_SECOND.get() / 20.0 * Math.max(0, 1 - stat(data, Stat.SPRINT_COST));
            if (tireless) {
                data.setStamina((float) Math.min(maxStamina, data.stamina() + regen * 0.5 - cost));
                // Incansável: correr não gasta fome (desconta a exaustão de corrida do vanilla).
                var food = player.getFoodData();
                food.setExhaustion(Math.max(0, food.getExhaustionLevel() - 0.02f));
            } else {
                useStamina(player, cost);
            }
        } else if (data.staminaRegenDelay > 0) {
            data.staminaRegenDelay--;
        } else if (data.stamina() < maxStamina) {
            data.setStamina((float) Math.min(maxStamina, data.stamina() + regen));
        }
        if (data.magicka() < maxMagicka) {
            data.setMagicka((float) Math.min(maxMagicka, data.magicka() + LWConfig.MAGICKA_REGEN_PER_SECOND.get() / 20.0));
        }
        clampResources(data);

        // Sincroniza a cada 2 ticks, só se mudou.
        if (player.tickCount % 2 == 0
                && (data.stamina() != data.lastSentStamina || data.magicka() != data.lastSentMagicka)) {
            data.lastSentStamina = data.stamina();
            data.lastSentMagicka = data.magicka();
            RpgNetwork.syncResources(player, data.stamina(), maxStamina, data.magicka(), maxMagicka);
        }
    }

    private static void clampResources(PlayerRpgData data) {
        data.setStamina(Math.max(0, Math.min(data.stamina(), maxStamina(data))));
        data.setMagicka(Math.min(data.magicka(), maxMagicka(data)));
    }

    /** Força reenvio dos recursos no próximo tick (login, respawn, troca de dimensão). */
    public static void markResourcesDirty(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        data.lastSentStamina = Float.NaN;
        data.lastSentMagicka = Float.NaN;
    }

    private AttributeService() {}
}
