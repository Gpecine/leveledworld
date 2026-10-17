package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: comprar o próximo rank de um perk (o servidor valida tudo). */
public record BuyPerkPayload(String perkId) implements CustomPacketPayload {
    public static final Type<BuyPerkPayload> TYPE = new Type<>(LeveledWorld.id("buy_perk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuyPerkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BuyPerkPayload::perkId,
            BuyPerkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
