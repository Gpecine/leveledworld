package com.leveledworld.mob;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dificuldade e recompensas por dimensão, lidas de {@code dimensions} na config do servidor.
 * Formato de cada entrada: {@code dimensão,bônusDeLevel,levelMínimo,bônusDeLoot,multiplicadorDeRecompensa,chanceExclusiva}.
 */
public final class DimensionRules {
    public record Rule(int levelBonus, int minLevel, int lootLevelBonus, double rewardMultiplier, double exclusiveChance) {
        public boolean harder() {
            return levelBonus > 0 || minLevel > 1 || lootLevelBonus > 0 || rewardMultiplier > 1;
        }
    }

    public static final Rule DEFAULT = new Rule(0, 1, 0, 1.0, 0.0);

    private static List<? extends String> cachedSource;
    private static Map<ResourceLocation, Rule> cached = Map.of();

    public static Rule of(Level level) {
        return of(level.dimension().location());
    }

    public static synchronized Rule of(ResourceLocation dimension) {
        List<? extends String> source = LWConfig.DIMENSION_RULES.get();
        if (source != cachedSource) {
            cached = parse(source);
            cachedSource = source;
        }
        return cached.getOrDefault(dimension, DEFAULT);
    }

    /** Tabela de loot exclusiva da dimensão: data/leveledworld/loot_table/dimension/<namespace>/<caminho>.json. */
    public static ResourceKey<LootTable> exclusiveTable(ResourceLocation dimension) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                LeveledWorld.id("dimension/" + dimension.getNamespace() + "/" + dimension.getPath()));
    }

    static Map<ResourceLocation, Rule> parse(List<? extends String> entries) {
        Map<ResourceLocation, Rule> out = new HashMap<>();
        for (String entry : entries) {
            String[] p = entry.split(",");
            try {
                ResourceLocation id = ResourceLocation.parse(p[0].trim());
                out.put(id, new Rule(Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()),
                        Integer.parseInt(p[3].trim()), Double.parseDouble(p[4].trim()), Double.parseDouble(p[5].trim())));
            } catch (RuntimeException e) {
                LeveledWorld.LOGGER.warn("Regra de dimensão inválida ignorada: '{}' ({})", entry, e.getMessage());
            }
        }
        return out;
    }

    public static boolean isValid(Object o) {
        if (!(o instanceof String s)) return false;
        return parse(List.of(s)).size() == 1;
    }

    private DimensionRules() {}
}
