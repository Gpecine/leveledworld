package com.leveledworld.skill;

/**
 * Valores numéricos dos perks a partir do level da skill. Cada tier substitui o anterior (não somam).
 * Mantido sem dependências de lado (cliente/servidor) para ser usado nos dois.
 */
public final class PerkEffects {
    private static int tier(int skillLevel) {
        return skillLevel >= 100 ? 4 : skillLevel >= 75 ? 3 : skillLevel >= 50 ? 2 : skillLevel >= 25 ? 1 : 0;
    }

    // Combate
    public static double meleeDamageBonus(int lvl) { return tier(lvl) >= 2 ? 0.20 : tier(lvl) >= 1 ? 0.10 : 0; }
    public static double attackStaminaMultiplier(int lvl) { return tier(lvl) >= 3 ? 0.5 : 1.0; }
    public static double meleeCritChance(int lvl) { return tier(lvl) >= 4 ? 0.15 : 0; }

    // Arquearia
    public static double rangedDamageBonus(int lvl) {
        int t = tier(lvl);
        return t >= 3 ? 0.30 : t == 2 ? 0.20 : t == 1 ? 0.10 : 0;
    }
    public static double rangedCritChance(int lvl) { return tier(lvl) >= 4 ? 0.15 : 0; }

    // Bloqueio
    public static double blockStaminaRestore(int lvl) { return tier(lvl) >= 1 ? 5 : 0; }
    public static double blockReflect(int lvl) { return tier(lvl) >= 4 ? 0.50 : tier(lvl) >= 2 ? 0.25 : 0; }
    public static boolean blockKnockback(int lvl) { return tier(lvl) >= 3; }

    // Armadura: redução máxima com armadura completa (proporcional às peças).
    public static double armorReduction(int lvl) { return tier(lvl) * 0.05; }

    // Mineração
    public static double miningSpeedBonus(int lvl) { return tier(lvl) >= 2 ? 0.30 : tier(lvl) >= 1 ? 0.15 : 0; }
    public static double oreDoubleChance(int lvl) { return tier(lvl) >= 4 ? 0.20 : tier(lvl) >= 3 ? 0.10 : 0; }

    // Lenhador
    public static double woodSpeedBonus(int lvl) { return tier(lvl) >= 2 ? 0.40 : tier(lvl) >= 1 ? 0.20 : 0; }
    public static double logExtraChance(int lvl) { return tier(lvl) >= 4 ? 0.30 : tier(lvl) >= 3 ? 0.15 : 0; }

    // Ferraria
    public static double smithEnchantChance(int lvl) { return tier(lvl) * 0.10; }
    public static int smithEnchantLevel(int lvl) { return 5 + tier(lvl) * 5; }

    private PerkEffects() {}
}
