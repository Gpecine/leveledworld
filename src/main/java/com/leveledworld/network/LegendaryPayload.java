package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: tornar uma skill (ordinal) lendária. */
public record LegendaryPayload(int skill) implements CustomPacketPayload {
    public static final Type<LegendaryPayload> TYPE = new Type<>(LeveledWorld.id("legendary"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LegendaryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LegendaryPayload::skill,
            LegendaryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
