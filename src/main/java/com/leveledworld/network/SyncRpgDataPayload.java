package com.leveledworld.network;

import com.leveledworld.LeveledWorld;
import com.leveledworld.skill.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: estado RPG completo do jogador.
 * Envia também o XP necessário para o próximo level, assim o cliente não precisa da config do servidor.
 * xpToNext = 0 significa level máximo.
 */
public record SyncRpgDataPayload(int level, float xp, float xpToNext,
                                 int[] skillLevels, float[] skillXp, float[] skillXpToNext) implements CustomPacketPayload {

    public static final Type<SyncRpgDataPayload> TYPE = new Type<>(LeveledWorld.id("sync_rpg_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncRpgDataPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SyncRpgDataPayload::write, SyncRpgDataPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(level);
        buf.writeFloat(xp);
        buf.writeFloat(xpToNext);
        buf.writeVarInt(skillLevels.length);
        for (int i = 0; i < skillLevels.length; i++) {
            buf.writeVarInt(skillLevels[i]);
            buf.writeFloat(skillXp[i]);
            buf.writeFloat(skillXpToNext[i]);
        }
    }

    private static SyncRpgDataPayload read(FriendlyByteBuf buf) {
        int level = buf.readVarInt();
        float xp = buf.readFloat();
        float xpToNext = buf.readFloat();
        int n = buf.readVarInt();
        int[] levels = new int[Skill.values().length];
        float[] xps = new float[levels.length];
        float[] toNext = new float[levels.length];
        for (int i = 0; i < n; i++) {
            int l = buf.readVarInt();
            float x = buf.readFloat();
            float t = buf.readFloat();
            if (i < levels.length) {
                levels[i] = l;
                xps[i] = x;
                toNext[i] = t;
            }
        }
        return new SyncRpgDataPayload(level, xp, xpToNext, levels, xps, toNext);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
