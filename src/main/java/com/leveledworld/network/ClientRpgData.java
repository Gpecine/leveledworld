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

    static void set(SyncRpgDataPayload data) {
        current = data;
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
    }

    private ClientRpgData() {}
}
