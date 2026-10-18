package com.leveledworld.item;

import com.leveledworld.LeveledWorld;
import com.leveledworld.mob.MobTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Itens vanilla marcados com tier (couro Raro, lã Épica...). Continuam sendo o mesmo item e funcionam em qualquer
 * receita; o tier fica no componente {@code leveledworld:tier} e o nome ganha a cor do tier.
 */
public final class TierItems {
    /** Produtos de animais que herdam o tier de quem os produziu (editável por datapack). */
    public static final TagKey<Item> PRODUCTS = TagKey.create(Registries.ITEM, LeveledWorld.id("tiered_products"));

    public static MobTier get(ItemStack stack) {
        Integer tier = stack.get(RpgItems.TIER.get());
        return tier == null ? MobTier.COMMON : MobTier.byIndex(tier);
    }

    public static boolean hasTier(ItemStack stack) {
        return stack.has(RpgItems.TIER.get());
    }

    /** Marca o item com o tier (comum remove a marca) e pinta o nome na cor do tier. */
    public static void set(ItemStack stack, MobTier tier) {
        if (stack.isEmpty()) return;
        if (tier == MobTier.COMMON) {
            stack.remove(RpgItems.TIER.get());
            stack.remove(DataComponents.ITEM_NAME);
            return;
        }
        stack.set(RpgItems.TIER.get(), tier.ordinal());
        stack.set(DataComponents.ITEM_NAME, Component.translatable(stack.getItem().getDescriptionId()).withStyle(tier.style()));
    }

    public static void copyTier(ItemStack from, ItemStack to) {
        if (hasTier(from)) set(to, get(from));
    }

    public static boolean isProduct(ItemStack stack) {
        return stack.is(PRODUCTS);
    }

    private TierItems() {}
}
