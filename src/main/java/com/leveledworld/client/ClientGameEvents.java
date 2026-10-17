package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = LeveledWorld.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Sem stamina não dá para correr (o servidor desconta, o cliente impede de continuar correndo).
        ResourcesPayload res = ClientRpgData.resources();
        if (mc.player != null && res != null && res.stamina() < 1f && mc.player.isSprinting()
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

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRpgData.clear();
    }

    private ClientGameEvents() {}
}
