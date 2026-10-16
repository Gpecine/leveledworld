package com.leveledworld.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuração do cliente: config/leveledworld-client.toml. */
public final class LWClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_NAMEPLATES;
    public static final ModConfigSpec.IntValue NAMEPLATE_DISTANCE;
    public static final ModConfigSpec.BooleanValue SHOW_HEALTH_BAR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("nameplates");
        SHOW_NAMEPLATES = b.comment("Mostrar nome e level acima dos mobs.")
                .define("enabled", true);
        NAMEPLATE_DISTANCE = b.comment("Distância máxima (em blocos) para exibir a placa.")
                .defineInRange("maxDistance", 24, 4, 128);
        SHOW_HEALTH_BAR = b.comment("Mostrar barra de vida abaixo do nome.")
                .define("healthBar", true);
        b.pop();
        SPEC = b.build();
    }

    private LWClientConfig() {}
}
