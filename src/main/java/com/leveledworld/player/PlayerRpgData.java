package com.leveledworld.player;

import com.leveledworld.config.LWConfig;
import com.leveledworld.skill.Skill;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Dados RPG do jogador (level, skills, atributos e recursos). Salvo como attachment e copiado ao morrer. */
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
            Codec.FLOAT.optionalFieldOf("magicka", -1f).forGetter(PlayerRpgData::magicka)
    ).apply(i, PlayerRpgData::new));

    private int level;
    private double xp;
    private final EnumMap<Skill, SkillProgress> skills = new EnumMap<>(Skill.class);
    private int unspentPoints;
    private final EnumMap<RpgAttribute, Integer> attributePoints = new EnumMap<>(RpgAttribute.class);
    /** Valores atuais dos recursos; -1 = ainda não inicializado (enche no primeiro tick). */
    private float stamina;
    private float magicka;

    // Estado de runtime, não salvo.
    int staminaRegenDelay;
    float lastSentStamina = Float.NaN;
    float lastSentMagicka = Float.NaN;

    public PlayerRpgData() {
        this(1, 0, Map.of(), 0, 0, 0, 0, -1f, -1f);
    }

    private PlayerRpgData(int level, double xp, Map<String, SkillProgress> saved, int unspentPoints,
                          int health, int staminaPts, int magickaPts, float stamina, float magicka) {
        this.level = level;
        this.xp = xp;
        for (Skill s : Skill.values()) {
            SkillProgress p = saved.get(s.id());
            skills.put(s, p != null ? p.copy() : new SkillProgress(startLevel(), 0));
        }
        this.unspentPoints = unspentPoints;
        attributePoints.put(RpgAttribute.HEALTH, health);
        attributePoints.put(RpgAttribute.STAMINA, staminaPts);
        attributePoints.put(RpgAttribute.MAGICKA, magickaPts);
        this.stamina = stamina;
        this.magicka = magicka;
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

    private Map<String, SkillProgress> skillsById() {
        Map<String, SkillProgress> out = new HashMap<>();
        skills.forEach((s, p) -> out.put(s.id(), p));
        return out;
    }
}
