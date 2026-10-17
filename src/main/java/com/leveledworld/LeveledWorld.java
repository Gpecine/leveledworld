package com.leveledworld;

import com.leveledworld.config.LWClientConfig;
import com.leveledworld.config.LWConfig;
import com.leveledworld.network.RpgNetwork;
import com.leveledworld.player.RpgAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(LeveledWorld.MODID)
public class LeveledWorld {
    public static final String MODID = "leveledworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LeveledWorld(IEventBus modBus, ModContainer container) {
        RpgAttachments.ATTACHMENT_TYPES.register(modBus);
        RpgSounds.SOUND_EVENTS.register(modBus);
        RpgEffects.MOB_EFFECTS.register(modBus);
        com.leveledworld.item.RpgItems.register(modBus);
        modBus.addListener(RpgNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, LWConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, LWClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
