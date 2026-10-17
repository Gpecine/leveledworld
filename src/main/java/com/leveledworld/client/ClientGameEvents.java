package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = LeveledWorld.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Sem stamina (ou exausto) não dá para correr: o servidor desconta, o cliente impede de continuar correndo.
        ResourcesPayload res = ClientRpgData.resources();
        if (mc.player != null && res != null && (res.stamina() < 1f || res.exhausted()) && mc.player.isSprinting()
                && !mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.setSprinting(false);
        }
        // Escalador (Atletismo): sobe escadas e trepadeiras mais rápido (movimento é do lado do cliente).
        if (mc.player != null && mc.player.onClimbable() && mc.player.getDeltaMovement().y > 0.05) {
            double climb = com.leveledworld.network.ClientRpgData.stat(com.leveledworld.skill.Stat.CLIMB_SPEED);
            if (climb > 0) {
                var v = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(v.x, Math.min(0.2 * (1 + climb), v.y * (1 + climb)), v.z);
            }
        }
        while (ClientSetup.OPEN_STATUS.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new StatusScreen());
            }
        }
    }

    /** Exausto não ataca: cancela o golpe em criaturas já no cliente (o servidor também recusa). */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        ResourcesPayload res = ClientRpgData.resources();
        if (!event.isAttack() || res == null || !res.exhausted() || mc.player == null
                || mc.player.isCreative() || mc.player.isSpectator()) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            event.setCanceled(true);
            event.setSwingHand(false);
            mc.player.displayClientMessage(Component.translatable("message.leveledworld.exhausted")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRpgData.clear();
    }

    private ClientGameEvents() {}
}
