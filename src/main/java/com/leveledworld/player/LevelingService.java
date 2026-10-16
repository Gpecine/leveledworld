package com.leveledworld.player;

import com.leveledworld.RpgSounds;
import com.leveledworld.config.LWConfig;
import com.leveledworld.network.RpgNetwork;
import com.leveledworld.skill.Perk;
import com.leveledworld.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Regras de progressão: skills sobem com o uso e cada level de skill dá XP de personagem. */
public final class LevelingService {
    /** Intervalo mínimo entre dois sons de level up / perk para o mesmo jogador (evita som empilhado). */
    private static final int SOUND_COOLDOWN_TICKS = 60;

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
        double characterXp = 0;
        boolean perkUnlocked = false;
        while (progress.level() < max && xp >= skillXpToNext(progress.level())) {
            xp -= skillXpToNext(progress.level());
            progress.setLevel(progress.level() + 1);
            perkUnlocked |= onSkillLevelUp(player, skill, progress.level());
            // Skyrim: cada level de skill vale XP de personagem proporcional ao novo level da skill.
            characterXp += progress.level() * LWConfig.CHARACTER_XP_PER_SKILL_LEVEL.get();
        }
        progress.setXp(progress.level() >= max ? 0 : xp);

        boolean leveledUp = characterXp > 0 && addCharacterXp(player, characterXp, false);
        if (perkUnlocked && !leveledUp) playOnce(player, false);
        RpgNetwork.sync(player);
    }

    /** Soma XP de personagem; pode subir vários levels, mas toca o som e manda a mensagem uma vez só. */
    public static boolean addCharacterXp(ServerPlayer player, double amount, boolean sync) {
        PlayerRpgData data = get(player);
        int max = LWConfig.MAX_PLAYER_LEVEL.get();
        if (data.level() >= max) return false;

        int before = data.level();
        double xp = data.xp() + amount;
        while (data.level() < max && xp >= characterXpToNext(data.level())) {
            xp -= characterXpToNext(data.level());
            data.setLevel(data.level() + 1);
            data.setUnspentPoints(data.unspentPoints() + LWConfig.POINTS_PER_LEVEL.get());
        }
        data.setXp(data.level() >= max ? 0 : xp);

        boolean leveledUp = data.level() > before;
        if (leveledUp) onCharacterLevelUp(player, data.level());
        if (sync) RpgNetwork.sync(player);
        return leveledUp;
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

    /** Retorna true se o novo level desbloqueou um perk. */
    private static boolean onSkillLevelUp(ServerPlayer player, Skill skill, int newLevel) {
        player.displayClientMessage(Component.translatable("message.leveledworld.skill_up",
                Component.translatable(skill.translationKey()), newLevel).withStyle(ChatFormatting.AQUA), true);
        boolean unlocked = false;
        for (Perk perk : Perk.of(skill)) {
            if (perk.level == newLevel) {
                player.sendSystemMessage(Component.translatable("message.leveledworld.perk_unlocked",
                                Component.translatable(perk.translationKey()).withStyle(ChatFormatting.GOLD),
                                Component.translatable(perk.translationKey() + ".desc").withStyle(ChatFormatting.GRAY))
                        .withStyle(ChatFormatting.YELLOW));
                unlocked = true;
            }
        }
        return unlocked;
    }

    private static void onCharacterLevelUp(ServerPlayer player, int newLevel) {
        player.sendSystemMessage(Component.translatable("message.leveledworld.level_up", newLevel)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("message.leveledworld.points_available")
                .withStyle(ChatFormatting.YELLOW));
        playOnce(player, true);
    }

    /** Toca o som de level up (ou de perk) só para o jogador, no máximo uma vez a cada poucos segundos. */
    private static void playOnce(ServerPlayer player, boolean levelUp) {
        PlayerRpgData data = get(player);
        if (player.tickCount - data.lastLevelSoundTick < SOUND_COOLDOWN_TICKS && data.lastLevelSoundTick != 0) return;
        data.lastLevelSoundTick = player.tickCount;
        player.playNotifySound(levelUp ? RpgSounds.LEVEL_UP.get() : RpgSounds.PERK_UNLOCK.get(),
                SoundSource.PLAYERS, levelUp ? 0.9F : 0.8F, 1.0F);
    }

    private LevelingService() {}
}
