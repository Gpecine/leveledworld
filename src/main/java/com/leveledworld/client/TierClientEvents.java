package com.leveledworld.client;

import com.leveledworld.LeveledWorld;
import com.leveledworld.item.TierItems;
import com.leveledworld.mob.MobTier;
import com.leveledworld.player.RpgAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Vector3f;

import java.util.List;

/** Partículas em volta de mobs Épicos ou melhores e a descrição dos itens com tier. */
@EventBusSubscriber(modid = LeveledWorld.MODID, value = Dist.CLIENT)
public final class TierClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        RandomSource random = mc.level.random;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Mob mob) || !mob.hasData(RpgAttachments.MOB_TIER) || mob.isInvisible()) continue;
            MobTier tier = MobTier.byIndex(mob.getData(RpgAttachments.MOB_TIER));
            if (tier.ordinal() < MobTier.EPIC.ordinal() || mob.distanceToSqr(mc.player) > 32 * 32) continue;
            // Épico: brilho esparso; Lendário: mais denso; Exótico: denso + faíscas.
            int every = tier == MobTier.EPIC ? 6 : tier == MobTier.LEGENDARY ? 3 : 2;
            if ((mob.tickCount + mob.getId()) % every != 0) continue;
            int color = MobNameplateRenderer.tierColor(tier);
            Vector3f rgb = new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f);
            double x = mob.getX() + (random.nextDouble() - 0.5) * mob.getBbWidth() * 1.4;
            double y = mob.getY() + random.nextDouble() * mob.getBbHeight();
            double z = mob.getZ() + (random.nextDouble() - 0.5) * mob.getBbWidth() * 1.4;
            mc.level.addParticle(new DustParticleOptions(rgb, 1.0f), x, y, z, 0, 0.02, 0);
            if (tier == MobTier.EXOTIC && random.nextInt(4) == 0) {
                mc.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.03, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!TierItems.hasTier(stack)) return;
        MobTier tier = TierItems.get(stack);
        List<Component> lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        lines.add(at++, Component.literal("◆ ").append(Component.translatable(tier.translationKey())).withStyle(tier.style()));
        String pct;
        if (stack.has(DataComponents.FOOD) || stack.is(Items.MILK_BUCKET)) {
            pct = Math.round(tier.food * 100) + "%";
            if (stack.has(DataComponents.FOOD)) {
                lines.add(at++, Component.translatable("tooltip.leveledworld.tier_food", pct).withStyle(ChatFormatting.GRAY));
            }
            if (tier.ordinal() >= MobTier.EPIC.ordinal()) {
                lines.add(at++, Component.translatable("tooltip.leveledworld.tier_food_effect." + tier.id()).withStyle(ChatFormatting.GRAY));
            }
        } else if (stack.isDamageableItem() || stack.getItem() instanceof ArmorItem) {
            pct = Math.round(tier.material * 100) + "%";
            lines.add(at, Component.translatable("tooltip.leveledworld.tier_material", pct).withStyle(ChatFormatting.GRAY));
            return;  // equipamento não é vendido como produto
        } else {
            pct = Math.round(tier.material * 100) + "%";
            lines.add(at++, Component.translatable("tooltip.leveledworld.tier_product", pct).withStyle(ChatFormatting.GRAY));
        }
        String sale = String.format(java.util.Locale.ROOT, "%.2f", tier.sale).replaceAll("0+$", "").replaceAll("\\.$", "");
        if (Minecraft.getInstance().options.languageCode.startsWith("pt")) sale = sale.replace('.', ',');
        lines.add(at, Component.translatable("tooltip.leveledworld.tier_sale", "×" + sale).withStyle(ChatFormatting.DARK_GRAY));
    }

    private TierClientEvents() {}
}
