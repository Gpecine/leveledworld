package com.leveledworld;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sons originais do mod (sintetizados em tools/gen_sounds.py). */
public final class RpgSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, LeveledWorld.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP = register("level_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> PERK_UNLOCK = register("perk_unlock");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(LeveledWorld.id(name)));
    }

    private RpgSounds() {}
}
