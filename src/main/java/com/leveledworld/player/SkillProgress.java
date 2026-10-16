package com.leveledworld.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Level e XP acumulado de uma skill. Mutável: alterado direto no attachment do jogador. */
public final class SkillProgress {
    public static final Codec<SkillProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(SkillProgress::level),
            Codec.DOUBLE.fieldOf("xp").forGetter(SkillProgress::xp)
    ).apply(i, SkillProgress::new));

    private int level;
    private double xp;

    public SkillProgress(int level, double xp) {
        this.level = level;
        this.xp = xp;
    }

    public int level() { return level; }
    public double xp() { return xp; }
    public void setLevel(int level) { this.level = level; }
    public void setXp(double xp) { this.xp = xp; }

    public SkillProgress copy() {
        return new SkillProgress(level, xp);
    }
}
