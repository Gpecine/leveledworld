package com.leveledworld.player;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/** Anti-farm: lembra troncos e minérios colocados por jogadores (guardado no próprio chunk). */
public final class PlacedBlocks {
    public static final Codec<LongOpenHashSet> CODEC = Codec.LONG.listOf()
            .xmap(LongOpenHashSet::new, set -> List.copyOf(set));

    public static void mark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        LongOpenHashSet set = chunk.getData(RpgAttachments.PLACED_BLOCKS);
        if (set.add(pos.asLong())) chunk.setUnsaved(true);
    }

    /** Retorna true se o bloco tinha sido colocado por um jogador (e esquece a marcação). */
    public static boolean consume(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(RpgAttachments.PLACED_BLOCKS)) return false;
        LongOpenHashSet set = chunk.getData(RpgAttachments.PLACED_BLOCKS);
        boolean removed = set.remove(pos.asLong());
        if (removed) chunk.setUnsaved(true);
        return removed;
    }

    public static boolean isPlaced(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.hasData(RpgAttachments.PLACED_BLOCKS) && chunk.getData(RpgAttachments.PLACED_BLOCKS).contains(pos.asLong());
    }

    private PlacedBlocks() {}
}
