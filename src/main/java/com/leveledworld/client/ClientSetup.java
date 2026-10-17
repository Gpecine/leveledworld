package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = LeveledWorld.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping OPEN_STATUS = new KeyMapping("key.leveledworld.status",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.leveledworld");
    /** Ação em área (veio, árvore, colheita 3×3). Sem tecla (padrão), usa o agachar. */
    public static final KeyMapping AREA_ACTION = new KeyMapping("key.leveledworld.area_action", KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN, "key.categories.leveledworld");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATUS);
        event.register(AREA_ACTION);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LeveledWorld.id("rpg_hud"), RpgHud::render);
    }

    private ClientSetup() {}
}
