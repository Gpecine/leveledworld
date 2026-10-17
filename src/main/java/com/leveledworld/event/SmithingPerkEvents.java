package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.PerkStats;
import com.leveledworld.skill.Skill;
import com.leveledworld.skill.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ferraria: equipamento fabricado ganha qualidade (Comum, Fina, Excelente, Obra-prima), assinatura do ferreiro,
 * durabilidade e atributos extras, e pode sair encantado. Também cuida da bigorna, economia e reciclagem.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class SmithingPerkEvents {
    private static final double[] QUALITY_BONUS = {0, 0.05, 0.10, 0.20};
    private static final ChatFormatting[] QUALITY_COLOR = {ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.GOLD};

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getCrafting();
        if (SkillXpEvents.smithingValue(stack) <= 0 || stack.isEmpty()) return;
        var random = player.getRandom();

        int quality = rollQuality(player, stack);
        applyQuality(player, stack, quality);

        double chance = PerkStats.get(player, Stat.SMITH_ENCHANT);
        if (chance > 0 && stack.isEnchantable() && !stack.isEnchanted() && random.nextDouble() < chance) {
            EnchantmentHelper.enchantItem(random, stack, (int) PerkStats.get(player, Stat.SMITH_ENCHANT_LEVEL),
                    player.level().registryAccess(), Optional.empty());
            if (stack.isEnchanted()) {
                player.displayClientMessage(Component.translatable("message.leveledworld.smith_enchanted").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
        }

        if (random.nextDouble() < PerkStats.get(player, Stat.CRAFT_REFUND)) refundIngredient(player, event.getInventory());
    }

    /** Sorteia a qualidade pelo nível de Ferraria e pelos perks do material. */
    private static int rollQuality(ServerPlayer player, ItemStack stack) {
        int level = LevelingService.get(player).skill(Skill.SMITHING).level();
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean materialPerk = name.startsWith("netherite") ? PerkStats.has(player, Stat.QUALITY_NETHERITE)
                : name.startsWith("diamond") ? PerkStats.has(player, Stat.QUALITY_DIAMOND)
                : name.startsWith("iron") || name.startsWith("chainmail") ? PerkStats.has(player, Stat.QUALITY_IRON)
                : level >= 30;
        var random = player.getRandom();
        int quality = 0;
        double fine = Math.min(0.5, level / 200.0) + (materialPerk ? 0.25 : 0);
        if (random.nextDouble() < fine) quality = 1;
        if (materialPerk && level >= 50 && random.nextDouble() < (level - 40) / 200.0) quality = 2;
        if (random.nextDouble() < PerkStats.get(player, Stat.MASTERWORK)) quality = 3;
        if (PerkStats.has(player, Stat.MIN_FINE)) quality = Math.max(quality, 1);
        return quality;
    }

    private static void applyQuality(ServerPlayer player, ItemStack stack, int quality) {
        double q = QUALITY_BONUS[quality];
        Item item = stack.getItem();
        ItemAttributeModifiers mods = stack.getAttributeModifiers();
        var id = LeveledWorld.id("smith_quality");

        double baseDamage = 0;
        double baseArmor = 0;
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)) {
                baseDamage = e.modifier().amount() + 1;
            }
            if (e.attribute().equals(Attributes.ARMOR)) baseArmor = Math.max(baseArmor, e.modifier().amount());
        }
        if (baseDamage > 0) {
            double extra = baseDamage * q + PerkStats.get(player, Stat.SMITH_WEAPON_DAMAGE);
            if (extra > 0) mods = mods.withModifierAdded(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(id, extra, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        if (item instanceof ArmorItem armor && baseArmor > 0) {
            double extra = baseArmor * q + PerkStats.get(player, Stat.SMITH_ARMOR);
            if (extra > 0) mods = mods.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(id, extra, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot()));
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);

        if (stack.isDamageableItem()) {
            double durability = 1 + PerkStats.get(player, Stat.SMITH_DURABILITY) + q;
            stack.set(DataComponents.MAX_DAMAGE, (int) Math.round(stack.getMaxDamage() * durability));
        }

        List<Component> lore = new ArrayList<>();
        Style plain = Style.EMPTY.withItalic(false);
        lore.add(Component.translatable("item.leveledworld.quality." + quality).withStyle(plain.withColor(QUALITY_COLOR[quality])));
        lore.add(Component.translatable("item.leveledworld.forged_by", player.getName()).withStyle(plain.withColor(ChatFormatting.GRAY)));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (quality > 0) {
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.leveledworld.quality." + quality)
                    .append(" ").append(stack.getHoverName()).withStyle(plain.withColor(QUALITY_COLOR[quality])));
        }
        if (quality == 3) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.6f);
            player.displayClientMessage(Component.translatable("message.leveledworld.masterwork").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Economia: devolve um dos ingredientes usados. */
    private static void refundIngredient(ServerPlayer player, Container grid) {
        List<ItemStack> used = new ArrayList<>();
        for (int i = 0; i < grid.getContainerSize(); i++) if (!grid.getItem(i).isEmpty()) used.add(grid.getItem(i));
        if (used.isEmpty()) return;
        ItemStack back = used.get(player.getRandom().nextInt(used.size())).copyWithCount(1);
        if (back.hasCraftingRemainingItem()) return;
        if (!player.getInventory().add(back)) player.drop(back, false);
    }

    @SubscribeEvent
    public static void onAnvil(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (PerkStats.has(player, Stat.ANVIL_SAFE)) event.setBreakChance(0);
        double discount = PerkStats.get(player, Stat.ANVIL_DISCOUNT);
        if (discount > 0 && player.containerMenu instanceof AnvilMenu menu && !player.getAbilities().instabuild) {
            int refund = (int) Math.floor(menu.getCost() * discount);
            if (refund > 0) player.giveExperienceLevels(refund);
        }
    }

    /** Reciclagem: derreter equipamento devolve pepitas extras. */
    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getSmelting();
        if (!result.is(Items.IRON_NUGGET) && !result.is(Items.GOLD_NUGGET)) return;
        int extra = (int) PerkStats.get(player, Stat.RECYCLE) * result.getCount();
        if (extra > 0) {
            ItemStack bonus = new ItemStack(result.getItem(), extra);
            if (!player.getInventory().add(bonus)) player.drop(bonus, false);
        }
    }

    private SmithingPerkEvents() {}
}
