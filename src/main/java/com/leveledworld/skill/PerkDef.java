package com.leveledworld.skill;

import java.util.List;

/**
 * Um perk de uma árvore. {@code levels[i]} é o nível de skill exigido para o rank i+1 (perks sem ranks têm 1
 * entrada). {@code requires} lista perks da mesma árvore dos quais basta ter UM (vazio = raiz).
 * {@code x}/{@code y} são a posição da estrela na constelação, em [-1, 1] (y para baixo), calculada pelo layout.
 */
public final class PerkDef {
    public record Effect(Stat stat, double[] values) {
        /** Valor para o rank (1..n); se houver um único valor, vale para todos os ranks. */
        public double at(int rank) {
            return values.length == 1 ? values[0] : values[Math.min(rank, values.length) - 1];
        }
    }

    public final Skill skill;
    public final String localId;
    public final String id;
    public final int[] levels;
    public final List<String> requires;
    public final List<Effect> effects;
    public final int index;
    float x;
    float y;

    PerkDef(Skill skill, String localId, int[] levels, List<String> requires, List<Effect> effects, int index) {
        this.skill = skill;
        this.localId = localId;
        this.id = skill.id() + "." + localId;
        this.levels = levels;
        this.requires = requires;
        this.effects = effects;
        this.index = index;
    }

    public int maxRank() {
        return levels.length;
    }

    /** Nível de skill necessário para comprar o próximo rank (a partir do rank atual). */
    public int levelForNextRank(int currentRank) {
        return levels[Math.min(currentRank, levels.length - 1)];
    }

    public float x() { return x; }
    public float y() { return y; }

    public String translationKey() {
        return "perk.leveledworld." + id;
    }
}
