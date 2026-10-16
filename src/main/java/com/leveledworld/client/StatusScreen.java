package com.leveledworld.client;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.skill.Skill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Tela de status (tecla K): level do personagem e progresso de cada skill. */
public class StatusScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 18;

    public StatusScreen() {
        super(Component.translatable("screen.leveledworld.status"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        SyncRpgDataPayload data = ClientRpgData.get();
        int left = (width - PANEL_WIDTH) / 2;
        int skillsHeight = Skill.values().length * ROW_HEIGHT;
        int top = Math.max(10, (height - (60 + skillsHeight)) / 2);

        g.fill(left - 8, top - 8, left + PANEL_WIDTH + 8, top + 64 + skillsHeight, 0xC0101018);
        g.drawCenteredString(font, title, width / 2, top, 0xFFFFFF);

        if (data == null) {
            g.drawCenteredString(font, Component.translatable("screen.leveledworld.no_data"), width / 2, top + 24, 0xAAAAAA);
            return;
        }

        // Personagem
        int y = top + 18;
        g.drawString(font, Component.translatable("screen.leveledworld.character_level", data.level()), left, y, 0xFFD700);
        g.drawString(font, xpText(data.xp(), data.xpToNext()), left + PANEL_WIDTH - font.width(xpText(data.xp(), data.xpToNext())), y, 0xCCCCCC);
        RpgHud.drawBar(g, left, y + 11, PANEL_WIDTH, 5, RpgHud.progress(data.xp(), data.xpToNext()), 0xFF3FA9F5);

        // Skills
        y += 30;
        g.drawString(font, Component.translatable("screen.leveledworld.skills"), left, y, 0xFFFFFF);
        y += 12;
        for (Skill skill : Skill.values()) {
            int i = skill.ordinal();
            Component name = Component.translatable(skill.translationKey());
            g.drawString(font, name, left, y, 0xE0E0E0);
            String lvl = String.valueOf(data.skillLevels()[i]);
            g.drawString(font, lvl, left + PANEL_WIDTH - font.width(lvl), y, 0x80FF80);
            RpgHud.drawBar(g, left, y + 10, PANEL_WIDTH, 3,
                    RpgHud.progress(data.skillXp()[i], data.skillXpToNext()[i]), 0xFF66CC66);
            y += ROW_HEIGHT;
        }
    }

    private static Component xpText(float xp, float toNext) {
        return toNext <= 0 ? Component.translatable("screen.leveledworld.max")
                : Component.literal((int) xp + " / " + (int) toNext + " XP");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientSetup.OPEN_STATUS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
