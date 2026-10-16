package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: gastar um ponto no atributo (ordinal de RpgAttribute). O servidor valida. */
public record SpendPointPayload(int attribute) implements CustomPacketPayload {
    public static final Type<SpendPointPayload> TYPE = new Type<>(LeveledWorld.id("spend_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpendPointPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpendPointPayload::attribute,
            SpendPointPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
