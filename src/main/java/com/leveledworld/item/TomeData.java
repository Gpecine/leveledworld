package com.leveledworld.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Dados de um tomo/lição: skill ("" = ainda não sorteada), níveis concedidos, nível máximo que o treinador ensina
 * (0 = sem limite) e se é uma lição de treinador (conta no limite por level do personagem).
 */
public record TomeData(String skill, int levels, int maxLevel, boolean lesson) {
    public static final Codec<TomeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("skill").forGetter(TomeData::skill),
            Codec.INT.optionalFieldOf("levels", 1).forGetter(TomeData::levels),
            Codec.INT.optionalFieldOf("max_level", 0).forGetter(TomeData::maxLevel),
            Codec.BOOL.optionalFieldOf("lesson", false).forGetter(TomeData::lesson)
    ).apply(i, TomeData::new));

    public static final StreamCodec<ByteBuf, TomeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TomeData::skill,
            ByteBufCodecs.VAR_INT, TomeData::levels,
            ByteBufCodecs.VAR_INT, TomeData::maxLevel,
            ByteBufCodecs.BOOL, TomeData::lesson,
            TomeData::new);
}
