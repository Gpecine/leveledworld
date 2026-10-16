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

    // Atributos
    public static final ModConfigSpec.IntValue POINTS_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue HEALTH_PER_POINT;
    public static final ModConfigSpec.DoubleValue BASE_STAMINA;
    public static final ModConfigSpec.DoubleValue STAMINA_PER_POINT;
    public static final ModConfigSpec.DoubleValue BASE_MAGICKA;
    public static final ModConfigSpec.DoubleValue MAGICKA_PER_POINT;
    public static final ModConfigSpec.DoubleValue SPRINT_STAMINA_PER_SECOND;
    public static final ModConfigSpec.DoubleValue JUMP_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue ATTACK_STAMINA_COST;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN_PER_SECOND;
    public static final ModConfigSpec.IntValue STAMINA_REGEN_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue MAGICKA_REGEN_PER_SECOND;

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

    // Recompensas
    public static final ModConfigSpec.DoubleValue XP_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue DROPS_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue BONUS_LOOT_CHANCE_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue BONUS_LOOT_MAX_CHANCE;
    public static final ModConfigSpec.IntValue UNCOMMON_LOOT_LEVEL;
    public static final ModConfigSpec.IntValue RARE_LOOT_LEVEL;

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

        b.push("attributes");
        POINTS_PER_LEVEL = b.comment("Pontos de atributo ganhos a cada level do personagem.")
                .defineInRange("pointsPerLevel", 1, 0, 100);
        HEALTH_PER_POINT = b.comment("Vida máxima ganha por ponto em Vida (2 = 1 coração).")
                .defineInRange("healthPerPoint", 2.0, 0.0, 100.0);
        BASE_STAMINA = b.comment("Stamina máxima sem pontos.")
                .defineInRange("baseStamina", 100.0, 1.0, 10000.0);
        STAMINA_PER_POINT = b.comment("Stamina máxima ganha por ponto em Stamina.")
                .defineInRange("staminaPerPoint", 10.0, 0.0, 1000.0);
        BASE_MAGICKA = b.comment("Mana máxima sem pontos.")
                .defineInRange("baseMagicka", 100.0, 1.0, 10000.0);
        MAGICKA_PER_POINT = b.comment("Mana máxima ganha por ponto em Mana.")
                .defineInRange("magickaPerPoint", 10.0, 0.0, 1000.0);
        SPRINT_STAMINA_PER_SECOND = b.comment("Stamina gasta por segundo correndo. Sem stamina não dá para correr.")
                .defineInRange("sprintCostPerSecond", 10.0, 0.0, 1000.0);
        JUMP_STAMINA_COST = b.comment("Stamina gasta por pulo.")
                .defineInRange("jumpCost", 4.0, 0.0, 1000.0);
        ATTACK_STAMINA_COST = b.comment("Stamina gasta por ataque corpo-a-corpo.")
                .defineInRange("attackCost", 3.0, 0.0, 1000.0);
        STAMINA_REGEN_PER_SECOND = b.comment("Stamina recuperada por segundo.")
                .defineInRange("staminaRegenPerSecond", 15.0, 0.0, 1000.0);
        STAMINA_REGEN_DELAY_TICKS = b.comment("Ticks sem gastar stamina antes de começar a recuperar (20 = 1s).")
                .defineInRange("staminaRegenDelay", 20, 0, 1200);
        MAGICKA_REGEN_PER_SECOND = b.comment("Mana recuperada por segundo.")
                .defineInRange("magickaRegenPerSecond", 3.0, 0.0, 1000.0);
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

        b.push("rewards");
        XP_PER_LEVEL = b.comment("Bônus de orbes de XP por level do mob acima do 1 (0.05 = +5%).")
                .defineInRange("xpPerLevel", 0.05, 0.0, 10.0);
        DROPS_PER_LEVEL = b.comment("Bônus de quantidade nos drops empilháveis por level acima do 1 (0.02 = +2%).")
                .defineInRange("dropsPerLevel", 0.02, 0.0, 10.0);
        BONUS_LOOT_CHANCE_PER_LEVEL = b.comment("Chance de loot bônus por level do mob (0.01 = 1% por level). Só quando morto por jogador.")
                .defineInRange("bonusLootChancePerLevel", 0.01, 0.0, 1.0);
        BONUS_LOOT_MAX_CHANCE = b.comment("Chance máxima de loot bônus.")
                .defineInRange("bonusLootMaxChance", 0.5, 0.0, 1.0);
        UNCOMMON_LOOT_LEVEL = b.comment("Level do mob a partir do qual o loot bônus é incomum (data/leveledworld/loot_table/bonus/uncommon.json).")
                .defineInRange("uncommonLootLevel", 15, 1, 10000);
        RARE_LOOT_LEVEL = b.comment("Level do mob a partir do qual o loot bônus é raro (bonus/rare.json).")
                .defineInRange("rareLootLevel", 35, 1, 10000);
        b.pop();

        SPEC = b.build();
    }

    private LWConfig() {}
}
