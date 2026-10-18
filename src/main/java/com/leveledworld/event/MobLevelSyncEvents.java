package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.mob.MobScaling;
import com.leveledworld.mob.MobTiers;
import com.leveledworld.network.MobLevelPayload;
import com.leveledworld.player.RpgAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Envia level e tier do mob a cada jogador que passa a rastreá-lo (o spawn do mob já chegou ao cliente nesse ponto). */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class MobLevelSyncEvents {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof Mob mob
                && (mob.hasData(RpgAttachments.MOB_LEVEL) || mob.hasData(RpgAttachments.MOB_TIER))) {
            PacketDistributor.sendToPlayer(player, new MobLevelPayload(mob.getId(), MobScaling.getLevel(mob),
                    MobTiers.get(mob).ordinal()));
        }
    }

    private MobLevelSyncEvents() {}
}
