package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: level de um mob, enviado quando o jogador começa a enxergá-lo (tracking). */
public record MobLevelPayload(int entityId, int level) implements CustomPacketPayload {
    public static final Type<MobLevelPayload> TYPE = new Type<>(LeveledWorld.id("mob_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobLevelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobLevelPayload::entityId,
            ByteBufCodecs.VAR_INT, MobLevelPayload::level,
            MobLevelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
