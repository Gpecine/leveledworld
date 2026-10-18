package com.leveledworld.mob;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.item.TierItems;
import com.leveledworld.player.RpgAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tiers de mobs: sorteio ao nascer, herança na reprodução, drops e XP extras, produtos dos animais marcados com
 * tier (morte, tosquia, ovos, leite), comida e equipamento melhores com material de tier e venda mais cara a aldeões.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class MobTierEvents {
    private static final ResourceKey<LootTable> BONUS_COMMON = table("bonus/common");
    private static final ResourceKey<LootTable> BONUS_UNCOMMON = table("bonus/uncommon");
    private static final ResourceKey<LootTable> BONUS_RARE = table("bonus/rare");
    private static final ResourceKey<LootTable> TIER_LEGENDARY = table("tier/legendary");
    private static final ResourceKey<LootTable> TIER_EXOTIC = table("tier/exotic");

    /** Ações para o próximo tick (o vanilla termina a tosquia, o ovo e o leite depois dos nossos eventos). */
    private static final List<Runnable> NEXT_TICK = new ArrayList<>();
    /** Ovelhas tosquiadas neste tick: a lã que aparecer ao lado herda o tier. */
    private static final List<PendingProduct> PENDING = new ArrayList<>();

    private record PendingProduct(ServerLevel level, Vec3 pos, MobTier tier, long tick) {}

    private static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, LeveledWorld.id(path));
    }

    // ---------- nascimento ----------

    /** Sorteia o tier de mobs novos (depois do level, que entra no sorteio). Mobs salvos mantêm o seu. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof ItemEntity item) {
            onProductSpawn(level, item);
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !MobTiers.canHaveTier(mob) || mob.hasData(RpgAttachments.MOB_TIER)) return;
        MobTier tier = MobTiers.roll(level, mob);
        MobTiers.apply(mob, tier, true);
        announce(level, mob, tier);
    }

    private static void announce(ServerLevel level, Mob mob, MobTier tier) {
        MobTier from = MobTier.byId(LWConfig.TIER_ANNOUNCE.get());
        if (from == null || tier.ordinal() < from.ordinal() || tier == MobTier.COMMON) return;
        Component msg = Component.translatable("message.leveledworld.tier_spawn", tier.displayName(),
                mob.getType().getDescription().copy().withStyle(tier.style()));
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(mob) <= 48 * 48) {
                p.displayClientMessage(msg, false);
                p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1f, 0.7f);
            }
        }
    }

    /** Filhote herda o tier dos pais (90% igual, 9% sobe, 1% desce). */
    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getChild() instanceof Mob child) || !MobTiers.canHaveTier(child)) return;
        MobTier tier = MobTiers.inherit(event.getParentA(), event.getParentB(), child.getRandom());
        MobTiers.apply(child, tier, true);
        if (event.getCausedByPlayer() instanceof ServerPlayer player && tier.ordinal() >= MobTier.RARE.ordinal()) {
            player.displayClientMessage(Component.translatable("message.leveledworld.tier_baby", tier.displayName()), true);
        }
    }

    // ---------- morte: XP, drops e loot ----------

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        MobTier tier = MobTiers.get(event.getEntity());
        if (tier != MobTier.COMMON) event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * tier.xp));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        MobTier tier = MobTiers.get(mob);
        if (tier == MobTier.COMMON) return;
        boolean animal = !MobTiers.isAggressive(mob);
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.getMaxStackSize() > 1) {
                ItemStack grown = stack.copy();
                grown.setCount(Math.min(stack.getMaxStackSize(), scale(stack.getCount(), tier.drops, mob.getRandom())));
                stack = grown;
            }
            if (animal && TierItems.isProduct(stack)) {
                stack = stack.copy();
                TierItems.set(stack, tier);
            }
            drop.setItem(stack);
        }
        if (!animal) rollTierLoot(event, level, mob, tier);
    }

    /** Agressivos de tier alto rolam tabelas de loot melhores (diamantes, equipamento e livros encantados...). */
    private static void rollTierLoot(LivingDropsEvent event, ServerLevel level, Mob mob, MobTier tier) {
        Player killer = MobRewardEvents.killer(event);
        if (killer == null) return;
        LootParams params = MobRewardEvents.lootParams(event, level, mob, killer);
        var random = mob.getRandom();
        switch (tier) {
            case UNCOMMON -> {
                if (random.nextDouble() < 0.30) roll(event, level, mob, BONUS_COMMON, params);
            }
            case RARE -> roll(event, level, mob, BONUS_UNCOMMON, params);
            case EPIC -> {
                roll(event, level, mob, BONUS_RARE, params);
                if (random.nextDouble() < 0.25) roll(event, level, mob, BONUS_UNCOMMON, params);
            }
            case LEGENDARY -> {
                roll(event, level, mob, TIER_LEGENDARY, params);
                roll(event, level, mob, BONUS_RARE, params);
            }
            case EXOTIC -> {
                roll(event, level, mob, TIER_EXOTIC, params);
                roll(event, level, mob, TIER_LEGENDARY, params);
            }
            default -> { }
        }
        if (tier.ordinal() >= MobTier.LEGENDARY.ordinal()) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 0.6f);
        }
    }

    private static void roll(LivingDropsEvent event, ServerLevel level, Mob mob, ResourceKey<LootTable> key, LootParams params) {
        MobRewardEvents.drop(event, level, mob, level.getServer().reloadableRegistries().getLootTable(key), params, true);
    }

    /** Quantidade × multiplicador, com a parte fracionária sorteada (1 × 1,5 = 1 ou 2, meio a meio). */
    private static int scale(int count, double multiplier, net.minecraft.util.RandomSource random) {
        double total = count * multiplier;
        int whole = (int) total;
        return whole + (random.nextDouble() < total - whole ? 1 : 0);
    }

    // ---------- produtos em vida: lã, ovos, leite ----------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        MobTier tier = MobTiers.get(event.getTarget());
        if (tier == MobTier.COMMON) return;
        ItemStack held = event.getItemStack();
        if (event.getTarget() instanceof Sheep sheep && held.is(Tags.Items.TOOLS_SHEAR) && sheep.readyForShearing()) {
            PENDING.add(new PendingProduct(level, sheep.position(), tier, level.getGameTime()));
        } else if ((event.getTarget() instanceof Cow || event.getTarget() instanceof Goat) && held.is(Items.BUCKET)
                && !((Animal) event.getTarget()).isBaby()) {
            NEXT_TICK.add(() -> markMilk(player, tier));
        }
    }

    /** Lã da tosquia e ovos postos: herdam o tier da ovelha/galinha e vêm em maior quantidade. */
    private static void onProductSpawn(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();
        if (stack.is(ItemTags.WOOL)) {
            long now = level.getGameTime();
            for (PendingProduct p : PENDING) {
                if (p.level() == level && now - p.tick() <= 1 && p.pos().distanceToSqr(item.position()) < 9) {
                    applyProduct(item, p.tier());
                    return;
                }
            }
        } else if (stack.is(Items.EGG) && !TierItems.hasTier(stack)) {
            for (Chicken chicken : level.getEntitiesOfClass(Chicken.class, item.getBoundingBox().inflate(0.6))) {
                if (chicken.eggTime > 0) continue;  // acabou de botar: o vanilla ainda não sorteou o próximo
                MobTier tier = MobTiers.get(chicken);
                if (tier == MobTier.COMMON) return;
                applyProduct(item, tier);
                // Galinhas de tier alto botam mais rápido.
                NEXT_TICK.add(() -> chicken.eggTime = (int) (chicken.eggTime / tier.products));
                return;
            }
        }
    }

    private static void applyProduct(ItemEntity item, MobTier tier) {
        ItemStack stack = item.getItem().copy();
        stack.setCount(Math.min(stack.getMaxStackSize(), scale(stack.getCount(), tier.products, item.getRandom())));
        TierItems.set(stack, tier);
        item.setItem(stack);
    }

    private static void markMilk(ServerPlayer player, MobTier tier) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.is(Items.MILK_BUCKET) && !TierItems.hasTier(stack)) {
                TierItems.set(stack, tier);
                return;
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.MILK_BUCKET) && !TierItems.hasTier(stack)) {
                TierItems.set(stack, tier);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!NEXT_TICK.isEmpty()) {
            List<Runnable> tasks = new ArrayList<>(NEXT_TICK);
            NEXT_TICK.clear();
            tasks.forEach(Runnable::run);
        }
        if (!PENDING.isEmpty()) {
            Iterator<PendingProduct> it = PENDING.iterator();
            while (it.hasNext()) {
                PendingProduct p = it.next();
                if (p.level().getGameTime() - p.tick() > 1) it.remove();
            }
        }
    }

    // ---------- comida ----------

    /** Comida (e leite) de tier: mais nutrição e, nos tiers altos, efeitos. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        MobTier tier = TierItems.get(stack);
        if (tier == MobTier.COMMON) return;
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null) {
            int extra = (int) Math.round(food.nutrition() * tier.food);
            if (extra > 0) player.getFoodData().eat(extra, 0.6f);
        }
        switch (tier) {
            case EPIC -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            case LEGENDARY -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
            }
            case EXOTIC -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
            }
            default -> { }
        }
    }

    // ---------- fabricação ----------

    /**
     * Fabricar com produtos de tier: o resultado leva o tier do pior ingrediente animal. Comida e produtos
     * (couro de peles de coelho, torta) ficam marcados; equipamento ganha armadura e durabilidade.
     */
    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ItemStack result = event.getCrafting();
        Container grid = event.getInventory();
        MobTier tier = null;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack in = grid.getItem(i);
            if (in.isEmpty() || !TierItems.isProduct(in)) continue;
            MobTier t = TierItems.get(in);
            tier = tier == null || t.ordinal() < tier.ordinal() ? t : tier;
        }
        if (tier == null || tier == MobTier.COMMON || result.isEmpty()) return;
        if (TierItems.isProduct(result) || result.has(DataComponents.FOOD)) {
            TierItems.set(result, tier);
        } else if (result.isDamageableItem() || result.getItem() instanceof ArmorItem) {
            applyMaterial(result, tier);
        }
    }

    private static void applyMaterial(ItemStack stack, MobTier tier) {
        if (stack.getItem() instanceof ArmorItem armor) {
            ItemAttributeModifiers mods = stack.getAttributeModifiers();
            double base = 0;
            for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
                if (e.attribute().equals(Attributes.ARMOR)) base = Math.max(base, e.modifier().amount());
            }
            double extra = Math.max(0.5, base * tier.material);
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods.withModifierAdded(Attributes.ARMOR,
                    new AttributeModifier(LeveledWorld.id("material_tier"), extra, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.bySlot(armor.getEquipmentSlot())));
        }
        if (stack.isDamageableItem()) {
            stack.set(DataComponents.MAX_DAMAGE, (int) Math.round(stack.getMaxDamage() * (1 + tier.material)));
        }
        TierItems.set(stack, tier);
    }

    // ---------- aldeões ----------

    /**
     * Vender produtos de tier a aldeões paga mais esmeraldas: enquanto a troca está aberta, o resultado é ajustado
     * pelo tier do item oferecido (o servidor manda a quantidade certa para a tela).
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.containerMenu instanceof MerchantMenu menu)) return;
        if (!(menu.getSlot(2).container instanceof MerchantContainer container)) return;
        MerchantOffer offer = container.getActiveOffer();
        ItemStack result = container.getItem(2);
        if (offer == null || result.isEmpty() || !offer.getResult().is(Items.EMERALD) || !result.is(Items.EMERALD)) return;
        MobTier best = MobTier.COMMON;
        for (int i = 0; i < 2; i++) {
            ItemStack pay = container.getItem(i);
            if (pay.is(offer.getCostA().getItem()) && TierItems.hasTier(pay)) {
                MobTier t = TierItems.get(pay);
                if (t.ordinal() > best.ordinal()) best = t;
            }
        }
        int base = offer.getResult().getCount();
        int wanted = Math.min(result.getMaxStackSize(), Math.max(base, (int) Math.round(base * best.sale)));
        if (result.getCount() != wanted) {
            ItemStack adjusted = result.copy();
            adjusted.setCount(wanted);
            container.setItem(2, adjusted);
        }
    }

    /** Usado pelo comando de admin: troca o tier e avisa quem enxerga o mob. */
    public static void setTier(Mob mob, MobTier tier) {
        MobTiers.apply(mob, tier, false);
        MobTiers.resync(mob);
    }

    private MobTierEvents() {}
}
