package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWClientConfig;
import com.leveledworld.mob.MobTier;
import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.player.RpgAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

/**
 * Desenha o level, o tier e uma barra de vida acima de cada mob escalado. Animais (sem level) só ganham placa a
 * partir do tier Incomum. A moldura fica na cor do tier a partir do Raro.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID, value = Dist.CLIENT)
public final class MobNameplateRenderer {
    private static final float SCALE = 0.025F;
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 4;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob)) return;
        boolean hasLevel = entity.hasData(RpgAttachments.MOB_LEVEL);
        MobTier tier = entity.hasData(RpgAttachments.MOB_TIER) ? MobTier.byIndex(entity.getData(RpgAttachments.MOB_TIER)) : MobTier.COMMON;
        if (!hasLevel && tier == MobTier.COMMON) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !LWClientConfig.SHOW_NAMEPLATES.get()) return;
        if (!entity.isAlive() || entity == mc.getCameraEntity() || entity.isInvisibleTo(mc.player)) return;

        double maxDist = LWClientConfig.NAMEPLATE_DISTANCE.get();
        if (mc.getEntityRenderDispatcher().distanceToSqr(entity) > maxDist * maxDist) return;
        if (!mc.player.hasLineOfSight(entity)) return;

        int mobLevel = hasLevel ? entity.getData(RpgAttachments.MOB_LEVEL) : 0;
        Difficulty diff = difficulty(mobLevel);
        Component levelText = Component.literal(diff == Difficulty.DEADLY ? "\u2620 " + mobLevel : String.valueOf(mobLevel));
        Component name = entity.getType().getDescription();
        int tierColor = tierColor(tier);
        Component tierText = tier == MobTier.COMMON ? null
                : Component.literal(Component.translatable(tier.translationKey()).getString().toUpperCase(java.util.Locale.ROOT));

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = LightTexture.FULL_BRIGHT;

        pose.pushPose();
        // Sobe um pouco mais se o mob tiver nome customizado visível, para não sobrepor a placa vanilla.
        double yOffset = entity.getBbHeight() + 0.55 + (entity.shouldShowName() ? 0.3 : 0.0);
        pose.translate(0.0, yOffset, 0.0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(SCALE, -SCALE, SCALE);
        Matrix4f m = pose.last().pose();

        Font font = mc.font;
        boolean showBar = LWClientConfig.SHOW_HEALTH_BAR.get();
        int badgeW = hasLevel ? font.width(levelText) + 6 : 0;
        int tierW = tierText != null ? font.width(tierText) + 4 : 0;
        int nameW = font.width(name);
        int contentW = Math.max(badgeW + (hasLevel ? 3 : 0) + tierW + nameW, showBar ? BAR_WIDTH : 0);
        float l = -contentW / 2f;
        float r = contentW / 2f;
        float top = -13;
        float bottom = showBar ? BAR_HEIGHT + 2 : -1;

        // Neste espaço z negativo = mais longe da câmera: fundo e barra ficam atrás do texto (z = 0).
        VertexConsumer vc = buffers.getBuffer(RenderType.textBackground());
        quad(vc, m, l - 3, top - 2, r + 3, bottom + 2, -5f, 0xB0100E16, 0xB0100E16, light);
        // moldura fina: dourada, ou na cor do tier a partir do Raro
        int frameTop = tier.ordinal() >= MobTier.RARE.ordinal() ? tierColor : GOLD;
        int frameBottom = tier.ordinal() >= MobTier.RARE.ordinal() ? darken(tierColor) : GOLD_DARK;
        quad(vc, m, l - 4, top - 3, r + 4, top - 2, -5f, frameTop, frameTop, light);
        quad(vc, m, l - 4, bottom + 2, r + 4, bottom + 3, -5f, frameBottom, frameBottom, light);
        quad(vc, m, l - 4, top - 2, l - 3, bottom + 2, -5f, frameTop, frameBottom, light);
        quad(vc, m, r + 3, top - 2, r + 4, bottom + 2, -5f, frameTop, frameBottom, light);
        // medalhão do level na cor da dificuldade
        float bx = l;
        if (hasLevel) quad(vc, m, bx, top, bx + badgeW, top + 11, -3f, diff.top, diff.bottom, light);

        float textY = top + 2;
        float x = bx + (hasLevel ? badgeW + 3 : 0);
        if (hasLevel) font.drawInBatch(levelText, bx + 3, textY, 0xFF140E06, false, m, buffers, Font.DisplayMode.NORMAL, 0, light);
        if (tierText != null) {
            font.drawInBatch(tierText, x, textY, tierColor, false, m, buffers, Font.DisplayMode.NORMAL, 0, light);
            x += tierW;
        }
        int nameColor = tier == MobTier.COMMON ? 0xFFF0EADA : tierColor;
        font.drawInBatch(name, x, textY, nameColor, false, m, buffers, Font.DisplayMode.NORMAL, 0, light);

        if (showBar) {
            // Pegar o buffer de novo: o drawInBatch acima pediu o buffer de texto, o que encerra o batch
            // compartilhado anterior. Escrever no VertexConsumer antigo causa "Not building!" (crash).
            vc = buffers.getBuffer(RenderType.textBackground());
            float health = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0f, 1f);
            float bl = -BAR_WIDTH / 2f;
            float br = bl + BAR_WIDTH;
            float fill = bl + BAR_WIDTH * health;
            int[] grad = healthGradient(health);
            quad(vc, m, bl - 1, 0, br + 1, BAR_HEIGHT + 1, -3f, 0xFF000000, 0xFF000000, light);
            quad(vc, m, bl, 1, br, BAR_HEIGHT, -2.5f, 0xFF1C1A24, 0xFF2A2734, light);
            quad(vc, m, bl, 1, fill, BAR_HEIGHT, -2f, grad[0], grad[1], light);
            quad(vc, m, bl, 1, fill, 1.6f, -1.5f, 0x60FFFFFF, 0x60FFFFFF, light);
        }
        pose.popPose();
    }

    private static final int GOLD = 0xFFC9A44C;

    /** Cor do tier; o Exótico pulsa entre rosa e ciano. */
    static int tierColor(MobTier tier) {
        if (tier != MobTier.EXOTIC) return tier.color;
        float t = 0.5f + 0.5f * Mth.sin((System.currentTimeMillis() % 3000) / 3000f * Mth.TWO_PI);
        int r = (int) Mth.lerp(t, 0xFF, 0x4F), g = (int) Mth.lerp(t, 0x4F, 0xE8), b = (int) Mth.lerp(t, 0xD8, 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int darken(int argb) {
        int r = (argb >> 16 & 0xFF) * 2 / 5, g = (argb >> 8 & 0xFF) * 2 / 5, b = (argb & 0xFF) * 2 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    private static final int GOLD_DARK = 0xFF5E4A20;

    /** Dificuldade pela diferença entre o level do mob e o do jogador (estilo MMO). */
    private enum Difficulty {
        TRIVIAL(0xFFC8C8C8, 0xFF707070),
        EASY(0xFF9BF08A, 0xFF2E9A3A),
        EVEN(0xFFF8E7A8, 0xFFC9A44C),
        HARD(0xFFFFC870, 0xFFD06A10),
        DEADLY(0xFFFF8A80, 0xFFB01822);

        final int top, bottom;

        Difficulty(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }
    }

    private static Difficulty difficulty(int mobLevel) {
        SyncRpgDataPayload data = ClientRpgData.get();
        int diff = mobLevel - (data != null ? data.level() : 1);
        if (diff >= 5) return Difficulty.DEADLY;
        if (diff >= 2) return Difficulty.HARD;
        if (diff >= -2) return Difficulty.EVEN;
        if (diff >= -5) return Difficulty.EASY;
        return Difficulty.TRIVIAL;
    }

    private static int[] healthGradient(float fraction) {
        if (fraction > 0.5f) return new int[]{0xFF8BE88A, 0xFF23803A};
        if (fraction > 0.25f) return new int[]{0xFFFFE07A, 0xFFC08A10};
        return new int[]{0xFFFF7A6E, 0xFFA8141E};
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2,
                             float z, int topColor, int bottomColor, int light) {
        vc.addVertex(m, x1, y1, z).setColor(topColor).setLight(light);
        vc.addVertex(m, x1, y2, z).setColor(bottomColor).setLight(light);
        vc.addVertex(m, x2, y2, z).setColor(bottomColor).setLight(light);
        vc.addVertex(m, x2, y1, z).setColor(topColor).setLight(light);
    }

    private MobNameplateRenderer() {}
}
