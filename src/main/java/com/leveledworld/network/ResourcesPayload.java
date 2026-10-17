package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: stamina e mana atuais/máximas e exaustão (enviado com frequência, por isso separado). */
public record ResourcesPayload(float stamina, float maxStamina, float magicka, float maxMagicka, boolean exhausted)
        implements CustomPacketPayload {
    public static final Type<ResourcesPayload> TYPE = new Type<>(LeveledWorld.id("resources"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourcesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ResourcesPayload::stamina,
            ByteBufCodecs.FLOAT, ResourcesPayload::maxStamina,
            ByteBufCodecs.FLOAT, ResourcesPayload::magicka,
            ByteBufCodecs.FLOAT, ResourcesPayload::maxMagicka,
            ByteBufCodecs.BOOL, ResourcesPayload::exhausted,
            ResourcesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
