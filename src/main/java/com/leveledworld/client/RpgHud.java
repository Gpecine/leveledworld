package com.leveledworld.client;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.SyncRpgDataPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Pequeno painel no canto superior esquerdo: "Lv. X" + barra de XP do personagem. */
public final class RpgHud {
    private static final int X = 4;
    private static final int Y = 4;
    private static final int BAR_WIDTH = 82;
    private static final int BAR_HEIGHT = 4;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SyncRpgDataPayload data = ClientRpgData.get();
        if (data == null || mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;

        Font font = mc.font;
        Component label = Component.translatable("hud.leveledworld.level", data.level());
        g.drawString(font, label, X, Y, 0xFFD700, true);

        int barY = Y + font.lineHeight + 1;
        drawBar(g, X, barY, BAR_WIDTH, BAR_HEIGHT, progress(data.xp(), data.xpToNext()), 0xFF3FA9F5);
    }

    static float progress(float xp, float toNext) {
        return toNext <= 0 ? 1f : Math.min(1f, xp / toNext);
    }

    static void drawBar(GuiGraphics g, int x, int y, int width, int height, float progress, int color) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xC0000000);
        g.fill(x, y, x + width, y + height, 0xFF2A2A2A);
        g.fill(x, y, x + Math.round(width * progress), y + height, color);
    }

    private RpgHud() {}
}
