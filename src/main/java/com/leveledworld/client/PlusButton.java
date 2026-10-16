package com.leveledworld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Botão "+" quadrado no estilo dourado do mod. */
public class PlusButton extends Button {
    public PlusButton(int x, int y, int size, OnPress onPress) {
        super(x, y, size, size, Component.literal("+"), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        if (!active) {
            g.fillGradient(x, y, x + w, y + h, 0xFF3A3640, 0xFF242028);
        } else if (isHoveredOrFocused()) {
            g.fillGradient(x, y, x + w, y + h, 0xFFFFE8A8, RpgUi.GOLD);
        } else {
            g.fillGradient(x, y, x + w, y + h, RpgUi.GOLD_LIGHT, RpgUi.GOLD_DARK);
        }
        g.fill(x + 1, y + 1, x + w - 1, y + 2, active ? 0x60FFFFFF : 0x20FFFFFF);
        int color = active ? 0xFF1A1206 : 0xFF6A6470;
        var font = Minecraft.getInstance().font;
        g.drawString(font, "+", x + (w - font.width("+")) / 2 + 1, y + (h - 8) / 2, color, false);
    }
}
