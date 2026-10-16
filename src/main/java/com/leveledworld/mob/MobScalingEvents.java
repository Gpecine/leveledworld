package com.leveledworld.mob;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.player.RpgAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Escala cada mob hostil na primeira vez que ele entra no mundo (spawn natural, spawner, ovo, /summon...).
 * O level fica salvo no mob, então recarregar o chunk não muda nada.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class MobScalingEvents {

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        if (mob.hasData(RpgAttachments.MOB_LEVEL) || !LWConfig.SCALE_MOBS.get()) return;

        MobScaling.apply(mob, MobScaling.rollLevel(level, mob));
    }

    private MobScalingEvents() {}
}
