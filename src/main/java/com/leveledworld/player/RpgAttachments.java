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

    private RpgAttachments() {}
}
