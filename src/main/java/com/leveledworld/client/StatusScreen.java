package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.BuyPerkPayload;
import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.LegendaryPayload;
import com.leveledworld.network.ResourcesPayload;
import com.leveledworld.network.SpendPointPayload;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.player.RpgAttribute;
import com.leveledworld.skill.PerkDef;
import com.leveledworld.skill.PerkTrees;
import com.leveledworld.skill.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tela de status (tecla K) inspirada no menu de skills do Skyrim: céu estrelado com uma constelação por skill
 * num carrossel (arraste, setas, A/D ou roda do mouse). A constelação selecionada mostra a árvore de perks —
 * clique numa estrela para aprender — e, embaixo, fica o painel do personagem com level, XP e atributos.
 */
public class StatusScreen extends Screen {
    private static final Skill[] SKILLS = Skill.values();
    private static final RpgAttribute[] ATTRIBUTE_ORDER = {RpgAttribute.MAGICKA, RpgAttribute.HEALTH, RpgAttribute.STAMINA};
    private static final int PANEL_H = 54;
    private static final int BUTTON = 12;
    private static final int SKY_W = 1600;
    private static final int SKY_H = 900;
    private static final int MAX_SKILL_LEVEL = 100;
    /** Fundos (1600x900), um por skill, na mesma ordem de Skill.values(). */
    private static final ResourceLocation[] SKY_TEXTURES = new ResourceLocation[SKILLS.length];

    static {
        for (Skill s : SKILLS) SKY_TEXTURES[s.ordinal()] = LeveledWorld.id("textures/gui/sky/" + s.id() + ".png");
    }

    private enum PerkState { MAXED, OWNED, AVAILABLE, NEEDS_LEVEL, LOCKED }

    private record PerkSpot(PerkDef perk, float x, float y, float size) {}

    private final Map<RpgAttribute, Button> buttons = new EnumMap<>(RpgAttribute.class);
    private final float[][] backgroundStars;
    private final List<PerkSpot> spots = new ArrayList<>();
    private float scrollTarget;
    private float scroll;
    /** Posição do fundo: segue {@link #scroll} mais devagar, para a troca de cenário ser suave. */
    private float skyScroll;
    private long lastFrameNanos;
    private boolean dragging;
    private double dragDistance;
    private double pressX;
    private double pressY;
    /** Popup de confirmação: perk a aprender, ou skill a tornar lendária. */
    private PerkDef confirmPerk;
    private Skill confirmLegendary;
    private int[] legendaryButton;  // x, y, w, h

    public StatusScreen() {
        super(Component.translatable("screen.leveledworld.status"));
        Random r = new Random(1337);
        backgroundStars = new float[170][4];
        for (float[] s : backgroundStars) {
            s[0] = r.nextFloat();
            s[1] = r.nextFloat();
            s[2] = r.nextFloat() * 6.28f;          // fase do brilho
            s[3] = r.nextFloat() < 0.15f ? 2 : 1;  // tamanho
        }
    }

    // ---------- layout ----------

    private int panelY() { return height - PANEL_H - 6; }
    private int panelX() { return 8; }
    private int panelW() { return width - 16; }
    private int columnW() { return (panelW() - 24) / 3; }
    private int columnX(int i) { return panelX() + 12 + i * columnW(); }
    private float spacing() { return width * 0.37f; }
    private float carouselY() { return panelY() * 0.42f; }
    /** Raio da árvore de perks da skill selecionada. */
    private float treeRadius() { return Math.min(panelY() * 0.30f, width * 0.2f); }
    /** Escala e deslocamento vertical (em raios) das silhuetas, para não encostarem no nome da skill. */
    private static final float SHAPE_SCALE = 0.92f;
    private static final float SHAPE_LIFT = 0.05f;
    /** Raio dos símbolos das constelações laterais. */
    private float sideRadius() { return Math.min(panelY() * 0.17f, width * 0.11f); }

