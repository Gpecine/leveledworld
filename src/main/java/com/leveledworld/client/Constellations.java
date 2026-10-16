package com.leveledworld.client;

import com.leveledworld.skill.Skill;

import java.util.EnumMap;
import java.util.Map;

/**
 * Formas das constelações de cada skill. Coordenadas normalizadas em [-1, 1] (y para baixo).
 * {@code perks[i]} é o índice da estrela que representa o perk de tier i (levels 25/50/75/100).
 */
public final class Constellations {
    public record Shape(float[][] stars, int[][] edges, int[] perks) {}

    private static final Map<Skill, Shape> SHAPES = new EnumMap<>(Skill.class);

    static {
        // Combate: espada na vertical
        SHAPES.put(Skill.ONE_HANDED, new Shape(
                new float[][]{{0, -1}, {0, -0.45f}, {0, 0.1f}, {-0.45f, 0.25f}, {0, 0.25f}, {0.45f, 0.25f}, {0, 0.6f}, {0, 0.9f}},
                new int[][]{{0, 1}, {1, 2}, {2, 4}, {3, 4}, {4, 5}, {4, 6}, {6, 7}},
                new int[]{7, 4, 1, 0}));
        // Arquearia: arco com flecha
        SHAPES.put(Skill.ARCHERY, new Shape(
                new float[][]{{0.15f, -0.95f}, {-0.3f, -0.6f}, {-0.45f, 0}, {-0.3f, 0.6f}, {0.15f, 0.95f},
                        {-0.8f, 0}, {0.9f, 0}, {-0.95f, -0.18f}, {-0.95f, 0.18f}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {0, 4}, {5, 2}, {2, 6}, {5, 7}, {5, 8}},
                new int[]{4, 2, 0, 6}));
        // Bloqueio: escudo
        SHAPES.put(Skill.BLOCK, new Shape(
                new float[][]{{-0.6f, -0.7f}, {0, -0.85f}, {0.6f, -0.7f}, {0.65f, 0}, {0.35f, 0.6f}, {0, 0.95f},
                        {-0.35f, 0.6f}, {-0.65f, 0}, {0, 0}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0}, {1, 8}, {8, 5}, {7, 8}, {8, 3}},
                new int[]{5, 7, 3, 1}));
        // Armadura: elmo com viseira
        SHAPES.put(Skill.ARMOR, new Shape(
                new float[][]{{-0.6f, 0.8f}, {-0.65f, 0}, {-0.45f, -0.6f}, {0, -0.85f}, {0.45f, -0.6f}, {0.65f, 0},
                        {0.6f, 0.8f}, {0, 0}, {0, 0.5f}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {1, 7}, {7, 5}, {7, 8}},
                new int[]{0, 6, 7, 3}));
        // Mineração: picareta
        SHAPES.put(Skill.MINING, new Shape(
                new float[][]{{-0.9f, -0.3f}, {-0.45f, -0.72f}, {0, -0.85f}, {0.45f, -0.72f}, {0.9f, -0.3f},
                        {0, 0}, {0, 0.5f}, {0, 0.95f}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {2, 5}, {5, 6}, {6, 7}},
                new int[]{7, 5, 0, 4}));
        // Lenhador: machado
        SHAPES.put(Skill.WOODCUTTING, new Shape(
                new float[][]{{0.2f, -0.95f}, {0.2f, -0.2f}, {0.2f, 0.35f}, {0.2f, 0.95f},
                        {-0.4f, -0.95f}, {-0.7f, -0.45f}, {-0.4f, 0.05f}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {0, 4}, {4, 5}, {5, 6}, {6, 1}},
                new int[]{3, 2, 5, 0}));
        // Ferraria: bigorna com martelo e faísca
        SHAPES.put(Skill.SMITHING, new Shape(
                new float[][]{{-0.95f, -0.15f}, {-0.55f, -0.05f}, {0.8f, -0.05f}, {0.35f, 0.3f}, {-0.25f, 0.3f},
                        {-0.55f, 0.8f}, {0.65f, 0.8f}, {0.05f, -0.95f}, {0.55f, -0.55f}, {0.3f, -0.45f}},
                new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 6}, {6, 5}, {5, 4}, {4, 1}, {7, 8}, {8, 9}},
                new int[]{5, 6, 0, 7}));
    }

    public static Shape of(Skill skill) {
        return SHAPES.get(skill);
    }

    private Constellations() {}
}
