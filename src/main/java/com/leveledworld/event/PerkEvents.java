package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.network.ClientRpgData;
import com.leveledworld.network.SyncRpgDataPayload;
import com.leveledworld.player.AttributeService;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.PerkEffects;
import com.leveledworld.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Efeitos dos perks em jogo. Os valores vêm de {@link PerkEffects}. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class PerkEvents {

    /** Level da skill do jogador no lado em que o evento roda (o cliente precisa prever a velocidade de quebra). */
    private static int skillLevel(Player player, Skill skill) {
        if (player instanceof ServerPlayer sp) return LevelingService.get(sp).skill(skill).level();
        SyncRpgDataPayload data = ClientRpgData.get();
        return data != null && player.level().isClientSide ? data.skillLevels()[skill.ordinal()] : 0;
    }

    // Combate e Arquearia: bônus de dano e crítico.
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker) || event.getEntity() == attacker) return;
        Entity direct = source.getDirectEntity();
        double bonus;
        double crit;
        if (direct == attacker) {
            int lvl = skillLevel(attacker, Skill.ONE_HANDED);
            bonus = PerkEffects.meleeDamageBonus(lvl);
            crit = PerkEffects.meleeCritChance(lvl);
        } else if (direct instanceof Projectile) {
            int lvl = skillLevel(attacker, Skill.ARCHERY);
            bonus = PerkEffects.rangedDamageBonus(lvl);
            crit = PerkEffects.rangedCritChance(lvl);
        } else {
            return;
        }
        float amount = (float) (event.getAmount() * (1 + bonus));
        if (crit > 0 && attacker.getRandom().nextDouble() < crit) {
            amount *= 1.5f;
            LivingEntity target = event.getEntity();
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 14, 0.3, 0.3, 0.3, 0.25);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f);
            }
        }
        event.setAmount(amount);
    }

    // Armadura: menos dano recebido, proporcional às peças usadas.
    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == null) return;
        double reduction = PerkEffects.armorReduction(skillLevel(player, Skill.ARMOR));
        if (reduction <= 0) return;
        int pieces = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() && player.getItemBySlot(slot).getItem() instanceof ArmorItem) pieces++;
        }
        if (pieces == 0) return;
        event.setNewDamage((float) (event.getNewDamage() * (1 - reduction * pieces / 4.0)));
    }

    // Bloqueio: stamina, devolução de dano e empurrão.
    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player)) return;
        int lvl = skillLevel(player, Skill.BLOCK);
        AttributeService.restoreStamina(player, PerkEffects.blockStaminaRestore(lvl));
        if (!(event.getDamageSource().getEntity() instanceof LivingEntity attacker) || attacker == player) return;
        double reflect = PerkEffects.blockReflect(lvl);
        if (reflect > 0 && event.getBlockedDamage() > 0) {
            attacker.hurt(player.damageSources().thorns(player), (float) (event.getBlockedDamage() * reflect));
        }
        if (PerkEffects.blockKnockback(lvl)) {
            attacker.knockback(0.8, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
        }
    }

    // Mineração e Lenhador: velocidade (roda nos dois lados para a previsão do cliente bater).
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        double bonus = 0;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) && tool.is(ItemTags.PICKAXES)) {
            bonus = PerkEffects.miningSpeedBonus(skillLevel(player, Skill.MINING));
        } else if (state.is(BlockTags.LOGS) && tool.is(ItemTags.AXES)) {
            bonus = PerkEffects.woodSpeedBonus(skillLevel(player, Skill.WOODCUTTING));
        }
        if (bonus > 0) event.setNewSpeed((float) (event.getNewSpeed() * (1 + bonus)));
    }

    // Mineração e Lenhador: drops extras.
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || event.getDrops().isEmpty()) return;
        BlockState state = event.getState();
        double chance;
        if (state.is(Tags.Blocks.ORES)) {
            // Com Toque de Seda o drop é o próprio bloco: não duplica (evita exploit).
            boolean silk = event.getDrops().stream().anyMatch(d -> d.getItem().is(state.getBlock().asItem()));
            if (silk) return;
            chance = PerkEffects.oreDoubleChance(skillLevel(player, Skill.MINING));
        } else if (state.is(BlockTags.LOGS)) {
            chance = PerkEffects.logExtraChance(skillLevel(player, Skill.WOODCUTTING));
        } else {
            return;
        }
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) return;
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            extra.add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        }
        extra.forEach(ItemEntity::setDefaultPickUpDelay);
        event.getDrops().addAll(extra);
    }

    // Ferraria: chance do equipamento fabricado sair encantado.
    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getCrafting();
        if (stack.isEmpty() || !stack.isEnchantable() || stack.isEnchanted()) return;
        int lvl = skillLevel(player, Skill.SMITHING);
        double chance = PerkEffects.smithEnchantChance(lvl);
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) return;
        EnchantmentHelper.enchantItem(player.getRandom(), stack, PerkEffects.smithEnchantLevel(lvl),
                player.level().registryAccess(), Optional.empty());
        if (stack.isEnchanted()) {
            player.displayClientMessage(Component.translatable("message.leveledworld.smith_enchanted")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    private PerkEvents() {}
}
