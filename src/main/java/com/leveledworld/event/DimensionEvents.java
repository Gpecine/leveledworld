package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.mob.DimensionRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Avisa o jogador ao entrar numa dimensão mais perigosa. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class DimensionEvents {

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DimensionRules.Rule rule = DimensionRules.of(event.getTo().location());
        if (!rule.harder()) return;
        var to = event.getTo().location();
        Component name = Component.translatableWithFallback(dimensionKey(to.getNamespace(), to.getPath()), to.getPath().replace('_', ' '));
        player.sendSystemMessage(Component.translatable("message.leveledworld.dimension_danger", name)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.leveledworld.dimension_details",
                rule.levelBonus(), rule.minLevel(), Math.round((rule.rewardMultiplier() - 1) * 100))
                .withStyle(ChatFormatting.GOLD));
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private static String dimensionKey(String namespace, String path) {
        return "dimension." + namespace + "." + path;
    }

    private DimensionEvents() {}
}
