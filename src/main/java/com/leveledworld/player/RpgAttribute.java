package com.leveledworld.player;

import java.util.Locale;

/** Atributos que recebem pontos ao subir de level, como no Skyrim. */
public enum RpgAttribute {
    HEALTH,   // vida máxima
    STAMINA,  // correr, pular e atacar gastam stamina
    MAGICKA;  // reserva de mana (base para o futuro sistema de magias)

    public String translationKey() {
        return "attribute.leveledworld." + name().toLowerCase(Locale.ROOT);
    }

    public static RpgAttribute byOrdinal(int i) {
        RpgAttribute[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }
}
