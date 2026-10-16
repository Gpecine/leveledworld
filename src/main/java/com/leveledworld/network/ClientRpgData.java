package com.leveledworld.network;

/**
 * Cópia do estado RPG do jogador local, lida pelo HUD e pela tela de status.
 * Sem imports de classes de cliente para poder ser referenciada pelo handler do pacote em qualquer lado.
 */
public final class ClientRpgData {
    private static volatile SyncRpgDataPayload current;

    public static SyncRpgDataPayload get() {
        return current;
    }

    static void set(SyncRpgDataPayload data) {
        current = data;
    }

    public static void clear() {
        current = null;
    }

    private ClientRpgData() {}
}
