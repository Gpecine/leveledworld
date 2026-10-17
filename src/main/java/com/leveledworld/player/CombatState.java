package com.leveledworld.player;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Estado temporário por jogador usado pelos perks (não é salvo). */
public final class CombatState {
    private static final Map<Player, CombatState> STATES = new WeakHashMap<>();

    public static synchronized CombatState of(Player player) {
        return STATES.computeIfAbsent(player, p -> new CombatState());
    }

    /** Tecla de ação em área (veio, árvore, colheita): escolhida nos controles e se está apertada. */
    public boolean areaKeyBound;
    public boolean areaKeyDown;

    /** Ação em área ativa: a tecla escolhida nos controles ou, sem tecla, o agachar. */
    public static boolean areaAction(Player player) {
        CombatState state = of(player);
        return state.areaKeyBound ? state.areaKeyDown : player.isCrouching();
    }

    /** Força do ataque (0..1) capturada no início do golpe, antes do vanilla zerar o carregamento. */
    public float lastAttackStrength = 1f;
    public long lastBlockTick = -1000;
    public long lastDodgeTick = -1000;
    public UUID comboTarget;
    public int comboCount;
    public long lastComboTick;
    public long lastSecondWindTick = -100000;
    public long lastLastStandTick = -100000;
    public long lastPhantomTick = -100000;
    public long lastEvasionTick = -100000;
    public int stillTicks;
    public double lastX, lastY, lastZ;
    public boolean hasLastPos;
    public double athleticsXpWindow;
    public long athleticsWindowStart;
    public double athleticsPending;
    public int stealthState = -1;
    public int handledShieldCooldown;
    /** Efeitos negativos a encurtar no próximo tick (perk Resiliência). */
    public final List<Holder<MobEffect>> pendingDebuffs = new ArrayList<>();

    private CombatState() {}
}
