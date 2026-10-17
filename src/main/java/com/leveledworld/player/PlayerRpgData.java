package com.leveledworld.player;

import com.leveledworld.config.LWConfig;
import com.leveledworld.skill.Skill;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Dados RPG do jogador (level, skills, perks, atributos e recursos). Salvo como attachment e copiado ao morrer. */
public final class PlayerRpgData {
    // Campos novos são opcionais para manter compatibilidade com mundos salvos em versões anteriores.
    public static final Codec<PlayerRpgData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(PlayerRpgData::level),
            Codec.DOUBLE.fieldOf("xp").forGetter(PlayerRpgData::xp),
            Codec.unboundedMap(Codec.STRING, SkillProgress.CODEC).fieldOf("skills").forGetter(PlayerRpgData::skillsById),
            Codec.INT.optionalFieldOf("unspent_points", 0).forGetter(PlayerRpgData::unspentPoints),
            Codec.INT.optionalFieldOf("health_points", 0).forGetter(d -> d.points(RpgAttribute.HEALTH)),
            Codec.INT.optionalFieldOf("stamina_points", 0).forGetter(d -> d.points(RpgAttribute.STAMINA)),
            Codec.INT.optionalFieldOf("magicka_points", 0).forGetter(d -> d.points(RpgAttribute.MAGICKA)),
            Codec.FLOAT.optionalFieldOf("stamina", -1f).forGetter(PlayerRpgData::stamina),
            Codec.FLOAT.optionalFieldOf("magicka", -1f).forGetter(PlayerRpgData::magicka),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("perks", Map.of()).forGetter(d -> d.perks),
            Codec.INT.optionalFieldOf("perk_points", -1).forGetter(PlayerRpgData::perkPoints),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("legendary", Map.of()).forGetter(d -> d.legendary),
            Codec.INT.optionalFieldOf("trainings", 0).forGetter(PlayerRpgData::trainingsThisLevel)
    ).apply(i, PlayerRpgData::new));

    private int level;
    private double xp;
    private final EnumMap<Skill, SkillProgress> skills = new EnumMap<>(Skill.class);
    private int unspentPoints;
    private final EnumMap<RpgAttribute, Integer> attributePoints = new EnumMap<>(RpgAttribute.class);
    /** Valores atuais dos recursos; -1 = ainda não inicializado (enche no primeiro tick). */
    private float stamina;
    private float magicka;
    /** Perks comprados: id completo ("skill.perk") → rank. */
    private final Map<String, Integer> perks = new HashMap<>();
    /** -1 = save antigo, sem pontos de perk ainda (migrado no login). */
    private int perkPoints;
    /** Quantas vezes cada skill ficou lendária. */
    private final Map<String, Integer> legendary = new HashMap<>();
    /** Lições de treinador usadas no level atual do personagem. */
    private int trainingsThisLevel;

    // Estado de runtime, não salvo.
    int staminaRegenDelay;
    /** Stamina zerou: sem correr nem atacar até recuperar parte dela (não é salvo). */
    boolean exhausted;
    float lastSentStamina = Float.NaN;
    float lastSentMagicka = Float.NaN;
    int lastLevelSoundTick;
    /** Stats dos perks em cache; recalculados quando os perks mudam. */
    public double[] statCache;

    public PlayerRpgData() {
        this(1, 0, Map.of(), 0, 0, 0, 0, -1f, -1f, Map.of(), 0, Map.of(), 0);
    }

    private PlayerRpgData(int level, double xp, Map<String, SkillProgress> saved, int unspentPoints,
                          int health, int staminaPts, int magickaPts, float stamina, float magicka,
                          Map<String, Integer> perks, int perkPoints, Map<String, Integer> legendary, int trainings) {
        this.level = level;
        this.xp = xp;
        for (Skill s : Skill.values()) {
            SkillProgress p = saved.get(s.id());
            // Migração 0.7 → 0.8: a antiga "armor" vira Armadura Pesada e Leve.
            if (p == null && (s == Skill.HEAVY_ARMOR || s == Skill.LIGHT_ARMOR)) p = saved.get("armor");
            skills.put(s, p != null ? p.copy() : new SkillProgress(startLevel(), 0));
        }
        this.unspentPoints = unspentPoints;
        attributePoints.put(RpgAttribute.HEALTH, health);
        attributePoints.put(RpgAttribute.STAMINA, staminaPts);
        attributePoints.put(RpgAttribute.MAGICKA, magickaPts);
        this.stamina = stamina;
        this.magicka = magicka;
        this.perks.putAll(perks);
        this.perkPoints = perkPoints;
        this.legendary.putAll(legendary);
        this.trainingsThisLevel = trainings;
    }

    private static int startLevel() {
        // A config de servidor ainda não está carregada quando o cliente cria dados padrão.
        return LWConfig.SPEC.isLoaded() ? LWConfig.SKILL_START_LEVEL.get() : 15;
    }

    public int level() { return level; }
    public double xp() { return xp; }
    public void setLevel(int level) { this.level = level; }
    public void setXp(double xp) { this.xp = xp; }

    public SkillProgress skill(Skill skill) {
        return skills.get(skill);
    }

    public int unspentPoints() { return unspentPoints; }
    public void setUnspentPoints(int points) { this.unspentPoints = points; }

    public int points(RpgAttribute attribute) {
        return attributePoints.get(attribute);
    }

    public void setPoints(RpgAttribute attribute, int points) {
        attributePoints.put(attribute, points);
    }

    public int spentPoints() {
        return attributePoints.values().stream().mapToInt(Integer::intValue).sum();
    }

    public float stamina() { return stamina; }
    public float magicka() { return magicka; }
    public void setStamina(float stamina) { this.stamina = stamina; }
    public void setMagicka(float magicka) { this.magicka = magicka; }

    // ----- perks

    public int perkRank(String perkId) {
        return perks.getOrDefault(perkId, 0);
    }

    public void setPerkRank(String perkId, int rank) {
        if (rank <= 0) perks.remove(perkId); else perks.put(perkId, rank);
        statCache = null;
    }

    public Map<String, Integer> perks() {
        return perks;
    }

    public int spentPerkPoints() {
        return perks.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int perkPoints() { return perkPoints; }
    public void setPerkPoints(int points) { this.perkPoints = points; }

    public int legendaryCount(Skill skill) {
        return legendary.getOrDefault(skill.id(), 0);
    }

    public void addLegendary(Skill skill) {
        legendary.merge(skill.id(), 1, Integer::sum);
    }

    public int totalLegendary() {
        return legendary.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int trainingsThisLevel() { return trainingsThisLevel; }
    public void setTrainingsThisLevel(int trainings) { this.trainingsThisLevel = trainings; }

    private Map<String, SkillProgress> skillsById() {
        Map<String, SkillProgress> out = new HashMap<>();
        skills.forEach((s, p) -> out.put(s.id(), p));
        return out;
    }
}
