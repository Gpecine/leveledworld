package com.leveledworld.client;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import com.leveledworld.network.SpendPointPayload;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.player.RpgAttribute;
import com.leveledworld.skill.Skill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;

/** Tela de status (tecla K): personagem e atributos à esquerda, skills à direita. */
public class StatusScreen extends Screen {
    private static final int COLUMN_WIDTH = 170;
    private static final int GAP = 20;
    private static final int PANEL_WIDTH = COLUMN_WIDTH * 2 + GAP;
    private static final int SKILL_ROW = 18;
    private static final int ATTRIBUTE_ROW = 24;
    private static final int BUTTON_SIZE = 16;

    private final Map<RpgAttribute, Button> buttons = new EnumMap<>(RpgAttribute.class);
    private int left;
    private int top;

    public StatusScreen() {
        super(Component.translatable("screen.leveledworld.status"));
    }

    private int panelHeight() {
        return 24 + Skill.values().length * SKILL_ROW;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(10, (height - panelHeight()) / 2);
        buttons.clear();
        int y = attributesTop();
        for (RpgAttribute attribute : RpgAttribute.values()) {
            Button b = Button.builder(Component.literal("+"),
                            btn -> PacketDistributor.sendToServer(new SpendPointPayload(attribute.ordinal())))
                    .bounds(left + COLUMN_WIDTH - BUTTON_SIZE, y - 2, BUTTON_SIZE, BUTTON_SIZE)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable(attribute.translationKey() + ".desc")))
                    .build();
            buttons.put(attribute, addRenderableWidget(b));
            y += ATTRIBUTE_ROW;
        }
        updateButtons();
    }

    private int attributesTop() {
        return top + 64;
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        SyncRpgDataPayload data = ClientRpgData.get();
        boolean canSpend = data != null && data.unspentPoints() > 0;
        buttons.values().forEach(b -> b.active = canSpend);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 10, top - 10, left + PANEL_WIDTH + 10, top + panelHeight() + 6, 0xC0101018);
        g.drawCenteredString(font, title, width / 2, top - 2, 0xFFFFFF);

        SyncRpgDataPayload data = ClientRpgData.get();
        if (data == null) {
            g.drawCenteredString(font, Component.translatable("screen.leveledworld.no_data"), width / 2, top + 24, 0xAAAAAA);
            return;
        }

        renderCharacter(g, data);
        renderSkills(g, data);
        // Botões por último, por cima do painel.
        for (var widget : renderables) widget.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCharacter(GuiGraphics g, SyncRpgDataPayload data) {
        int y = top + 14;
        g.drawString(font, Component.translatable("screen.leveledworld.character_level", data.level()), left, y, 0xFFD700);
        Component xp = xpText(data.xp(), data.xpToNext());
        g.drawString(font, xp, left + COLUMN_WIDTH - font.width(xp), y, 0xCCCCCC);
        RpgHud.drawBar(g, left, y + 11, COLUMN_WIDTH, 4, RpgHud.progress(data.xp(), data.xpToNext()), RpgHud.XP_COLOR);

        y += 24;
        int pointsColor = data.unspentPoints() > 0 ? 0xFFFF55 : 0x888888;
        g.drawString(font, Component.translatable("screen.leveledworld.points", data.unspentPoints()), left, y, pointsColor);

        ResourcesPayload res = ClientRpgData.resources();
        var player = minecraft != null ? minecraft.player : null;
        y = attributesTop();
        for (RpgAttribute attribute : RpgAttribute.values()) {
            int points = attribute.ordinal() < data.attributePoints().length ? data.attributePoints()[attribute.ordinal()] : 0;
            Component name = Component.translatable(attribute.translationKey()).append(" (" + points + ")");
            g.drawString(font, name, left, y, 0xE0E0E0);

            float value = 0, max = 0;
            int color = 0xFFCC3333;
            switch (attribute) {
                case HEALTH -> {
                    if (player != null) { value = player.getHealth(); max = player.getMaxHealth(); }
                }
                case STAMINA -> {
                    if (res != null) { value = res.stamina(); max = res.maxStamina(); }
                    color = RpgHud.STAMINA_COLOR;
                }
                case MAGICKA -> {
                    if (res != null) { value = res.magicka(); max = res.maxMagicka(); }
                    color = RpgHud.MAGICKA_COLOR;
                }
            }
            String amount = Math.round(value) + " / " + Math.round(max);
            g.drawString(font, amount, left + COLUMN_WIDTH - BUTTON_SIZE - 4 - font.width(amount), y, 0xAAAAAA);
            RpgHud.drawBar(g, left, y + 10, COLUMN_WIDTH - BUTTON_SIZE - 4, 3, RpgHud.progress(value, max), color);
            y += ATTRIBUTE_ROW;
        }
    }

    private void renderSkills(GuiGraphics g, SyncRpgDataPayload data) {
        int x = left + COLUMN_WIDTH + GAP;
        int y = top + 14;
        g.drawString(font, Component.translatable("screen.leveledworld.skills"), x, y, 0xFFFFFF);
        y += 14;
        for (Skill skill : Skill.values()) {
            int i = skill.ordinal();
            g.drawString(font, Component.translatable(skill.translationKey()), x, y, 0xE0E0E0);
            String lvl = String.valueOf(data.skillLevels()[i]);
            g.drawString(font, lvl, x + COLUMN_WIDTH - font.width(lvl), y, 0x80FF80);
            RpgHud.drawBar(g, x, y + 10, COLUMN_WIDTH, 3,
                    RpgHud.progress(data.skillXp()[i], data.skillXpToNext()[i]), 0xFF66CC66);
            y += SKILL_ROW;
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
