package com.leveledworld.player;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.network.RpgNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Pontos de atributo (Vida/Stamina/Mana) e os recursos stamina e mana. */
public final class AttributeService {
    private static final ResourceLocation HEALTH_MODIFIER = LeveledWorld.id("health_points");

    public static float maxStamina(PlayerRpgData data) {
        return (float) (LWConfig.BASE_STAMINA.get() + LWConfig.STAMINA_PER_POINT.get() * data.points(RpgAttribute.STAMINA));
    }

    public static float maxMagicka(PlayerRpgData data) {
        return (float) (LWConfig.BASE_MAGICKA.get() + LWConfig.MAGICKA_PER_POINT.get() * data.points(RpgAttribute.MAGICKA));
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

    /** Reaplica os modificadores de atributo vanilla (login, respawn e mudança de pontos). */
    public static void applyModifiers(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double bonus = LWConfig.HEALTH_PER_POINT.get() * data.points(RpgAttribute.HEALTH);
            if (bonus > 0) {
                maxHealth.addOrReplacePermanentModifier(
                        new AttributeModifier(HEALTH_MODIFIER, bonus, AttributeModifier.Operation.ADD_VALUE));
            } else {
                maxHealth.removeModifier(HEALTH_MODIFIER);
            }
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
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
        return enough;
    }

    /** Chamado a cada tick do jogador no servidor: gasto por corrida, regeneração e sync. */
    public static void tick(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        float maxStamina = maxStamina(data);
        float maxMagicka = maxMagicka(data);
        if (data.stamina() < 0) data.setStamina(maxStamina);
        if (data.magicka() < 0) data.setMagicka(maxMagicka);

        if (player.isSprinting() && !player.isCreative() && !player.isSpectator()) {
            useStamina(player, LWConfig.SPRINT_STAMINA_PER_SECOND.get() / 20.0);
        } else if (data.staminaRegenDelay > 0) {
            data.staminaRegenDelay--;
        } else if (data.stamina() < maxStamina) {
            data.setStamina((float) Math.min(maxStamina, data.stamina() + LWConfig.STAMINA_REGEN_PER_SECOND.get() / 20.0));
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
        data.setStamina(Math.min(data.stamina(), maxStamina(data)));
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
