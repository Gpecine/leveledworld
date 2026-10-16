package com.leveledworld.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuração do servidor: fica em saves/<mundo>/serverconfig/leveledworld-server.toml. */
public final class LWConfig {
    public static final ModConfigSpec SPEC;

    // Progressão
    public static final ModConfigSpec.DoubleValue SKILL_XP_MULTIPLIER;
    public static final ModConfigSpec.IntValue SKILL_START_LEVEL;
    public static final ModConfigSpec.IntValue SKILL_MAX_LEVEL;
    public static final ModConfigSpec.IntValue MAX_PLAYER_LEVEL;

    // Escala de mobs
    public static final ModConfigSpec.BooleanValue SCALE_MOBS;
    public static final ModConfigSpec.IntValue PLAYER_SEARCH_RADIUS;
    public static final ModConfigSpec.DoubleValue MOB_LEVEL_MIN_FACTOR;
    public static final ModConfigSpec.DoubleValue MOB_LEVEL_MAX_FACTOR;
    public static final ModConfigSpec.IntValue MOB_MAX_LEVEL;
    public static final ModConfigSpec.IntValue BOSS_MIN_LEVEL;
    public static final ModConfigSpec.DoubleValue HEALTH_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue DAMAGE_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue ARMOR_PER_LEVEL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("progression");
        SKILL_XP_MULTIPLIER = b.comment("Multiplicador global de XP ganho nas skills.")
                .defineInRange("skillXpMultiplier", 1.0, 0.0, 100.0);
        SKILL_START_LEVEL = b.comment("Level inicial de cada skill (Skyrim usa 15).")
                .defineInRange("skillStartLevel", 15, 1, 100);
        SKILL_MAX_LEVEL = b.comment("Level máximo de cada skill.")
                .defineInRange("skillMaxLevel", 100, 1, 1000);
        MAX_PLAYER_LEVEL = b.comment("Level máximo do personagem.")
                .defineInRange("maxPlayerLevel", 100, 1, 1000);
        b.pop();

        b.push("mobScaling");
        SCALE_MOBS = b.comment("Se os mobs hostis devem escalar com o level do jogador.")
                .define("enabled", true);
        PLAYER_SEARCH_RADIUS = b.comment("Raio (em blocos) para encontrar o jogador que define o level do mob.")
                .defineInRange("playerSearchRadius", 128, 8, 1024);
        MOB_LEVEL_MIN_FACTOR = b.comment("Level do mob = level do jogador * aleatório entre minFactor e maxFactor.")
                .defineInRange("minFactor", 0.8, 0.0, 10.0);
        MOB_LEVEL_MAX_FACTOR = b.defineInRange("maxFactor", 1.2, 0.0, 10.0);
        MOB_MAX_LEVEL = b.comment("Level máximo que um mob pode ter.")
                .defineInRange("maxMobLevel", 150, 1, 10000);
        BOSS_MIN_LEVEL = b.comment("Level mínimo de bosses (Wither, Ender Dragon, Warden, Elder Guardian).")
                .defineInRange("bossMinLevel", 20, 1, 10000);
        HEALTH_PER_LEVEL = b.comment("Bônus de vida máxima por level acima do 1 (0.05 = +5% da vida base).")
                .defineInRange("healthPerLevel", 0.05, 0.0, 10.0);
        DAMAGE_PER_LEVEL = b.comment("Bônus de dano de ataque por level acima do 1 (0.03 = +3% do dano base).")
                .defineInRange("damagePerLevel", 0.03, 0.0, 10.0);
        ARMOR_PER_LEVEL = b.comment("Pontos de armadura somados por level acima do 1.")
                .defineInRange("armorPerLevel", 0.2, 0.0, 10.0);
        b.pop();

        SPEC = b.build();
    }

    private LWConfig() {}
}
