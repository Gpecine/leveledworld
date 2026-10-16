package com.leveledworld.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Kit visual compartilhado por HUD, tela de status e avisos: painéis escuros com moldura dourada,
 * barras com gradiente/brilho e o "medalhão" de level. Tudo desenhado com fills, sem texturas.
 */
public final class RpgUi {
    // Paleta
    public static final int PANEL_BG = 0xD8100E16;
    public static final int PANEL_INNER = 0xE0080710;
    public static final int GOLD = 0xFFC9A44C;
    public static final int GOLD_LIGHT = 0xFFF2D488;
    public static final int GOLD_DARK = 0xFF5E4A20;
    public static final int TEXT = 0xFFE8E2D0;
    public static final int TEXT_DIM = 0xFF9A9384;

    /** Gradientes (topo, base) das barras. */
    public static final int[] XP = {0xFFF8DC7A, 0xFFC0861C};
    public static final int[] HEALTH = {0xFFFF7A6E, 0xFFA8141E};
    public static final int[] STAMINA = {0xFF8BE88A, 0xFF23803A};
    public static final int[] MAGICKA = {0xFF8DB8FF, 0xFF2448C0};
    public static final int[] SKILL = {0xFF7FE0C8, 0xFF1F7F70};

    /** Painel com moldura dourada dupla e cantos destacados. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_BG);
        // moldura externa escura + dourada + filete interno
        outline(g, x - 1, y - 1, w + 2, h + 2, 0xFF000000);
        outline(g, x, y, w, h, GOLD_DARK);
        outline(g, x + 1, y + 1, w - 2, h - 2, GOLD);
        outline(g, x + 3, y + 3, w - 6, h - 6, 0x40C9A44C);
        // cantos
        corner(g, x, y);
        corner(g, x + w - 4, y);
        corner(g, x, y + h - 4);
        corner(g, x + w - 4, y + h - 4);
    }

    private static void corner(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 4, y + 4, GOLD_LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + 3, GOLD_DARK);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Barra com contorno, fundo rebaixado, preenchimento em gradiente, brilho no topo e marcações a cada 25%. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float progress, int[] gradient) {
        outline(g, x - 1, y - 1, w + 2, h + 2, 0xFF000000);
        g.fillGradient(x, y, x + w, y + h, 0xFF15131C, 0xFF24212E);
        int filled = Math.round(w * Math.max(0f, Math.min(1f, progress)));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + h, gradient[0], gradient[1]);
            g.fill(x, y, x + filled, y + 1, 0x50FFFFFF);
            if (filled < w) g.fill(x + filled - 1, y, x + filled, y + h, 0x60FFFFFF);
        }
        if (w >= 40) {
            for (int i = 1; i < 4; i++) {
                int tx = x + w * i / 4;
                g.fill(tx, y, tx + 1, y + h, 0x50000000);
            }
        }
    }

    /** Medalhão quadrado com o número do level. */
    public static void levelBadge(GuiGraphics g, Font font, int x, int y, int size, int level) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        g.fillGradient(x, y, x + size, y + size, GOLD_LIGHT, GOLD_DARK);
        g.fillGradient(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF2A2234, 0xFF0E0B14);
        outline(g, x + 3, y + 3, size - 6, size - 6, 0x30F2D488);
        String text = String.valueOf(level);
        int tx = x + (size - font.width(text)) / 2 + 1;
        int ty = y + (size - font.lineHeight) / 2 + 1;
        g.drawString(font, text, tx, ty, GOLD_LIGHT, true);
    }

    /** Linha divisória dourada que esmaece nas pontas. */
    public static void divider(GuiGraphics g, int x, int y, int w) {
        int seg = Math.max(1, w / 6);
        g.fill(x, y, x + seg, y + 1, 0x405E4A20);
        g.fill(x + seg, y, x + w - seg, y + 1, GOLD_DARK);
        g.fill(x + w - seg, y, x + w, y + 1, 0x405E4A20);
        int cx = x + w / 2;
        g.fill(cx - 2, y - 1, cx + 2, y + 2, GOLD);
    }

    /** Título dourado centralizado com divisória abaixo. */
    public static void header(GuiGraphics g, Font font, Component text, int centerX, int y, int dividerWidth) {
        g.drawCenteredString(font, text, centerX, y, GOLD_LIGHT);
        divider(g, centerX - dividerWidth / 2, y + font.lineHeight + 2, dividerWidth);
    }

    /** Cor com alpha multiplicado por {@code alpha} (0..1). */
    public static int fade(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private RpgUi() {}
}
