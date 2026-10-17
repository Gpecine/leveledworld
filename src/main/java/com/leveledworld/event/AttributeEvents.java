package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.player.AttributeService;
import com.leveledworld.player.PerkService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Aplica os atributos e controla stamina/mana. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class AttributeEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeService.tick(player);
            DefensePerkEvents.tickDebuffs(player);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeService.useStamina(player, AttributeService.jumpCost(player));
        }
    }

    /** Exausto não ataca; cada ataque gasta stamina (mais com armas de duas mãos). */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (AttributeService.isExhausted(player)) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.leveledworld.exhausted")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            AttributeService.useStamina(player, AttributeService.attackCost(player));
        }
    }

    /** Disparar com arco gasta stamina; exausto, o disparo sai fraco. */
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.hasAmmo()) {
            if (AttributeService.isExhausted(player)) {
                event.setCharge(event.getCharge() / 2);
                return;
            }
            AttributeService.useStamina(player, LWConfig.BOW_STAMINA_COST.get());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PerkService.migrate(p);
            AttributeService.applyModifiers(p);
            AttributeService.markResourcesDirty(p);
            com.leveledworld.network.RpgNetwork.sync(p);
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
