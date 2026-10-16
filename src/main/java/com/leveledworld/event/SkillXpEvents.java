package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.player.LevelingService;
import com.leveledworld.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Concede XP às skills conforme o jogador as usa. */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class SkillXpEvents {

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        float damage = event.getNewDamage();
        if (damage <= 0) return;
        DamageSource source = event.getSource();

        // Jogador causou dano
        if (source.getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker) {
            Entity direct = source.getDirectEntity();
            if (direct instanceof Projectile) {
                LevelingService.addSkillXp(attacker, Skill.ARCHERY, damage * 1.2);
            } else if (direct == attacker) {
                LevelingService.addSkillXp(attacker, Skill.ONE_HANDED, damage);
            }
        }

        // Jogador recebeu dano de alguma criatura usando armadura
        if (event.getEntity() instanceof ServerPlayer victim && source.getEntity() != null) {
            int armorPieces = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor() && victim.getItemBySlot(slot).getItem() instanceof ArmorItem) armorPieces++;
            }
            if (armorPieces > 0) {
                LevelingService.addSkillXp(victim, Skill.ARMOR, damage * 0.5 * armorPieces);
            }
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && event.getEntity() instanceof ServerPlayer player
                && event.getDamageSource().getEntity() != null) {
            LevelingService.addSkillXp(player, Skill.BLOCK, Math.max(1.0, event.getBlockedDamage()) * 1.5);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        if (state.is(Tags.Blocks.ORES)) {
            LevelingService.addSkillXp(player, Skill.MINING, 6.0);
        } else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            LevelingService.addSkillXp(player, Skill.MINING, 0.5);
        } else if (state.is(BlockTags.LOGS)) {
            LevelingService.addSkillXp(player, Skill.WOODCUTTING, 2.0);
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getCrafting();
        var item = stack.getItem();
        if (item instanceof ArmorItem || item instanceof TieredItem
                || item instanceof ProjectileWeaponItem || item instanceof ShieldItem) {
            LevelingService.addSkillXp(player, Skill.SMITHING, 8.0 * stack.getCount());
        }
    }

    private SkillXpEvents() {}
}
