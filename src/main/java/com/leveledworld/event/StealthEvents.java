package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.RpgNetwork;
import com.leveledworld.player.CombatState;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Skill;
import com.leveledworld.skill.Stat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.EnderManAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Furtividade: agachado, os mobs demoram mais a notar o jogador. O estado (oculto / alerta / detectado) é enviado
 * ao cliente para o ícone do HUD, e ficar oculto perto de inimigos dá XP de Furtividade.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class StealthEvents {
    private static final double RANGE = 16;

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && player.isCrouching()) {
            double reduction = PerkStats.get(player, Stat.SNEAK_VISIBILITY);
            if (reduction > 0) event.modifyVisibility(1 - reduction);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        CombatState state = CombatState.of(player);
        boolean crouching = player.isCrouching() && !player.isSpectator();

        // Parado agachado conta para "Desaparecer".
        double moved = state.hasLastPos ? player.distanceToSqr(state.lastX, player.getY(), state.lastZ) : 1;
        state.stillTicks = crouching && moved < 0.0025 ? state.stillTicks + 10 : 0;

        int newState = -1;
        if (crouching) {
            List<Mob> hostiles = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE),
                    m -> m instanceof Enemy && m.isAlive());
            boolean detected = false;
            int unaware = 0;
            boolean close = false;
            for (Mob mob : hostiles) {
                if (mob.getTarget() == player) {
                    detected = true;
                } else {
                    unaware++;
                    if (mob.distanceToSqr(player) < 36) close = true;
                }
            }
            newState = detected ? 2 : close ? 1 : 0;
            // XP: oculto perto de inimigos que não te viram (por segundo, no máximo 1).
            if (player.tickCount % 20 == 0 && unaware > 0) {
                LevelingService.addSkillXp(player, Skill.SNEAK, Math.min(1.0, unaware * 0.25));
            }

            boolean invisible = false;
            if (PerkStats.has(player, Stat.VANISH) && state.stillTicks >= 60) invisible = true;
            if (PerkStats.has(player, Stat.SHADOW_INVIS) && player.level().getMaxLocalRawBrightness(player.blockPosition()) < 7) invisible = true;
            if (invisible) player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false, true));

            // Fantasma: inimigos distantes perdem o rastro (recarga de 30s).
            long now = player.level().getGameTime();
            if (detected && PerkStats.has(player, Stat.PHANTOM) && now - state.lastPhantomTick > 600) {
                boolean lost = false;
                for (Mob mob : hostiles) {
                    if (mob.getTarget() == player && mob.distanceToSqr(player) >= 64) {
                        mob.setTarget(null);
                        lost = true;
                    }
                }
                if (lost) {
                    state.lastPhantomTick = now;
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.leveledworld.phantom")
                            .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
                }
            }
        }
        if (newState != state.stealthState) {
            state.stealthState = newState;
            RpgNetwork.syncStealth(player, newState);
        }
        state.lastX = player.getX();
        state.lastZ = player.getZ();
        state.hasLastPos = true;
    }

    /** Evasão: ao ser notado, o mob hesita por 1s. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.getTarget() == player || !PerkStats.has(player, Stat.EVASION)) return;
        CombatState state = CombatState.of(player);
        long now = player.level().getGameTime();
        if (now - state.lastEvasionTick < 40) return;
        state.lastEvasionTick = now;
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4));
        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 2));
    }

    /** Sem Rastro: Endermen não se irritam quando você olha para eles. */
    @SubscribeEvent
    public static void onEndermanAnger(EnderManAngerEvent event) {
        if (PerkStats.has(event.getPlayer(), Stat.NO_ENDERMAN)) event.setCanceled(true);
    }

    /** Passos Silenciosos: agachado, não dispara sensores sculk nem o Warden. */
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof ServerPlayer player && player.isCrouching() && PerkStats.has(player, Stat.SILENT)) {
            event.setCanceled(true);
        }
    }

    private StealthEvents() {}
}
