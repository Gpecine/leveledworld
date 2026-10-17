package com.leveledworld.skill;

import java.util.Locale;

/** As 12 skills, divididas nos grupos Guerreiro, Ladrão e Artesão (ordem do carrossel). */
public enum Skill {
    ONE_HANDED(Group.WARRIOR),   // espadas
    TWO_HANDED(Group.WARRIOR),   // machados, maça, tridente corpo a corpo
    ARCHERY(Group.WARRIOR),      // arco, besta, tridente arremessado
    BLOCK(Group.WARRIOR),        // escudo
    HEAVY_ARMOR(Group.WARRIOR),  // ferro, diamante, netherite
    LIGHT_ARMOR(Group.THIEF),    // couro, cota de malha, ouro, tartaruga
    SNEAK(Group.THIEF),          // furtividade
    ATHLETICS(Group.THIEF),      // correr, nadar, pular, escalar
    MINING(Group.CRAFTSMAN),
    WOODCUTTING(Group.CRAFTSMAN),
    SMITHING(Group.CRAFTSMAN),
    FARMING(Group.CRAFTSMAN);

    public enum Group {
        WARRIOR, THIEF, CRAFTSMAN;

        public String translationKey() {
            return "skill_group.leveledworld." + name().toLowerCase(Locale.ROOT);
        }
    }

    public final Group group;

    Skill(Group group) {
        this.group = group;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "skill.leveledworld." + id();
    }

    public static Skill byId(String id) {
        for (Skill s : values()) {
            if (s.id().equals(id)) return s;
        }
        return null;
    }
}
