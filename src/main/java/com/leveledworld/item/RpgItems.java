package com.leveledworld.item;

import com.leveledworld.LeveledWorld;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Itens e componentes de dados do mod. */
public final class RpgItems {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, LeveledWorld.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LeveledWorld.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TomeData>> TOME = COMPONENTS.registerComponentType(
            "tome", b -> b.persistent(TomeData.CODEC).networkSynchronized(TomeData.STREAM_CODEC));

    /** Tier de raridade de um produto animal (couro, lã, carne...) ou de algo fabricado com ele. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIER = COMPONENTS.registerComponentType(
            "tier", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    /** Tomo de skill: encontrado no loot, dá níveis numa skill. */
    public static final DeferredItem<SkillTomeItem> SKILL_TOME = ITEMS.registerItem("skill_tome",
            p -> new SkillTomeItem(p, false), new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    /** Lição de treinador: vendida por aldeões, limitada por level do personagem. */
    public static final DeferredItem<SkillTomeItem> TRAINING_LESSON = ITEMS.registerItem("training_lesson",
            p -> new SkillTomeItem(p, true), new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(RpgItems::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (var skill : com.leveledworld.skill.Skill.values()) {
                event.accept(SkillTomeItem.create(SKILL_TOME.get(), skill, 1, 0, false));
            }
        }
    }

    private RpgItems() {}
}