    @Override
    protected void init() {
        buttons.clear();
        for (int i = 0; i < ATTRIBUTE_ORDER.length; i++) {
            RpgAttribute attribute = ATTRIBUTE_ORDER[i];
            Button b = new PlusButton(columnX(i) + columnW() - BUTTON - 6, panelY() + 29, BUTTON,
                    btn -> PacketDistributor.sendToServer(new SpendPointPayload(attribute.ordinal())));
            b.setTooltip(Tooltip.create(Component.translatable(attribute.translationKey() + ".desc")));
            buttons.put(attribute, addRenderableWidget(b));
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
        buttons.values().forEach(b -> b.visible = canSpend && confirmPerk == null && confirmLegendary == null);
    }

    // ---------- fundo ----------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF000000);
        renderSkyScenes(g);
        // Véu escuro para o texto e as constelações ficarem legíveis.
        g.fillGradient(0, 0, width, height, 0x38000008, 0x70000008);
        float time = (System.currentTimeMillis() % 100000) / 1000f;
        for (float[] s : backgroundStars) {
            int x = (int) (s[0] * width);
            int y = (int) (s[1] * height);
            float a = 0.2f + 0.3f * Mth.sin(time * 1.3f + s[2]);
            int size = (int) s[3];
            g.fill(x, y, x + size, y + size, RpgUi.fade(0xFFDDE6FF, a));
        }
    }

    /**
     * Cada skill tem uma região do espaço como fundo. Ao girar o carrossel, o fundo da skill atual desliza
     * (parallax) e se funde com o da próxima, como a câmera atravessando o céu.
     */
    private void renderSkyScenes(GuiGraphics g) {
        int near = -1, far = -1;
        for (int i = 0; i < SKILLS.length; i++) {
            float ad = Math.abs(skyOffset(i));
            if (ad >= 1f) continue;
            if (near < 0 || ad < Math.abs(skyOffset(near))) { far = near; near = i; }
            else if (far < 0 || ad < Math.abs(skyOffset(far))) far = i;
        }
        if (far >= 0) drawSky(g, far, 1f);
        if (near >= 0) {
            float t = far >= 0 ? Mth.clamp(1f - Math.abs(skyOffset(near)), 0f, 1f) : 1f;
            drawSky(g, near, t * t * (3f - 2f * t));  // smoothstep: começo e fim da mistura suaves
        }
    }

    private void drawSky(GuiGraphics g, int skill, float alpha) {
        float d = skyOffset(skill);
        float time = (System.currentTimeMillis() % 600000) / 1000f;
        float zoom = 1.16f + 0.04f * Math.abs(d);
        int w = Math.round(width * zoom);
        int h = Math.round(height * zoom);
        float drift = Mth.sin(time * 0.07f + skill) * width * 0.015f;  // deriva lenta, "respirando"
        float x = (width - w) / 2f - d * width * 0.08f + drift;
        float y = (height - h) / 2f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.blit(SKY_TEXTURES[skill], 0, 0, w, h, 0, 0, SKY_W, SKY_H, SKY_W, SKY_H);
        g.pose().popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // ---------- render ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Animação independente do FPS (desaceleração exponencial).
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0f : Math.min(0.1f, (now - lastFrameNanos) / 1e9f);
        lastFrameNanos = now;
        scroll = dragging ? scrollTarget : approach(scroll, scrollTarget, 9f, dt);
        skyScroll = approach(skyScroll, scroll, 3.2f, dt);
        renderBackground(g, mouseX, mouseY, partialTick);

        SyncRpgDataPayload data = ClientRpgData.get();
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 7, RpgUi.GOLD_LIGHT);
        if (data == null) {
            g.drawCenteredString(font, Component.translatable("screen.leveledworld.no_data"), width / 2, height / 2, RpgUi.TEXT_DIM);
            return;
        }

        boolean popup = confirmPerk != null || confirmLegendary != null;
        int mx = popup ? -1000 : mouseX;
        int my = popup ? -1000 : mouseY;
        PerkDef hovered = renderCarousel(g, data, mx, my);
        renderSelectedInfo(g, data, mx, my);
        renderPanel(g, data);
        for (var widget : renderables) widget.render(g, mouseX, mouseY, partialTick);

