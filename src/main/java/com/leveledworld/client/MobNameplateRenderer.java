package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWClientConfig;
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

/** Desenha "Nome Lv. X" e uma barra de vida acima de cada mob escalado. */
@EventBusSubscriber(modid = LeveledWorld.MODID, value = Dist.CLIENT)
public final class MobNameplateRenderer {
    private static final float SCALE = 0.025F;
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 3;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob) || !entity.hasData(RpgAttachments.MOB_LEVEL)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !LWClientConfig.SHOW_NAMEPLATES.get()) return;
        if (!entity.isAlive() || entity == mc.getCameraEntity() || entity.isInvisibleTo(mc.player)) return;

        double maxDist = LWClientConfig.NAMEPLATE_DISTANCE.get();
        if (mc.getEntityRenderDispatcher().distanceToSqr(entity) > maxDist * maxDist) return;
        if (!mc.player.hasLineOfSight(entity)) return;

        int mobLevel = entity.getData(RpgAttachments.MOB_LEVEL);
        Component text = Component.empty()
                .append(entity.getType().getDescription())
                .append(" ")
                .append(Component.translatable("nameplate.leveledworld.level", mobLevel));

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = LightTexture.FULL_BRIGHT;

        pose.pushPose();
        // Sobe um pouco mais se o mob tiver nome customizado visível, para não sobrepor a placa vanilla.
        double yOffset = entity.getBbHeight() + 0.5 + (entity.shouldShowName() ? 0.3 : 0.0);
        pose.translate(0.0, yOffset, 0.0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(SCALE, -SCALE, SCALE);
        Matrix4f matrix = pose.last().pose();

        Font font = mc.font;
        int textWidth = font.width(text);
        boolean showBar = LWClientConfig.SHOW_HEALTH_BAR.get();
        VertexConsumer vc = buffers.getBuffer(RenderType.textBackground());

        // Fundo próprio, bem atrás do texto (neste espaço, z negativo = mais longe da câmera). O fundo embutido da
        // Font fica a 0.01 unidade das letras e entra em z-fighting à distância, "apagando" parte do nome.
        float panelHalf = Math.max(textWidth, BAR_WIDTH) / 2f + 2;
        float panelBottom = showBar ? BAR_HEIGHT + 1 : -1;
        quad(vc, matrix, -panelHalf, -11, panelHalf, panelBottom, -3f, 0x60000000, light);

        font.drawInBatch(text, -textWidth / 2f, -10, levelColor(mobLevel), false, matrix, buffers,
                Font.DisplayMode.NORMAL, 0, light);

        if (showBar) {
            // Pegar o buffer de novo: o drawInBatch acima pediu o buffer de texto, o que encerra o batch
            // compartilhado anterior. Escrever no VertexConsumer antigo causa "Not building!" (crash).
            vc = buffers.getBuffer(RenderType.textBackground());
            float health = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0f, 1f);
            float left = -BAR_WIDTH / 2f;
            quad(vc, matrix, left, 0, left + BAR_WIDTH, BAR_HEIGHT, -2f, 0xFF202020, light);
            quad(vc, matrix, left, 0, left + BAR_WIDTH * health, BAR_HEIGHT, -1f, healthColor(health), light);
        }
        pose.popPose();
    }

    /** Cor do texto conforme a diferença entre o level do mob e o do jogador (estilo MMO). */
    private static int levelColor(int mobLevel) {
        SyncRpgDataPayload data = ClientRpgData.get();
        int diff = mobLevel - (data != null ? data.level() : 1);
        if (diff >= 5) return 0xFFFF5555;   // bem mais forte
        if (diff >= 2) return 0xFFFFAA00;   // mais forte
        if (diff >= -2) return 0xFFFFFFFF;  // equivalente
        if (diff >= -5) return 0xFF55FF55;  // mais fraco
        return 0xFFAAAAAA;                  // muito mais fraco
    }

    private static int healthColor(float fraction) {
        int r = (int) (Mth.clamp(2f * (1f - fraction), 0f, 1f) * 255);
        int g = (int) (Mth.clamp(2f * fraction, 0f, 1f) * 255);
        return 0xFF000000 | (r << 16) | (g << 8) | 0x20;
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2,
                             float z, int argb, int light) {
        vc.addVertex(m, x1, y1, z).setColor(argb).setLight(light);
        vc.addVertex(m, x1, y2, z).setColor(argb).setLight(light);
        vc.addVertex(m, x2, y2, z).setColor(argb).setLight(light);
        vc.addVertex(m, x2, y1, z).setColor(argb).setLight(light);
    }

    private MobNameplateRenderer() {}
}
