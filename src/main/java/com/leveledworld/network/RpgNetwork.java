package com.leveledworld.network;

import com.leveledworld.config.LWConfig;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.PlayerRpgData;
import com.leveledworld.player.RpgAttachments;
import com.leveledworld.player.SkillProgress;
import com.leveledworld.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RpgNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(SyncRpgDataPayload.TYPE, SyncRpgDataPayload.STREAM_CODEC, RpgNetwork::handleSync)
                .playToClient(MobLevelPayload.TYPE, MobLevelPayload.STREAM_CODEC, RpgNetwork::handleMobLevel);
    }

    private static void handleMobLevel(MobLevelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Guarda o level no attachment da entidade do lado do cliente; o renderer lê de lá.
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity != null) entity.setData(RpgAttachments.MOB_LEVEL, payload.level());
        });
    }

    private static void handleSync(SyncRpgDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRpgData.set(payload));
    }

    public static void sync(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        Skill[] skills = Skill.values();
        int[] levels = new int[skills.length];
        float[] xps = new float[skills.length];
        float[] toNext = new float[skills.length];
        int skillMax = LWConfig.SKILL_MAX_LEVEL.get();
        for (Skill s : skills) {
            SkillProgress p = data.skill(s);
            levels[s.ordinal()] = p.level();
            xps[s.ordinal()] = (float) p.xp();
            toNext[s.ordinal()] = p.level() >= skillMax ? 0 : (float) LevelingService.skillXpToNext(p.level());
        }
        float charToNext = data.level() >= LWConfig.MAX_PLAYER_LEVEL.get()
                ? 0 : (float) LevelingService.characterXpToNext(data.level());
        PacketDistributor.sendToPlayer(player,
                new SyncRpgDataPayload(data.level(), (float) data.xp(), charToNext, levels, xps, toNext));
    }

    private RpgNetwork() {}
}
