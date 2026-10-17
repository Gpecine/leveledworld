package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Stat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Mão Rápida / Besteiro: puxa o arco e carrega a besta mais rápido. Roda de forma determinística nos dois lados
 * (cliente e servidor) para a animação e o disparo baterem.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class ItemUsePerkEvents {

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        double speed;
        if (stack.getItem() instanceof BowItem) {
            speed = PerkStats.get(player, Stat.DRAW_SPEED);
        } else if (stack.getItem() instanceof CrossbowItem) {
            speed = PerkStats.get(player, Stat.DRAW_SPEED) + (PerkStats.has(player, Stat.CROSSBOW_DAMAGE) ? 0.15 : 0);
        } else {
            return;
        }
        if (speed <= 0) return;
        int used = stack.getUseDuration(player) - event.getDuration();
        int extra = (int) ((used + 1) * speed) - (int) (used * speed);
        if (extra > 0) event.setDuration(Math.max(0, event.getDuration() - extra));
    }

    private ItemUsePerkEvents() {}
}
