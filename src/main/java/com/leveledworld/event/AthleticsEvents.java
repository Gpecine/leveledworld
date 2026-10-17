package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.player.CombatState;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** XP de Atletismo: correr, nadar, escalar, pular e sobreviver a quedas (com limite por minuto contra farm). */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class AthleticsEvents {
    private static final double CAP_PER_MINUTE = 40;

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator() || player.isPassenger()) return;
        CombatState state = CombatState.of(player);
        // Usa a posição do tick anterior guardada no próprio jogador (xo/yo/zo).
        double dx = player.getX() - player.xo;
        double dy = player.getY() - player.yo;
        double dz = player.getZ() - player.zo;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > 2) return;  // teleporte
        double xp = 0;
        if (player.isSwimming() || (player.isInWater() && horizontal > 0.05)) {
            xp += horizontal * 0.12;
        } else if (player.isSprinting() && player.onGround()) {
            xp += horizontal * 0.08;
        }
        if (player.onClimbable() && dy > 0) xp += dy * 0.15;
        if (xp > 0) give(player, state, xp);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) give(player, CombatState.of(player), 0.1);
    }

    /** Sobreviver a uma queda que machuca dá XP proporcional ao dano. */
    @SubscribeEvent
    public static void onFall(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FALL)
                && player.isAlive() && event.getNewDamage() > 0) {
            give(player, CombatState.of(player), event.getNewDamage() * 2);
        }
    }

    private static void give(ServerPlayer player, CombatState state, double xp) {
        long now = player.level().getGameTime();
        if (now - state.athleticsWindowStart > 1200) {
            state.athleticsWindowStart = now;
            state.athleticsXpWindow = 0;
        }
        double allowed = Math.min(xp, CAP_PER_MINUTE - state.athleticsXpWindow);
        if (allowed <= 0) return;
        state.athleticsXpWindow += allowed;
        // Acumula frações pequenas para não sincronizar a cada tick.
        state.athleticsPending += allowed;
        if (state.athleticsPending >= 0.5) {
            LevelingService.addSkillXp(player, Skill.ATHLETICS, state.athleticsPending);
            state.athleticsPending = 0;
        }
    }

    private AthleticsEvents() {}
}
