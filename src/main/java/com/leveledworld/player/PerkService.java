package com.leveledworld.player;

import com.leveledworld.RpgSounds;
import com.leveledworld.config.LWConfig;
import com.leveledworld.network.RpgNetwork;
import com.leveledworld.skill.PerkDef;
import com.leveledworld.skill.PerkTrees;
import com.leveledworld.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Compra de perks, skills lendárias e migração de saves antigos. */
public final class PerkService {

    public enum BuyResult { OK, NO_POINTS, MAX_RANK, SKILL_TOO_LOW, MISSING_REQUIREMENT, UNKNOWN }

    public static BuyResult check(PlayerRpgData data, PerkDef perk) {
        if (perk == null) return BuyResult.UNKNOWN;
        int rank = data.perkRank(perk.id);
        if (rank >= perk.maxRank()) return BuyResult.MAX_RANK;
        if (data.perkPoints() <= 0) return BuyResult.NO_POINTS;
        if (data.skill(perk.skill).level() < perk.levelForNextRank(rank)) return BuyResult.SKILL_TOO_LOW;
        if (!perk.requires.isEmpty()) {
            boolean ok = false;
            for (String r : perk.requires) {
                if (data.perkRank(perk.skill.id() + "." + r) > 0) { ok = true; break; }
            }
            if (!ok) return BuyResult.MISSING_REQUIREMENT;
        }
        return BuyResult.OK;
    }

    public static void buy(ServerPlayer player, String perkId) {
        PlayerRpgData data = LevelingService.get(player);
        PerkDef perk = PerkTrees.byId(perkId);
        BuyResult result = check(data, perk);
        if (result != BuyResult.OK) {
            player.displayClientMessage(Component.translatable("message.leveledworld.perk_buy_failed."
                    + result.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.RED), true);
            return;
        }
        int rank = data.perkRank(perk.id) + 1;
        data.setPerkRank(perk.id, rank);
        data.setPerkPoints(data.perkPoints() - 1);
        AttributeService.applyModifiers(player);
        player.displayClientMessage(Component.translatable("message.leveledworld.perk_learned",
                Component.translatable(perk.translationKey()).withStyle(ChatFormatting.GOLD),
                rank, perk.maxRank()).withStyle(ChatFormatting.YELLOW), true);
        player.playNotifySound(RpgSounds.PERK_UNLOCK.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        RpgNetwork.sync(player);
    }

    /** Torna a skill lendária: volta ao nível inicial e devolve os pontos de perk gastos nela. */
    public static void makeLegendary(ServerPlayer player, Skill skill) {
        PlayerRpgData data = LevelingService.get(player);
        SkillProgress progress = data.skill(skill);
        if (progress.level() < LWConfig.SKILL_MAX_LEVEL.get()) return;
        int refunded = 0;
        for (PerkDef p : PerkTrees.of(skill)) {
            refunded += data.perkRank(p.id);
            data.setPerkRank(p.id, 0);
        }
        data.setPerkPoints(data.perkPoints() + refunded);
        progress.setLevel(LWConfig.SKILL_START_LEVEL.get());
        progress.setXp(0);
        data.addLegendary(skill);
        AttributeService.applyModifiers(player);
        player.sendSystemMessage(Component.translatable("message.leveledworld.legendary",
                Component.translatable(skill.translationKey()).withStyle(ChatFormatting.GOLD), refunded)
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        player.playNotifySound(RpgSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 0.9F, 1.15F);
        player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.4F, 1.4F);
        RpgNetwork.sync(player);
    }

    /** Devolve todos os pontos de perk (comando de admin). */
    public static void resetPerks(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        int refunded = data.spentPerkPoints();
        for (String id : new java.util.ArrayList<>(data.perks().keySet())) data.setPerkRank(id, 0);
        data.setPerkPoints(Math.max(0, data.perkPoints()) + refunded);
        AttributeService.applyModifiers(player);
        RpgNetwork.sync(player);
    }

    /** Saves de antes da 0.8 não tinham pontos de perk: dá um por level já conquistado. */
    public static void migrate(ServerPlayer player) {
        PlayerRpgData data = LevelingService.get(player);
        if (data.perkPoints() < 0) {
            data.setPerkPoints(Math.max(0, data.level() - 1) * LWConfig.PERK_POINTS_PER_LEVEL.get());
            player.sendSystemMessage(Component.translatable("message.leveledworld.perk_migration", data.perkPoints())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private PerkService() {}
}
