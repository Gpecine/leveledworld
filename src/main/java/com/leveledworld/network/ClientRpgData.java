package com.leveledworld.network;

/**
 * Cópia do estado RPG do jogador local, lida pelo HUD e pela tela de status.
 * Sem imports de classes de cliente para poder ser referenciada pelo handler do pacote em qualquer lado.
 */
public final class ClientRpgData {
    private static volatile SyncRpgDataPayload current;
    private static volatile ResourcesPayload resources;

    public static SyncRpgDataPayload get() {
        return current;
    }

    private static volatile long levelUpAt;
    private static volatile int levelUpLevel;

    static void set(SyncRpgDataPayload data) {
        SyncRpgDataPayload previous = current;
        if (previous != null && data.level() > previous.level()) {
            levelUpAt = System.currentTimeMillis();
            levelUpLevel = data.level();
        }
        current = data;
        stats = com.leveledworld.skill.PerkStats.compute(data.perks());
    }

    private static volatile double[] stats = new double[com.leveledworld.skill.Stat.values().length];

    /** Stat dos perks do jogador local (para efeitos que o cliente precisa prever). */
    public static double stat(com.leveledworld.skill.Stat stat) {
        return stats[stat.ordinal()];
    }

    /** Momento (ms) do último level up visto pelo cliente; 0 se nenhum. */
    public static long levelUpAt() {
        return levelUpAt;
    }

    public static int levelUpLevel() {
        return levelUpLevel;
    }

    public static ResourcesPayload resources() {
        return resources;
    }

    static void setResources(ResourcesPayload data) {
        resources = data;
    }

    private static volatile int stealth = -1;

    /** Estado de furtividade: -1 = não agachado, 0 = oculto, 1 = alerta, 2 = detectado. */
    public static int stealth() {
        return stealth;
    }

    static void setStealth(int state) {
        stealth = state;
    }

    public static void clear() {
        current = null;
        resources = null;
        stealth = -1;
        levelUpAt = 0;
        stats = new double[com.leveledworld.skill.Stat.values().length];
    }

    private ClientRpgData() {}
}
