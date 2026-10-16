package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.player.AttributeService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Aplica os atributos e controla stamina/mana. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class AttributeEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) AttributeService.tick(player);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeService.useStamina(player, LWConfig.JUMP_STAMINA_COST.get());
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeService.useStamina(player, LWConfig.ATTACK_STAMINA_COST.get());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            AttributeService.applyModifiers(p);
            AttributeService.markResourcesDirty(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            AttributeService.applyModifiers(p);
            p.setHealth(p.getMaxHealth());
            AttributeService.refill(p);
            AttributeService.markResourcesDirty(p);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) AttributeService.markResourcesDirty(p);
    }

    private AttributeEvents() {}
}
