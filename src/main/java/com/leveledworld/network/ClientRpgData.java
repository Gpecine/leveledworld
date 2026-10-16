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

    public static void clear() {
        current = null;
        resources = null;
        levelUpAt = 0;
    }

    private ClientRpgData() {}
}
