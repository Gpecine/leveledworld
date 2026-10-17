package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.ResourcesPayload;
import com.leveledworld.network.SpendPointPayload;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.player.RpgAttribute;
import com.leveledworld.skill.Perk;
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
 * num carrossel (arraste, setas, A/D ou roda do mouse), estrelas de perk com tooltip e, embaixo, o painel do
 * personagem com level, XP e os atributos Mana / Vida / Stamina.
 */
public class StatusScreen extends Screen {
    private static final Skill[] SKILLS = Skill.values();
    private static final RpgAttribute[] ATTRIBUTE_ORDER = {RpgAttribute.MAGICKA, RpgAttribute.HEALTH, RpgAttribute.STAMINA};
    private static final int PANEL_H = 54;
    private static final int BUTTON = 12;
    private static final int SKY_W = 960;
    private static final int SKY_H = 540;
    /** Fundos gerados por tools/gen_skybox.py, um por skill (mesma ordem de Skill.values()). */
    private static final ResourceLocation[] SKY_TEXTURES = new ResourceLocation[SKILLS.length];

    static {
        for (Skill s : SKILLS) SKY_TEXTURES[s.ordinal()] = LeveledWorld.id("textures/gui/sky/" + s.id() + ".png");
    }

    private final Map<RpgAttribute, Button> buttons = new EnumMap<>(RpgAttribute.class);
    private final float[][] backgroundStars;
    private float scrollTarget;
    private float scroll;
    /** Posição do fundo: segue {@link #scroll} mais devagar, para a troca de cenário ser suave. */
    private float skyScroll;
    private long lastFrameNanos;
    private boolean dragging;
    private double dragDistance;
    private double pressX;

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
    private float spacing() { return width * 0.36f; }
    private float carouselY() { return panelY() * 0.43f; }
    private float radius() { return Math.min(panelY() * 0.19f, width * 0.14f); }

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
        buttons.values().forEach(b -> b.visible = canSpend);
    }

    // ---------- render ----------

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

        Perk hovered = renderCarousel(g, data, mouseX, mouseY);
        renderSelectedInfo(g, data);
        renderPanel(g, data);
        for (var widget : renderables) widget.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) renderPerkTooltip(g, data, hovered, mouseX, mouseY);
    }

    /** Desenha as constelações; retorna o perk sob o mouse (só na constelação selecionada). */
    private Perk renderCarousel(GuiGraphics g, SyncRpgDataPayload data, int mouseX, int mouseY) {
        float cx = width / 2f;
        float cy = carouselY();
        Perk hovered = null;
        // Desenha de trás para frente: as mais distantes primeiro.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < SKILLS.length; i++) order.add(i);
        order.sort((a, b) -> Float.compare(Math.abs(offset(b)), Math.abs(offset(a))));

        for (int i : order) {
            float d = offset(i);
            float ad = Math.abs(d);
            float alpha = Mth.clamp(1f - ad * 0.42f, 0f, 1f);
            if (alpha < 0.05f) continue;
            float scale = 1f / (1f + ad * 0.55f);
            float x = cx + d * spacing() * (1f - 0.1f * ad);
            float r = radius() * scale;
            Skill skill = SKILLS[i];
            int level = data.skillLevels()[i];
            Perk h = drawConstellation(g, skill, level, x, cy, r, alpha, ad < 0.5f, mouseX, mouseY);
            if (h != null) hovered = h;

            if (ad >= 0.5f) {
                Component name = Component.translatable(skill.translationKey());
                g.drawCenteredString(font, name, (int) x, (int) (cy + r + 6), RpgUi.fade(0xFFB8C4E8, alpha));
            }
        }
        // Setas indicando que dá para girar.
        float pulse = 0.5f + 0.3f * Mth.sin((System.currentTimeMillis() % 2000) / 2000f * Mth.TWO_PI);
        g.drawString(font, "«", 10, (int) cy - 4, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse));
        g.drawString(font, "»", width - 16, (int) cy - 4, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse));
        return hovered;
    }

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
        float d = raw;
        d = ((d % n) + n) % n;
        if (d > n / 2f) d -= n;
        return d;
    }

    private int selectedIndex() {
        int n = SKILLS.length;
        return ((Math.round(scrollTarget) % n) + n) % n;
    }

    private Perk drawConstellation(GuiGraphics g, Skill skill, int level, float cx, float cy, float r, float alpha,
                                   boolean selected, int mouseX, int mouseY) {
        Constellations.Shape shape = Constellations.of(skill);
        List<Perk> perks = Perk.of(skill);
        float[][] pts = new float[shape.stars().length][2];
        for (int k = 0; k < pts.length; k++) {
            pts[k][0] = cx + shape.stars()[k][0] * r;
            pts[k][1] = cy + shape.stars()[k][1] * r;
        }

        // Brilho de nebulosa atrás da constelação (mais forte na selecionada).
        float glow = selected ? 1f : 0.45f;
        for (int ring = 5; ring >= 1; ring--) {
            circle(g, cx, cy, r * (0.45f + ring * 0.22f), RpgUi.fade(0xFF0A0E20, 0.07f * glow * alpha));
        }

        int lineColor = RpgUi.fade(selected ? 0xFFAFC3FF : 0xFF8090C0, (selected ? 0.75f : 0.45f) * alpha);
        for (int[] e : shape.edges()) {
            line(g, pts[e[0]][0], pts[e[0]][1], pts[e[1]][0], pts[e[1]][1], selected ? 0.9f : 0.6f, lineColor);
        }

        Perk hovered = null;
        float time = (System.currentTimeMillis() % 100000) / 1000f;
        for (int k = 0; k < pts.length; k++) {
            int perkTier = indexOf(shape.perks(), k);
            float x = pts[k][0], y = pts[k][1];
            if (perkTier >= 0) {
                Perk perk = perks.get(perkTier);
                boolean unlocked = perk.unlocked(level);
                float size = Math.max(2.5f, r * 0.09f);
                if (unlocked) {
                    float tw = 0.75f + 0.25f * Mth.sin(time * 2f + k);
                    circle(g, x, y, size * 3.2f, RpgUi.fade(0xFFF2C25A, 0.12f * alpha * tw));
                    circle(g, x, y, size * 2.0f, RpgUi.fade(0xFFF2C25A, 0.22f * alpha * tw));
                    diamond(g, x, y, size * 1.3f, RpgUi.fade(0xFFFFE6A0, alpha));
                    diamond(g, x, y, size * 0.6f, RpgUi.fade(0xFFFFFFFF, alpha));
                } else {
                    diamond(g, x, y, size * 1.2f, RpgUi.fade(0xFF2A3050, alpha));
                    diamond(g, x, y, size * 0.8f, RpgUi.fade(0xFF8A94B8, 0.8f * alpha));
                }
                if (selected && Math.abs(mouseX - x) <= size * 1.6f + 2 && Math.abs(mouseY - y) <= size * 1.6f + 2) {
                    hovered = perk;
                    diamond(g, x, y, size * 2.2f, RpgUi.fade(0xFFFFFFFF, 0.25f));
                }
            } else {
                float size = Math.max(1.2f, r * 0.045f);
                circle(g, x, y, size * 2.2f, RpgUi.fade(0xFF9FB4FF, 0.16f * alpha));
                diamond(g, x, y, size, RpgUi.fade(0xFFE8EEFF, alpha));
            }
        }
        return hovered;
    }

    private void renderSelectedInfo(GuiGraphics g, SyncRpgDataPayload data) {
        int i = selectedIndex();
        Skill skill = SKILLS[i];
        int level = data.skillLevels()[i];
        int cx = width / 2;
        int y = (int) (carouselY() + radius() + 7);

        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(1.35f, 1.35f, 1f);
        g.drawCenteredString(font, Component.translatable(skill.translationKey()).withStyle(ChatFormatting.BOLD), 0, 0, RpgUi.GOLD_LIGHT);
        g.pose().popPose();
        y += 15;

        Component lvl = Component.translatable("screen.leveledworld.skill_level", level);
        g.drawCenteredString(font, lvl, cx, y, RpgUi.TEXT);
        y += 11;
        int bw = 130;
        RpgUi.bar(g, cx - bw / 2, y, bw, 3, RpgHud.progress(data.skillXp()[i], data.skillXpToNext()[i]), RpgUi.SKILL);
        y += 7;

        Perk next = null;
        for (Perk p : Perk.of(skill)) {
            if (!p.unlocked(level)) { next = p; break; }
        }
        Component hint = next != null
                ? Component.translatable("screen.leveledworld.next_perk",
                        Component.translatable(next.translationKey()).withStyle(ChatFormatting.GOLD), next.level)
                : Component.translatable("screen.leveledworld.all_perks").withStyle(ChatFormatting.GOLD);
        g.drawCenteredString(font, hint, cx, y, RpgUi.TEXT_DIM);
    }

    private void renderPanel(GuiGraphics g, SyncRpgDataPayload data) {
        int px = panelX(), py = panelY(), pw = panelW();
        RpgUi.panel(g, px, py, pw, PANEL_H);
        int cx = px + pw / 2;

        // Linha 1: pontos (esquerda) | medalhão + level + XP (centro)
        Component pts = Component.translatable("screen.leveledworld.points", data.unspentPoints());
        if (data.unspentPoints() > 0) {
            float pulse = 0.75f + 0.25f * Mth.sin((System.currentTimeMillis() % 1200) / 1200f * Mth.TWO_PI);
            int w = font.width(pts) + 10;
            g.fill(px + 9, py + 6, px + 11 + w, py + 20, 0xFF000000);
            g.fillGradient(px + 10, py + 7, px + 10 + w, py + 19, RpgUi.fade(RpgUi.GOLD_LIGHT, pulse), RpgUi.fade(RpgUi.GOLD_DARK, pulse));
            g.drawString(font, pts, px + 15, py + 9, 0xFF1A1206, false);
        } else {
            g.drawString(font, pts, px + 10, py + 9, RpgUi.TEXT_DIM);
        }

        int barW = Math.min(170, pw / 3);
        int bx = cx - barW / 2 + 14;
        RpgUi.levelBadge(g, font, bx - 26, py + 5, 20, data.level());
        g.drawString(font, Component.translatable("screen.leveledworld.character_level", data.level()), bx, py + 6, RpgUi.GOLD_LIGHT);
        String xp = data.xpToNext() <= 0 ? "MAX" : (int) data.xp() + " / " + (int) data.xpToNext();
        g.drawString(font, xp, bx + barW - font.width(xp), py + 6, RpgUi.TEXT_DIM);
        RpgUi.bar(g, bx, py + 17, barW, 4, RpgHud.progress(data.xp(), data.xpToNext()), RpgUi.XP);

        // Linha 2: Mana | Vida | Stamina
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
                case STAMINA -> { if (res != null) { value = res.stamina(); max = res.maxStamina(); } gradient = RpgUi.STAMINA; }
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

    private void renderPerkTooltip(GuiGraphics g, SyncRpgDataPayload data, Perk perk, int mouseX, int mouseY) {
        boolean unlocked = perk.unlocked(data.skillLevels()[perk.skill.ordinal()]);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(perk.translationKey()).withStyle(unlocked ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        lines.add(Component.translatable(perk.translationKey() + ".desc").withStyle(ChatFormatting.WHITE));
        lines.add(unlocked
                ? Component.translatable("screen.leveledworld.perk_unlocked").withStyle(ChatFormatting.GREEN)
                : Component.translatable("screen.leveledworld.perk_requires",
                        Component.translatable(perk.skill.translationKey()), perk.level).withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    // ---------- primitivas (quads no buffer de GUI) ----------

    private static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return -1;
    }

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
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && mouseY < panelY()) {
            dragging = true;
            dragDistance = 0;
            pressX = mouseX;
            return true;
        }
        return false;
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
            if (dragDistance < 3) {
                // Clique simples numa constelação lateral: gira até ela.
                float rel = (float) ((pressX - width / 2f) / spacing());
                if (Math.abs(rel) > 0.5f) scrollTarget += Math.round(rel);
            }
            scrollTarget = Math.round(scrollTarget);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollTarget = Math.round(scrollTarget - Math.signum(scrollY != 0 ? scrollY : scrollX));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
