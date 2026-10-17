package com.leveledworld.skill;

import com.leveledworld.network.ClientRpgData;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.PlayerRpgData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/** Soma os efeitos dos perks de um jogador. Funciona no servidor (dados reais) e no cliente (dados sincronizados). */
public final class PerkStats {
    private static final Stat[] STATS = Stat.values();

    public static double[] compute(Map<String, Integer> perks) {
        double[] out = new double[STATS.length];
        for (Map.Entry<String, Integer> e : perks.entrySet()) {
            PerkDef def = PerkTrees.byId(e.getKey());
            if (def == null || e.getValue() <= 0) continue;
            for (PerkDef.Effect fx : def.effects) {
                double v = fx.at(e.getValue());
                int i = fx.stat().ordinal();
                out[i] = fx.stat().agg == Stat.Agg.MAX ? Math.max(out[i], v) : out[i] + v;
            }
        }
        return out;
    }

    public static double[] of(PlayerRpgData data) {
        if (data.statCache == null) data.statCache = compute(data.perks());
        return data.statCache;
    }

    /** Valor do stat para o jogador, no lado em que o código está rodando. */
    public static double get(Player player, Stat stat) {
        if (player instanceof ServerPlayer sp) return of(LevelingService.get(sp))[stat.ordinal()];
        if (player != null && player.level().isClientSide) return ClientRpgData.stat(stat);
        return 0;
    }

    public static boolean has(Player player, Stat stat) {
        return get(player, stat) > 0;
    }

    private PerkStats() {}
}