        if (popup) {
            renderConfirm(g, data, mouseX, mouseY);
        } else if (hovered != null) {
            renderPerkTooltip(g, data, hovered, mouseX, mouseY);
        }
    }

    /** Desenha as constelações; a selecionada mostra a árvore de perks. Retorna o perk sob o mouse. */
    private PerkDef renderCarousel(GuiGraphics g, SyncRpgDataPayload data, int mouseX, int mouseY) {
        float cx = width / 2f;
        float cy = carouselY();
        PerkDef hovered = null;
        spots.clear();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < SKILLS.length; i++) order.add(i);
        order.sort((a, b) -> Float.compare(Math.abs(offset(b)), Math.abs(offset(a))));

        for (int i : order) {
            float d = offset(i);
            float ad = Math.abs(d);
            float alpha = Mth.clamp(1f - ad * 0.42f, 0f, 1f);
            if (alpha < 0.05f) continue;
            float x = cx + d * spacing() * (1f - 0.1f * ad);
            Skill skill = SKILLS[i];
            // A árvore aparece só na selecionada; nas outras, o símbolo menor.
            float focus = Mth.clamp(1f - ad * 2f, 0f, 1f);
            float r = sideRadius() + (treeRadius() - sideRadius()) * focus;
            r /= (1f + ad * 0.45f);
            drawSymbol(g, skill, x, cy, r, alpha * (focus > 0 ? 0.35f + 0.25f * (1 - focus) : 0.75f), focus > 0.5f);
            if (focus > 0.05f) {
                PerkDef h = drawPerkTree(g, data, skill, x, cy, r, alpha * focus, focus > 0.9f, mouseX, mouseY);
                if (h != null) hovered = h;
            }
            if (ad >= 0.5f) {
                Component name = Component.translatable(skill.translationKey());
                g.drawCenteredString(font, name, (int) x, (int) (cy + r + 6), RpgUi.fade(0xFFB8C4E8, alpha));
            }
        }
        float pulse = 0.5f + 0.3f * Mth.sin((System.currentTimeMillis() % 2000) / 2000f * Mth.TWO_PI);
        g.drawString(font, "«", 10, (int) cy - 4, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse));
        g.drawString(font, "»", width - 16, (int) cy - 4, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse));
        return hovered;
    }

    /** Silhueta da skill (espada, machadão, arco…) formada pelas estrelas da árvore, sem estado dos perks. */
    private void drawSymbol(GuiGraphics g, Skill skill, float cx, float cy, float r, float alpha, boolean faint) {
        float halo = faint ? 0.16f : 0.07f;
        for (int ring = 5; ring >= 1; ring--) {
            circle(g, cx, cy, r * (0.5f + ring * 0.26f), RpgUi.fade(0xFF05070F, halo * alpha));
        }
        if (faint) return;  // a árvore de perks desenha a silhueta por cima
        List<PerkDef> perks = PerkTrees.of(skill);
        cy -= SHAPE_LIFT * r;
        r *= SHAPE_SCALE;
        int lineColor = RpgUi.fade(0xFFAFC3FF, 0.6f * alpha);
        for (PerkDef p : perks) {
            for (String req : p.requires) {
                PerkDef parent = PerkTrees.byLocal(skill, req);
                line(g, cx + parent.x() * r, cy + parent.y() * r, cx + p.x() * r, cy + p.y() * r, 0.7f, lineColor);
            }
        }
        float size = Math.max(1.2f, r * 0.04f);
        for (PerkDef p : perks) {
            float x = cx + p.x() * r;
            float y = cy + p.y() * r;
            circle(g, x, y, size * 2.2f, RpgUi.fade(0xFF9FB4FF, 0.16f * alpha));
            diamond(g, x, y, size, RpgUi.fade(0xFFE8EEFF, alpha));
        }
    }

    private PerkState state(SyncRpgDataPayload data, PerkDef perk) {
        int rank = data.perkRank(perk.id);
        if (rank >= perk.maxRank()) return PerkState.MAXED;
        boolean reqOk = perk.requires.isEmpty();
        for (String r : perk.requires) if (data.perkRank(perk.skill.id() + "." + r) > 0) reqOk = true;
        if (!reqOk) return rank > 0 ? PerkState.OWNED : PerkState.LOCKED;
        if (data.skillLevels()[perk.skill.ordinal()] < perk.levelForNextRank(rank)) {
            return rank > 0 ? PerkState.OWNED : PerkState.NEEDS_LEVEL;
        }
        return data.perkPoints() > 0 ? PerkState.AVAILABLE : (rank > 0 ? PerkState.OWNED : PerkState.NEEDS_LEVEL);
    }

    /** Árvore de perks: estrelas ligadas pelos requisitos. */
    private PerkDef drawPerkTree(GuiGraphics g, SyncRpgDataPayload data, Skill skill, float cx, float cy, float r,
                                 float alpha, boolean interactive, int mouseX, int mouseY) {
        List<PerkDef> perks = PerkTrees.of(skill);
        cy -= SHAPE_LIFT * r;
        float rx = r * SHAPE_SCALE;
        float ry = r * SHAPE_SCALE;
        for (PerkDef p : perks) {
            for (String req : p.requires) {
                PerkDef parent = PerkTrees.byLocal(skill, req);
                boolean lit = data.perkRank(p.id) > 0 && data.perkRank(parent.id) > 0;
                int color = lit ? RpgUi.fade(0xFFF2C25A, 0.9f * alpha) : RpgUi.fade(0xFFA8B8E8, 0.55f * alpha);
                line(g, cx + parent.x() * rx, cy + parent.y() * ry, cx + p.x() * rx, cy + p.y() * ry, lit ? 1.0f : 0.7f, color);
            }
        }
        PerkDef hovered = null;
        float time = (System.currentTimeMillis() % 100000) / 1000f;
        float size = Math.max(2.4f, r * 0.055f);
        for (PerkDef p : perks) {
            float x = cx + p.x() * rx;
            float y = cy + p.y() * ry;
            PerkState st = state(data, p);
            float tw = 0.75f + 0.25f * Mth.sin(time * 2.2f + p.index);
            switch (st) {
                case MAXED -> {
                    circle(g, x, y, size * 3.0f, RpgUi.fade(0xFFF2C25A, 0.14f * alpha * tw));
                    circle(g, x, y, size * 1.9f, RpgUi.fade(0xFFF2C25A, 0.25f * alpha * tw));
                    diamond(g, x, y, size * 1.3f, RpgUi.fade(0xFFFFE6A0, alpha));
                    diamond(g, x, y, size * 0.6f, RpgUi.fade(0xFFFFFFFF, alpha));
                }
                case OWNED -> {
                    circle(g, x, y, size * 2.0f, RpgUi.fade(0xFFF2C25A, 0.18f * alpha * tw));
                    diamond(g, x, y, size * 1.15f, RpgUi.fade(0xFFE0B860, alpha));
                    diamond(g, x, y, size * 0.5f, RpgUi.fade(0xFF2A2234, alpha));
                }
                case AVAILABLE -> {
                    float pulse = 0.55f + 0.45f * Mth.sin(time * 4f + p.index);
                    boolean partial = data.perkRank(p.id) > 0;
                    circle(g, x, y, size * 2.6f, RpgUi.fade(partial ? 0xFFF2C25A : 0xFFBFD6FF, 0.24f * alpha * pulse));
                    diamond(g, x, y, size * 1.15f, RpgUi.fade(partial ? 0xFFFFE6A0 : 0xFFE8F0FF, alpha));
                    if (partial) diamond(g, x, y, size * 0.5f, RpgUi.fade(0xFFFFFFFF, alpha));
                }
                case NEEDS_LEVEL -> {
                    diamond(g, x, y, size * 1.1f, RpgUi.fade(0xFF2A3050, alpha));
                    diamond(g, x, y, size * 0.75f, RpgUi.fade(0xFF8A94B8, 0.85f * alpha));
                }
                case LOCKED -> {
                    diamond(g, x, y, size * 0.95f, RpgUi.fade(0xFF1C2036, alpha));
                    diamond(g, x, y, size * 0.6f, RpgUi.fade(0xFF4A5070, 0.8f * alpha));
                }
            }
            int rank = data.perkRank(p.id);
            if (p.maxRank() > 1 && rank > 0 && interactive) {
                String txt = rank + "/" + p.maxRank();
                g.pose().pushPose();
                g.pose().translate(x + size * 1.4f, y - size * 2.6f, 0);
                g.pose().scale(0.5f, 0.5f, 1f);
                g.drawString(font, txt, 0, 0, RpgUi.fade(0xFFFFE6A0, alpha));
                g.pose().popPose();
            }
            if (interactive) {
                spots.add(new PerkSpot(p, x, y, size));
                if (Math.abs(mouseX - x) <= size * 1.8f + 1.5f && Math.abs(mouseY - y) <= size * 1.8f + 1.5f) {
                    hovered = p;
                    circle(g, x, y, size * 2.4f, RpgUi.fade(0xFFFFFFFF, 0.22f));
                }
            }
        }
        return hovered;
    }

    private void renderSelectedInfo(GuiGraphics g, SyncRpgDataPayload data, int mouseX, int mouseY) {
        int i = selectedIndex();
        Skill skill = SKILLS[i];
        int level = data.skillLevels()[i];
        int cx = width / 2;
        int y = (int) (carouselY() + treeRadius() + 5);
        legendaryButton = null;

        Component group = Component.translatable(skill.group.translationKey());
        int stars = i < data.legendary().length ? data.legendary()[i] : 0;
        Component name = Component.translatable(skill.translationKey()).withStyle(ChatFormatting.BOLD);
        if (stars > 0) name = name.copy().append(Component.literal(" ★" + (stars > 1 ? stars : "")).withStyle(ChatFormatting.GOLD));

        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(1.25f, 1.25f, 1f);
        g.drawCenteredString(font, name, 0, 0, RpgUi.GOLD_LIGHT);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(cx, y - 7, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawCenteredString(font, group.copy().withStyle(ChatFormatting.BOLD), 0, 0, 0xFF9FB0D8);
        g.pose().popPose();
        y += 13;

        String lvl = Component.translatable("screen.leveledworld.skill_level", level).getString();
        int bw = 110;
        int lw = font.width(lvl);
        int rowX = cx - (lw + 6 + bw) / 2;
        g.drawString(font, lvl, rowX, y, RpgUi.TEXT);
        RpgUi.bar(g, rowX + lw + 6, y + 2, bw, 3, RpgHud.progress(data.skillXp()[i], data.skillXpToNext()[i]), RpgUi.SKILL);
        y += 11;

        if (level >= MAX_SKILL_LEVEL) {
            Component label = Component.translatable("screen.leveledworld.make_legendary");
            int w = font.width(label) + 12;
            int bx = cx - w / 2;
            boolean hover = mouseX >= bx && mouseX <= bx + w && mouseY >= y - 1 && mouseY <= y + 11;
            g.fill(bx - 1, y - 2, bx + w + 1, y + 12, 0xFF000000);
            g.fillGradient(bx, y - 1, bx + w, y + 11, hover ? 0xFFFFE8A8 : RpgUi.GOLD_LIGHT, RpgUi.GOLD_DARK);
            g.drawString(font, label, bx + 6, y + 1, 0xFF1A1206, false);
            legendaryButton = new int[]{bx, y - 1, w, 12};
        } else {
            g.drawCenteredString(font, Component.translatable("skill.leveledworld." + skill.id() + ".desc"), cx, y, RpgUi.TEXT_DIM);
        }
    }

    private void renderPanel(GuiGraphics g, SyncRpgDataPayload data) {
        int px = panelX(), py = panelY(), pw = panelW();
        RpgUi.panel(g, px, py, pw, PANEL_H);
        int cx = px + pw / 2;

        chip(g, px + 10, py + 7, Component.translatable("screen.leveledworld.points", data.unspentPoints()), data.unspentPoints() > 0, false);
        Component perkPts = Component.translatable("screen.leveledworld.perk_points", data.perkPoints());
        chip(g, px + pw - 10 - font.width(perkPts) - 10, py + 7, perkPts, data.perkPoints() > 0, true);

        int barW = Math.min(150, pw / 3);
        int bx = cx - barW / 2 + 14;
        RpgUi.levelBadge(g, font, bx - 26, py + 5, 20, data.level());
        g.drawString(font, Component.translatable("screen.leveledworld.character_level", data.level()), bx, py + 6, RpgUi.GOLD_LIGHT);
        String xp = data.xpToNext() <= 0 ? "MAX" : (int) data.xp() + " / " + (int) data.xpToNext();
        g.drawString(font, xp, bx + barW - font.width(xp), py + 6, RpgUi.TEXT_DIM);
        RpgUi.bar(g, bx, py + 17, barW, 4, RpgHud.progress(data.xp(), data.xpToNext()), RpgUi.XP);

        ResourcesPayload res = ClientRpgData.resources();
        var player = minecraft != null ? minecraft.player : null;
        for (int i = 0; i < ATTRIBUTE_ORDER.length; i++) {
            RpgAttribute attribute = ATTRIBUTE_ORDER[i];
            int x = columnX(i);
            int y = py + 29;
            float value = 0, max = 0;
            int[] gradient = RpgUi.HEALTH;
            switch (attribute) {
                case HEALTH -> { if (player != null) { value = player.getHealth(); max = player.getMaxHealth(); } }
                case STAMINA -> {
                    if (res != null) { value = res.stamina(); max = res.maxStamina(); }
                    gradient = res != null && res.exhausted() ? RpgUi.STAMINA_EXHAUSTED : RpgUi.STAMINA;
                }
                case MAGICKA -> { if (res != null) { value = res.magicka(); max = res.maxMagicka(); } gradient = RpgUi.MAGICKA; }
            }
            int points = attribute.ordinal() < data.attributePoints().length ? data.attributePoints()[attribute.ordinal()] : 0;
            int right = x + columnW() - BUTTON - 10;
            g.fill(x + 1, y + 2, x + 6, y + 7, gradient[1]);
            g.fill(x + 2, y + 1, x + 5, y + 8, gradient[0]);
            Component name = Component.translatable(attribute.translationKey());
            g.drawString(font, name, x + 9, y, RpgUi.TEXT);
            g.drawString(font, "+" + points, x + 11 + font.width(name), y, RpgUi.GOLD);
            String amount = Math.round(value) + "/" + Math.round(max);
            g.drawString(font, amount, right - font.width(amount), y, RpgUi.TEXT_DIM);
            RpgUi.bar(g, x, y + 11, right - x, 5, RpgHud.progress(value, max), gradient);
        }
    }

    /** Selo de pontos (dourado pulsante quando há pontos; roxo para perks). */
    private void chip(GuiGraphics g, int x, int y, Component text, boolean active, boolean perk) {
        int w = font.width(text) + 10;
        if (active) {
            float pulse = 0.75f + 0.25f * Mth.sin((System.currentTimeMillis() % 1200) / 1200f * Mth.TWO_PI);
            int top = perk ? 0xFFD9B8FF : RpgUi.GOLD_LIGHT;
            int bottom = perk ? 0xFF5A2E8A : RpgUi.GOLD_DARK;
            g.fill(x - 1, y - 1, x + w + 1, y + 13, 0xFF000000);
            g.fillGradient(x, y, x + w, y + 12, RpgUi.fade(top, pulse), RpgUi.fade(bottom, pulse));
            g.drawString(font, text, x + 5, y + 2, perk ? 0xFFFFFFFF : 0xFF1A1206, false);
        } else {
            g.drawString(font, text, x + 5, y + 2, RpgUi.TEXT_DIM);
        }
    }

    private void renderPerkTooltip(GuiGraphics g, SyncRpgDataPayload data, PerkDef perk, int mouseX, int mouseY) {
        int rank = data.perkRank(perk.id);
        PerkState st = state(data, perk);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(perk.translationKey())
                .withStyle(rank > 0 ? ChatFormatting.GOLD : st == PerkState.AVAILABLE ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        lines.add(Component.translatable(perk.translationKey() + ".desc").withStyle(ChatFormatting.WHITE));
        if (perk.maxRank() > 1) {
            lines.add(Component.translatable("screen.leveledworld.perk_rank", rank, perk.maxRank()).withStyle(ChatFormatting.YELLOW));
        }
        int need = perk.levelForNextRank(rank);
        if (st != PerkState.MAXED && need > 0) {
            boolean levelOk = data.skillLevels()[perk.skill.ordinal()] >= need;
            lines.add(Component.translatable("screen.leveledworld.perk_requires",
                    Component.translatable(perk.skill.translationKey()), need)
                    .withStyle(levelOk ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        }
        if (st == PerkState.LOCKED) {
            Component reqs = Component.empty();
            for (int k = 0; k < perk.requires.size(); k++) {
                if (k > 0) reqs = reqs.copy().append(Component.translatable("screen.leveledworld.or"));
                reqs = reqs.copy().append(Component.translatable(PerkTrees.byLocal(perk.skill, perk.requires.get(k)).translationKey()));
            }
            lines.add(Component.translatable("screen.leveledworld.perk_requires_perk", reqs).withStyle(ChatFormatting.RED));
        }
        lines.add(switch (st) {
            case MAXED -> Component.translatable("screen.leveledworld.perk_unlocked").withStyle(ChatFormatting.GREEN);
            case AVAILABLE -> Component.translatable("screen.leveledworld.perk_click").withStyle(ChatFormatting.AQUA);
            default -> data.perkPoints() <= 0 && st == PerkState.OWNED || data.perkPoints() <= 0 && st == PerkState.NEEDS_LEVEL
                    ? Component.translatable("screen.leveledworld.perk_no_points").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.empty();
        });
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    /** Popup de confirmação (aprender perk / tornar lendária). */
    private void renderConfirm(GuiGraphics g, SyncRpgDataPayload data, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0x80000000);
        Component titleText;
        Component body;
        if (confirmPerk != null) {
            titleText = Component.translatable(confirmPerk.translationKey());
            body = Component.translatable(confirmPerk.translationKey() + ".desc");
        } else {
            titleText = Component.translatable("screen.leveledworld.make_legendary");
            body = Component.translatable("screen.leveledworld.legendary_confirm", Component.translatable(confirmLegendary.translationKey()));
        }
        List<net.minecraft.util.FormattedCharSequence> wrapped = font.split(body, 200);
        int w = 220;
        int h = 46 + wrapped.size() * 10;
        int x = (width - w) / 2;
        int y = (int) (carouselY() - h / 2f);
        RpgUi.panel(g, x, y, w, h);
        g.drawCenteredString(font, titleText.copy().withStyle(ChatFormatting.BOLD), width / 2, y + 7, RpgUi.GOLD_LIGHT);
        for (int k = 0; k < wrapped.size(); k++) g.drawCenteredString(font, wrapped.get(k), width / 2, y + 20 + k * 10, RpgUi.TEXT);
        int by = y + h - 17;
        drawButton(g, x + 20, by, 80, Component.translatable(confirmPerk != null ? "screen.leveledworld.learn" : "screen.leveledworld.confirm"), mouseX, mouseY, true);
        drawButton(g, x + w - 100, by, 80, Component.translatable("gui.cancel"), mouseX, mouseY, false);
        g.pose().popPose();
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, Component label, int mouseX, int mouseY, boolean primary) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 12;
        g.fill(x - 1, y - 1, x + w + 1, y + 13, 0xFF000000);
        int top = primary ? (hover ? 0xFFFFE8A8 : RpgUi.GOLD_LIGHT) : (hover ? 0xFF6A6478 : 0xFF4A4458);
        int bottom = primary ? RpgUi.GOLD_DARK : 0xFF242030;
        g.fillGradient(x, y, x + w, y + 12, top, bottom);
        g.drawCenteredString(font, label, x + w / 2, y + 2, primary ? 0xFF1A1206 : 0xFFE8E2D0);
    }

    // ---------- carrossel ----------

    private static float approach(float current, float target, float rate, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-rate * dt));
    }

    /** Distância (em "slots") da skill i até o centro do carrossel, com volta circular. */
    private float offset(int i) {
        return wrapOffset(i - scroll);
    }

    /** Mesma distância, mas usando a posição (mais lenta) do fundo. */
    private float skyOffset(int i) {
        return wrapOffset(i - skyScroll);
    }

    private static float wrapOffset(float raw) {
        float n = SKILLS.length;
        float d = ((raw % n) + n) % n;
        if (d > n / 2f) d -= n;
        return d;
    }

    private int selectedIndex() {
        int n = SKILLS.length;
        return ((Math.round(scrollTarget) % n) + n) % n;
    }

    // ---------- primitivas (quads no buffer de GUI) ----------

    /** Disco aproximado por 24 fatias (quads degenerados centro-a-b-b). */
    private static void circle(GuiGraphics g, float cx, float cy, float r, int color) {
        int n = 24;
        for (int i = 0; i < n; i++) {
            float a1 = Mth.TWO_PI * i / n, a2 = Mth.TWO_PI * (i + 1) / n;
            float x1 = cx + Mth.cos(a1) * r, y1 = cy + Mth.sin(a1) * r;
            float x2 = cx + Mth.cos(a2) * r, y2 = cy + Mth.sin(a2) * r;
            quad(g, cx, cy, x1, y1, x2, y2, x2, y2, color);
        }
    }

    private static void diamond(GuiGraphics g, float cx, float cy, float r, int color) {
        quad(g, cx, cy - r, cx - r, cy, cx, cy + r, cx + r, cy, color);
    }

    private static void line(GuiGraphics g, float x1, float y1, float x2, float y2, float halfWidth, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len * halfWidth, ny = dx / len * halfWidth;
        quad(g, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x2 - nx, y2 - ny, x2 + nx, y2 + ny, color);
    }

    /** Quad arbitrário; corrige a ordem dos vértices para a face ficar visível (mesmo sentido do fill). */
    private static void quad(GuiGraphics g, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int color) {
        float area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
        // Buffer obtido a cada chamada: desenhar texto entre quads encerra o batch anterior.
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        if (area > 0) {
            vc.addVertex(m, dx, dy, 0).setColor(color);
            vc.addVertex(m, cx, cy, 0).setColor(color);
            vc.addVertex(m, bx, by, 0).setColor(color);
            vc.addVertex(m, ax, ay, 0).setColor(color);
        } else {
            vc.addVertex(m, ax, ay, 0).setColor(color);
            vc.addVertex(m, bx, by, 0).setColor(color);
            vc.addVertex(m, cx, cy, 0).setColor(color);
            vc.addVertex(m, dx, dy, 0).setColor(color);
        }
    }

    // ---------- entrada ----------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmPerk != null || confirmLegendary != null) {
            handleConfirmClick(mouseX, mouseY);
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && mouseY < panelY()) {
            dragging = true;
            dragDistance = 0;
            pressX = mouseX;
            pressY = mouseY;
            return true;
        }
        return false;
    }

    private void handleConfirmClick(double mouseX, double mouseY) {
        SyncRpgDataPayload data = ClientRpgData.get();
        int lines = data == null ? 1 : font.split(confirmPerk != null
                ? Component.translatable(confirmPerk.translationKey() + ".desc")
                : Component.translatable("screen.leveledworld.legendary_confirm", Component.translatable(confirmLegendary.translationKey())), 200).size();
        int w = 220;
        int h = 46 + lines * 10;
        int x = (width - w) / 2;
        int y = (int) (carouselY() - h / 2f);
        int by = y + h - 17;
        if (mouseY >= by && mouseY <= by + 12) {
            if (mouseX >= x + 20 && mouseX <= x + 100) {
                if (confirmPerk != null) PacketDistributor.sendToServer(new BuyPerkPayload(confirmPerk.id));
                else PacketDistributor.sendToServer(new LegendaryPayload(confirmLegendary.ordinal()));
            } else if (!(mouseX >= x + w - 100 && mouseX <= x + w - 20)) {
                return;
            }
            confirmPerk = null;
            confirmLegendary = null;
            updateButtons();
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            dragDistance += Math.abs(dragX);
            scrollTarget -= (float) (dragX / spacing());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            if (dragDistance < 3) onClick(pressX, pressY);
            scrollTarget = Math.round(scrollTarget);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Clique simples: estrela de perk, botão de lendária ou constelação lateral. */
    private void onClick(double mouseX, double mouseY) {
        SyncRpgDataPayload data = ClientRpgData.get();
        if (data == null) return;
        if (legendaryButton != null && mouseX >= legendaryButton[0] && mouseX <= legendaryButton[0] + legendaryButton[2]
                && mouseY >= legendaryButton[1] && mouseY <= legendaryButton[1] + legendaryButton[3]) {
            confirmLegendary = SKILLS[selectedIndex()];
            updateButtons();
            return;
        }
        for (PerkSpot s : spots) {
            if (Math.abs(mouseX - s.x()) <= s.size() * 1.8f + 1.5f && Math.abs(mouseY - s.y()) <= s.size() * 1.8f + 1.5f) {
                if (state(data, s.perk()) == PerkState.AVAILABLE) {
                    confirmPerk = s.perk();
                    updateButtons();
                }
                return;
            }
        }
        float rel = (float) ((mouseX - width / 2f) / spacing());
        if (Math.abs(rel) > 0.5f) scrollTarget += Math.round(rel);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (confirmPerk != null || confirmLegendary != null) return true;
        scrollTarget = Math.round(scrollTarget - Math.signum(scrollY != 0 ? scrollY : scrollX));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmPerk != null || confirmLegendary != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                confirmPerk = null;
                confirmLegendary = null;
                updateButtons();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (confirmPerk != null) PacketDistributor.sendToServer(new BuyPerkPayload(confirmPerk.id));
                else PacketDistributor.sendToServer(new LegendaryPayload(confirmLegendary.ordinal()));
                confirmPerk = null;
                confirmLegendary = null;
                updateButtons();
                return true;
            }
            return true;
        }
        if (ClientSetup.OPEN_STATUS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            scrollTarget = Math.round(scrollTarget) - 1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            scrollTarget = Math.round(scrollTarget) + 1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
