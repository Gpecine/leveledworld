package com.leveledworld.player;

import com.leveledworld.config.LWConfig;
import com.leveledworld.skill.Skill;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Dados RPG do jogador (level do personagem + skills). Salvo como attachment e copiado ao morrer. */
public final class PlayerRpgData {
    public static final Codec<PlayerRpgData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(PlayerRpgData::level),
            Codec.DOUBLE.fieldOf("xp").forGetter(PlayerRpgData::xp),
            Codec.unboundedMap(Codec.STRING, SkillProgress.CODEC).fieldOf("skills").forGetter(PlayerRpgData::skillsById)
    ).apply(i, PlayerRpgData::new));

    private int level;
    private double xp;
    private final EnumMap<Skill, SkillProgress> skills = new EnumMap<>(Skill.class);

    public PlayerRpgData() {
        this(1, 0, Map.of());
    }

    private PlayerRpgData(int level, double xp, Map<String, SkillProgress> saved) {
        this.level = level;
        this.xp = xp;
        for (Skill s : Skill.values()) {
            SkillProgress p = saved.get(s.id());
            skills.put(s, p != null ? p.copy() : new SkillProgress(startLevel(), 0));
        }
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

    private Map<String, SkillProgress> skillsById() {
        Map<String, SkillProgress> out = new HashMap<>();
        skills.forEach((s, p) -> out.put(s.id(), p));
        return out;
    }
}
