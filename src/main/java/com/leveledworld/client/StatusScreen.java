package com.leveledworld.client;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import com.leveledworld.network.SpendPointPayload;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.player.RpgAttribute;
import com.leveledworld.skill.Skill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;

/** Tela de status (tecla K) no mesmo estilo do HUD: personagem e atributos à esquerda, skills à direita. */
public class StatusScreen extends Screen {
    private static final int COLUMN_WIDTH = 170;
    private static final int GAP = 24;
    private static final int PAD = 12;
    private static final int PANEL_WIDTH = COLUMN_WIDTH * 2 + GAP + PAD * 2;
    private static final int PANEL_HEIGHT = 196;
    private static final int SKILL_ROW = 19;
    private static final int ATTRIBUTE_ROW = 26;
    private static final int BUTTON_SIZE = 14;

    private final Map<RpgAttribute, Button> buttons = new EnumMap<>(RpgAttribute.class);
    private int left;   // borda do painel
    private int top;

    public StatusScreen() {
        super(Component.translatable("screen.leveledworld.status"));
    }

    private int colLeft() { return left + PAD; }
    private int colRight() { return left + PAD + COLUMN_WIDTH + GAP; }
    private int attributesTop() { return top + 96; }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
        buttons.clear();
        int y = attributesTop();
        for (RpgAttribute attribute : RpgAttribute.values()) {
            Button b = new PlusButton(colLeft() + COLUMN_WIDTH - BUTTON_SIZE, y + 2, BUTTON_SIZE,
                    btn -> PacketDistributor.sendToServer(new SpendPointPayload(attribute.ordinal())));
            b.setTooltip(Tooltip.create(Component.translatable(attribute.translationKey() + ".desc")));
            buttons.put(attribute, addRenderableWidget(b));
            y += ATTRIBUTE_ROW;
        }
        updateButtons();
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
        RpgUi.panel(g, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        RpgUi.header(g, font, title.copy().withStyle(s -> s.withBold(true)), width / 2, top + 8, PANEL_WIDTH - 60);

        SyncRpgDataPayload data = ClientRpgData.get();
        if (data == null) {
            g.drawCenteredString(font, Component.translatable("screen.leveledworld.no_data"), width / 2, top + 40, RpgUi.TEXT_DIM);
            return;
        }

        renderCharacter(g, data);
        renderSkills(g, data);
        // divisória vertical entre as colunas
        int vx = left + PAD + COLUMN_WIDTH + GAP / 2;
        g.fill(vx, top + 30, vx + 1, top + PANEL_HEIGHT - 12, RpgUi.GOLD_DARK);
        for (var widget : renderables) widget.render(g, mouseX, mouseY, partialTick);
    }

    private void renderCharacter(GuiGraphics g, SyncRpgDataPayload data) {
        int x = colLeft();
        int y = top + 32;
        RpgUi.levelBadge(g, font, x, y, 28, data.level());

        int tx = x + 36;
        g.drawString(font, Component.translatable("screen.leveledworld.character_level", data.level()), tx, y + 2, RpgUi.GOLD_LIGHT);
        Component xp = xpText(data.xp(), data.xpToNext());
        g.drawString(font, xp, x + COLUMN_WIDTH - font.width(xp), y + 2, RpgUi.TEXT_DIM);
        RpgUi.bar(g, tx, y + 15, COLUMN_WIDTH - 36, 7, RpgHud.progress(data.xp(), data.xpToNext()), RpgUi.XP);

        // Selo de pontos disponíveis
        y += 40;
        Component pts = Component.translatable("screen.leveledworld.points", data.unspentPoints());
        if (data.unspentPoints() > 0) {
            float pulse = 0.7f + 0.3f * Mth.sin((System.currentTimeMillis() % 1200) / 1200f * Mth.TWO_PI);
            int w = font.width(pts) + 10;
            g.fill(x - 1, y - 1, x + w + 1, y + 13, 0xFF000000);
            g.fillGradient(x, y, x + w, y + 12, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse), RpgUi.fade(RpgUi.GOLD_DARK, pulse));
            g.drawString(font, pts, x + 5, y + 2, 0xFF1A1206, false);
        } else {
            g.drawString(font, pts, x, y + 2, RpgUi.TEXT_DIM);
        }

        ResourcesPayload res = ClientRpgData.resources();
        var player = minecraft != null ? minecraft.player : null;
        y = attributesTop();
        for (RpgAttribute attribute : RpgAttribute.values()) {
            int points = attribute.ordinal() < data.attributePoints().length ? data.attributePoints()[attribute.ordinal()] : 0;
            float value = 0, max = 0;
            int[] gradient = RpgUi.HEALTH;
            switch (attribute) {
                case HEALTH -> { if (player != null) { value = player.getHealth(); max = player.getMaxHealth(); } }
                case STAMINA -> { if (res != null) { value = res.stamina(); max = res.maxStamina(); } gradient = RpgUi.STAMINA; }
                case MAGICKA -> { if (res != null) { value = res.magicka(); max = res.maxMagicka(); } gradient = RpgUi.MAGICKA; }
            }
            // ícone: losango na cor do atributo
            g.fill(x + 1, y + 3, x + 6, y + 8, gradient[1]);
            g.fill(x + 2, y + 2, x + 5, y + 9, gradient[0]);
            Component name = Component.translatable(attribute.translationKey());
            g.drawString(font, name, x + 10, y + 2, RpgUi.TEXT);
            g.drawString(font, "+" + points, x + 12 + font.width(name), y + 2, RpgUi.GOLD);
            String amount = Math.round(value) + " / " + Math.round(max);
            int barRight = x + COLUMN_WIDTH - BUTTON_SIZE - 6;
            g.drawString(font, amount, barRight - font.width(amount), y + 2, RpgUi.TEXT_DIM);
            RpgUi.bar(g, x, y + 13, barRight - x, 4, RpgHud.progress(value, max), gradient);
            y += ATTRIBUTE_ROW;
        }
    }

    private void renderSkills(GuiGraphics g, SyncRpgDataPayload data) {
        int x = colRight();
        int y = top + 32;
        g.drawString(font, Component.translatable("screen.leveledworld.skills"), x, y, RpgUi.GOLD_LIGHT);
        RpgUi.divider(g, x, y + 11, COLUMN_WIDTH);
        y += 18;
        for (Skill skill : Skill.values()) {
            int i = skill.ordinal();
            g.drawString(font, Component.translatable(skill.translationKey()), x, y, RpgUi.TEXT);
            String lvl = String.valueOf(data.skillLevels()[i]);
            g.drawString(font, lvl, x + COLUMN_WIDTH - font.width(lvl), y, RpgUi.GOLD_LIGHT);
            RpgUi.bar(g, x, y + 10, COLUMN_WIDTH, 3,
                    RpgHud.progress(data.skillXp()[i], data.skillXpToNext()[i]), RpgUi.SKILL);
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
