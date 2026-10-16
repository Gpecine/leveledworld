package com.leveledworld.skill;

import java.util.Locale;

/** Skills que evoluem com o uso, no estilo Skyrim. */
public enum Skill {
    ONE_HANDED,   // ataques corpo-a-corpo
    ARCHERY,      // projéteis (arco, besta, tridente)
    BLOCK,        // bloquear com escudo
    ARMOR,        // receber dano usando armadura
    MINING,       // minerar pedra e minérios
    WOODCUTTING,  // cortar troncos
    SMITHING;     // fabricar armas, ferramentas e armaduras

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
