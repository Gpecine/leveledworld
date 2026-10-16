package com.leveledworld.client;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import com.leveledworld.network.SyncRpgDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Painel no canto superior esquerdo: "Lv. X", XP do personagem, stamina e mana. */
public final class RpgHud {
    public static final int XP_COLOR = 0xFFE0B030;
    public static final int STAMINA_COLOR = 0xFF4CC94C;
    public static final int MAGICKA_COLOR = 0xFF3F7FF5;

    private static final int X = 4;
    private static final int Y = 4;
    private static final int BAR_WIDTH = 82;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SyncRpgDataPayload data = ClientRpgData.get();
        if (data == null || mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;

        Font font = mc.font;
        Component label = Component.translatable("hud.leveledworld.level", data.level());
        if (data.unspentPoints() > 0) {
            label = label.copy().append(Component.literal("  +" + data.unspentPoints()).withStyle(ChatFormatting.YELLOW));
        }
        g.drawString(font, label, X, Y, 0xFFD700, true);

        int y = Y + font.lineHeight + 1;
        drawBar(g, X, y, BAR_WIDTH, 3, progress(data.xp(), data.xpToNext()), XP_COLOR);

        ResourcesPayload res = ClientRpgData.resources();
        if (res != null) {
            y += 6;
            drawBar(g, X, y, BAR_WIDTH, 4, progress(res.stamina(), res.maxStamina()), STAMINA_COLOR);
            y += 7;
            drawBar(g, X, y, BAR_WIDTH, 4, progress(res.magicka(), res.maxMagicka()), MAGICKA_COLOR);
        }
    }

    static float progress(float value, float max) {
        return max <= 0 ? 1f : Math.max(0f, Math.min(1f, value / max));
    }

    static void drawBar(GuiGraphics g, int x, int y, int width, int height, float progress, int color) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xC0000000);
        g.fill(x, y, x + width, y + height, 0xFF2A2A2A);
        g.fill(x, y, x + Math.round(width * progress), y + height, color);
    }

    private RpgHud() {}
}
