package com.leveledworld.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Perks desbloqueados automaticamente ao atingir certos levels de skill (estrelas das constelações). */
public enum Perk {
    ONE_HANDED_1(Skill.ONE_HANDED, 25), ONE_HANDED_2(Skill.ONE_HANDED, 50), ONE_HANDED_3(Skill.ONE_HANDED, 75), ONE_HANDED_4(Skill.ONE_HANDED, 100),
    ARCHERY_1(Skill.ARCHERY, 25), ARCHERY_2(Skill.ARCHERY, 50), ARCHERY_3(Skill.ARCHERY, 75), ARCHERY_4(Skill.ARCHERY, 100),
    BLOCK_1(Skill.BLOCK, 25), BLOCK_2(Skill.BLOCK, 50), BLOCK_3(Skill.BLOCK, 75), BLOCK_4(Skill.BLOCK, 100),
    ARMOR_1(Skill.ARMOR, 25), ARMOR_2(Skill.ARMOR, 50), ARMOR_3(Skill.ARMOR, 75), ARMOR_4(Skill.ARMOR, 100),
    MINING_1(Skill.MINING, 25), MINING_2(Skill.MINING, 50), MINING_3(Skill.MINING, 75), MINING_4(Skill.MINING, 100),
    WOODCUTTING_1(Skill.WOODCUTTING, 25), WOODCUTTING_2(Skill.WOODCUTTING, 50), WOODCUTTING_3(Skill.WOODCUTTING, 75), WOODCUTTING_4(Skill.WOODCUTTING, 100),
    SMITHING_1(Skill.SMITHING, 25), SMITHING_2(Skill.SMITHING, 50), SMITHING_3(Skill.SMITHING, 75), SMITHING_4(Skill.SMITHING, 100);

    public final Skill skill;
    public final int level;

    Perk(Skill skill, int level) {
        this.skill = skill;
        this.level = level;
    }

    public String translationKey() {
        return "perk.leveledworld." + name().toLowerCase(Locale.ROOT);
    }

    public boolean unlocked(int skillLevel) {
        return skillLevel >= level;
    }

    /** Perks da skill, em ordem de level. */
    public static List<Perk> of(Skill skill) {
        List<Perk> out = new ArrayList<>(4);
        for (Perk p : values()) if (p.skill == skill) out.add(p);
        return out;
    }
}
