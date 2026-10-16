package com.leveledworld.player;

import com.leveledworld.config.LWConfig;
import com.leveledworld.network.RpgNetwork;
import com.leveledworld.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Regras de progressão: skills sobem com o uso e cada level de skill dá XP de personagem. */
public final class LevelingService {

    /** XP necessário para uma skill sair de {@code level} para o próximo (curva aproximada do Skyrim). */
    public static double skillXpToNext(int level) {
        return 0.5 * Math.pow(level, 1.95) + 10.0;
    }

    /** XP de personagem necessário para sair de {@code level} para o próximo (fórmula do Skyrim). */
    public static double characterXpToNext(int level) {
        return (level + 3) * 25.0;
    }

    public static PlayerRpgData get(ServerPlayer player) {
        return player.getData(RpgAttachments.PLAYER_DATA);
    }

    public static void addSkillXp(ServerPlayer player, Skill skill, double amount) {
        if (amount <= 0 || player.isCreative() || player.isSpectator()) return;
        amount *= LWConfig.SKILL_XP_MULTIPLIER.get();

        PlayerRpgData data = get(player);
        SkillProgress progress = data.skill(skill);
        int max = LWConfig.SKILL_MAX_LEVEL.get();
        if (progress.level() >= max) return;

        double xp = progress.xp() + amount;
        while (progress.level() < max && xp >= skillXpToNext(progress.level())) {
            xp -= skillXpToNext(progress.level());
            progress.setLevel(progress.level() + 1);
            onSkillLevelUp(player, skill, progress.level());
            // Skyrim: cada level de skill vale XP de personagem igual ao novo level da skill.
            addCharacterXp(player, progress.level(), false);
        }
        progress.setXp(progress.level() >= max ? 0 : xp);

        RpgNetwork.sync(player);
    }

    public static void addCharacterXp(ServerPlayer player, double amount, boolean sync) {
        PlayerRpgData data = get(player);
        int max = LWConfig.MAX_PLAYER_LEVEL.get();
        if (data.level() >= max) return;

        double xp = data.xp() + amount;
        while (data.level() < max && xp >= characterXpToNext(data.level())) {
            xp -= characterXpToNext(data.level());
            data.setLevel(data.level() + 1);
            data.setUnspentPoints(data.unspentPoints() + LWConfig.POINTS_PER_LEVEL.get());
            onCharacterLevelUp(player, data.level());
        }
        data.setXp(data.level() >= max ? 0 : xp);

        if (sync) RpgNetwork.sync(player);
    }

    public static void setCharacterLevel(ServerPlayer player, int level) {
        PlayerRpgData data = get(player);
        data.setLevel(Math.max(1, Math.min(level, LWConfig.MAX_PLAYER_LEVEL.get())));
        data.setXp(0);
        // Pontos disponíveis = pontos que o level dá menos os já gastos. Se baixou o level, devolve tudo.
        int earned = (data.level() - 1) * LWConfig.POINTS_PER_LEVEL.get();
        if (data.spentPoints() > earned) {
            AttributeService.resetPoints(player);
        }
        data.setUnspentPoints(Math.max(0, earned - data.spentPoints()));
        RpgNetwork.sync(player);
    }

    public static void setSkillLevel(ServerPlayer player, Skill skill, int level) {
        SkillProgress progress = get(player).skill(skill);
        progress.setLevel(Math.max(1, Math.min(level, LWConfig.SKILL_MAX_LEVEL.get())));
        progress.setXp(0);
        RpgNetwork.sync(player);
    }

    private static void onSkillLevelUp(ServerPlayer player, Skill skill, int newLevel) {
        player.displayClientMessage(Component.translatable("message.leveledworld.skill_up",
                Component.translatable(skill.translationKey()), newLevel).withStyle(ChatFormatting.AQUA), true);
    }

    private static void onCharacterLevelUp(ServerPlayer player, int newLevel) {
        player.sendSystemMessage(Component.translatable("message.leveledworld.level_up", newLevel)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.leveledworld.points_available")
                .withStyle(ChatFormatting.YELLOW));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private LevelingService() {}
}
