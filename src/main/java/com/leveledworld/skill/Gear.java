package com.leveledworld.skill;

import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Classificação de armas e armaduras usada pelas skills. */
public final class Gear {
    public static boolean isSword(ItemStack stack) {
        return stack.is(ItemTags.SWORDS);
    }

    /** Duas Mãos: machados, maça e tridente (corpo a corpo). */
    public static boolean isTwoHanded(ItemStack stack) {
        return stack.is(ItemTags.AXES) || stack.is(Items.MACE) || stack.is(Items.TRIDENT);
    }

    public enum Weight { NONE, LIGHT, HEAVY }

    public static Weight weight(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) return Weight.NONE;
        Holder<ArmorMaterial> m = armor.getMaterial();
        ArmorMaterial v = m.value();
        if (v == ArmorMaterials.IRON.value() || v == ArmorMaterials.DIAMOND.value() || v == ArmorMaterials.NETHERITE.value()) {
            return Weight.HEAVY;
        }
        if (v == ArmorMaterials.LEATHER.value() || v == ArmorMaterials.CHAIN.value() || v == ArmorMaterials.GOLD.value()
                || v == ArmorMaterials.TURTLE.value() || v == ArmorMaterials.ARMADILLO.value()) {
            return Weight.LIGHT;
        }
        // Armaduras de outros mods: pela defesa do peitoral.
        return v.getDefense(ArmorItem.Type.CHESTPLATE) >= 6 ? Weight.HEAVY : Weight.LIGHT;
    }

    public static int count(LivingEntity entity, Weight weight) {
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() && slot != EquipmentSlot.BODY && weight(entity.getItemBySlot(slot)) == weight) n++;
        }
        return n;
    }

    /** Pontos de armadura vindos das peças do peso dado. */
    public static double armorFrom(LivingEntity entity, Weight weight) {
        double total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (slot.isArmor() && slot != EquipmentSlot.BODY && stack.getItem() instanceof ArmorItem armor && weight(stack) == weight) {
                total += armor.getDefense();
            }
        }
        return total;
    }

    public static boolean holdingShield(LivingEntity entity) {
        return entity.getMainHandItem().is(Items.SHIELD) || entity.getOffhandItem().is(Items.SHIELD);
    }

    private Gear() {}
}
