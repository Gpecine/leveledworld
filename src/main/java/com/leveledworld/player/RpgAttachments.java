package com.leveledworld.player;

import com.leveledworld.LeveledWorld;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class RpgAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LeveledWorld.MODID);

    /** Progresso RPG do jogador. Persiste entre mortes. */
    public static final Supplier<AttachmentType<PlayerRpgData>> PLAYER_DATA = ATTACHMENT_TYPES.register("player_data",
            () -> AttachmentType.builder(PlayerRpgData::new)
                    .serialize(PlayerRpgData.CODEC)
                    .copyOnDeath()
                    .build());

    /** Level atribuído a um mob quando ele entrou no mundo. Ausente = ainda não escalado. */
    public static final Supplier<AttachmentType<Integer>> MOB_LEVEL = ATTACHMENT_TYPES.register("mob_level",
            () -> AttachmentType.builder(() -> 1)
                    .serialize(Codec.INT)
                    .build());

    /** Mob nascido de spawner (XP de combate reduzido). */
    public static final Supplier<AttachmentType<Boolean>> FROM_SPAWNER = ATTACHMENT_TYPES.register("from_spawner",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** Blocos (troncos/minérios) colocados por jogadores neste chunk. */
    public static final Supplier<AttachmentType<it.unimi.dsi.fastutil.longs.LongOpenHashSet>> PLACED_BLOCKS =
            ATTACHMENT_TYPES.register("placed_blocks",
                    () -> AttachmentType.builder(() -> new it.unimi.dsi.fastutil.longs.LongOpenHashSet())
                            .serialize(PlacedBlocks.CODEC).build());

    private RpgAttachments() {}
}
