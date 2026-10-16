package com.leveledworld.client;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import com.leveledworld.network.SyncRpgDataPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** HUD no canto superior esquerdo (medalhão de level + XP, stamina e mana) e o aviso de LEVEL UP. */
public final class RpgHud {
    private static final int X = 4;
    private static final int Y = 4;
    private static final int W = 118;
    private static final int H = 32;
    private static final long BANNER_MS = 4000;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SyncRpgDataPayload data = ClientRpgData.get();
        if (data == null || mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        Font font = mc.font;

        RpgUi.panel(g, X, Y, W, H);
        RpgUi.levelBadge(g, font, X + 5, Y + 4, 24, data.level());

        int bx = X + 34;
        int bw = W - 39;
        RpgUi.bar(g, bx, Y + 6, bw, 5, progress(data.xp(), data.xpToNext()), RpgUi.XP);
        ResourcesPayload res = ClientRpgData.resources();
        if (res != null) {
            RpgUi.bar(g, bx, Y + 15, bw, 3, progress(res.stamina(), res.maxStamina()), RpgUi.STAMINA);
            RpgUi.bar(g, bx, Y + 22, bw, 3, progress(res.magicka(), res.maxMagicka()), RpgUi.MAGICKA);
        }

        if (data.unspentPoints() > 0) {
            // Selo pulsante "+N" ao lado do painel: há pontos para distribuir (tecla K).
            String txt = "+" + data.unspentPoints();
            int cw = font.width(txt) + 8;
            int cx = X + W + 4;
            float pulse = 0.85f + 0.15f * Mth.sin((System.currentTimeMillis() % 1200) / 1200f * Mth.TWO_PI);
            g.fill(cx - 1, Y + 9, cx + cw + 1, Y + 23, 0xFF000000);
            g.fillGradient(cx, Y + 10, cx + cw, Y + 22, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse), RpgUi.fade(RpgUi.GOLD_DARK, pulse));
            g.drawString(font, txt, cx + 4, Y + 12, 0xFF1A1206, false);
        }

        renderLevelUpBanner(g, font);
    }

    private static void renderLevelUpBanner(GuiGraphics g, Font font) {
        long since = System.currentTimeMillis() - ClientRpgData.levelUpAt();
        if (ClientRpgData.levelUpAt() == 0 || since > BANNER_MS) return;
        // Entra deslizando e some nos últimos 600ms.
        float alpha = since > BANNER_MS - 600 ? (BANNER_MS - since) / 600f : 1f;
        if (alpha <= 0.05f) return;
        int slide = since < 250 ? (int) ((250 - since) / 250f * 12) : 0;

        Component sub = Component.translatable("hud.leveledworld.level_up_sub", ClientRpgData.levelUpLevel());
        int w = Math.max(170, font.width(sub) + 24);
        int h = 40;
        int x = (g.guiWidth() - w) / 2;
        int y = Y + H + 14 - slide;  // abaixo do HUD

        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x, y, x + w, y + h, RpgUi.fade(RpgUi.PANEL_BG, alpha));
        RpgUi.outline(g, x - 1, y - 1, w + 2, h + 2, RpgUi.fade(0xFF000000, alpha));
        RpgUi.outline(g, x, y, w, h, RpgUi.fade(RpgUi.GOLD_DARK, alpha));
        RpgUi.outline(g, x + 1, y + 1, w - 2, h - 2, RpgUi.fade(RpgUi.GOLD, alpha));

        Component title = Component.translatable("hud.leveledworld.level_up");
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 6, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawCenteredString(font, title, 0, 0, RpgUi.fade(RpgUi.GOLD_LIGHT, alpha));
        g.pose().popPose();

        g.drawCenteredString(font, sub, x + w / 2, y + h - 13, RpgUi.fade(RpgUi.TEXT, alpha));
        g.pose().popPose();
    }

    static float progress(float value, float max) {
        return max <= 0 ? 1f : Math.max(0f, Math.min(1f, value / max));
    }

    private RpgHud() {}
}
