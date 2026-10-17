package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente → servidor: estado da tecla de ação em área (veio, árvore, colheita 3×3). {@code bound} = o jogador
 * escolheu uma tecla nos controles; sem tecla, a ação usa o agachar.
 */
public record AreaKeyPayload(boolean bound, boolean down) implements CustomPacketPayload {
    public static final Type<AreaKeyPayload> TYPE = new Type<>(LeveledWorld.id("area_key"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AreaKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AreaKeyPayload::bound,
            ByteBufCodecs.BOOL, AreaKeyPayload::down,
            AreaKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
