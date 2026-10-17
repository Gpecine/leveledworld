package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: estado de furtividade (-1 em pé, 0 oculto, 1 alerta, 2 detectado). */
public record StealthPayload(int state) implements CustomPacketPayload {
    public static final Type<StealthPayload> TYPE = new Type<>(LeveledWorld.id("stealth"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StealthPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StealthPayload::state,
            StealthPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
