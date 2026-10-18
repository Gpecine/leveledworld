package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: level e tier de um mob, enviados quando o jogador começa a enxergá-lo (tracking).
 * {@code level} 0 = mob sem level (animais).
 */
public record MobLevelPayload(int entityId, int level, int tier) implements CustomPacketPayload {
    public static final Type<MobLevelPayload> TYPE = new Type<>(LeveledWorld.id("mob_level"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobLevelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobLevelPayload::entityId,
            ByteBufCodecs.VAR_INT, MobLevelPayload::level,
            ByteBufCodecs.VAR_INT, MobLevelPayload::tier,
            MobLevelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
