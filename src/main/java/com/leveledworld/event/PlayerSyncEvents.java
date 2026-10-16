package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.RpgNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Mantém o cliente atualizado com os dados RPG do servidor. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class PlayerSyncEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) RpgNetwork.sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) RpgNetwork.sync(p);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) RpgNetwork.sync(p);
    }

    private PlayerSyncEvents() {}
}
