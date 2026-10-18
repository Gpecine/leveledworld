package com.leveledworld.mob;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Tiers de raridade dos mobs, do mais comum ao mais raro. Cada tier define a chance base de aparecer (peso), quanto o
 * mob fica mais forte e quanto rende: drops, XP, produtos dos animais, preço com aldeões e bônus de comida, de
 * material fabricado e de cavalos/pets.
 */
public enum MobTier {
    //          cor        peso  força  drops  xp   produtos venda comida material montaria pet
    COMMON(0xFFD8D8D8, 60.0, 0.00, 1.00, 1.0, 1.00, 1.00, 0.00, 0.00, 0.00, 0.00),
    UNCOMMON(0xFF5FE35F, 25.0, 0.05, 1.25, 1.25, 1.25, 1.25, 0.15, 0.10, 0.04, 0.10),
    RARE(0xFF4FA8FF, 10.0, 0.10, 1.50, 1.5, 1.50, 1.50, 0.30, 0.20, 0.08, 0.20),
    EPIC(0xFFC266FF, 4.0, 0.15, 2.00, 2.0, 2.00, 2.00, 0.50, 0.30, 0.12, 0.35),
    LEGENDARY(0xFFFFA62B, 0.9, 0.25, 2.50, 3.0, 2.50, 3.00, 0.75, 0.40, 0.18, 0.50),
    EXOTIC(0xFFFF4FD8, 0.1, 0.40, 3.00, 5.0, 3.00, 5.00, 1.00, 0.50, 0.25, 0.75);

    /** Cor do nome e da placa (ARGB). O Exótico também tem um brilho animado no cliente. */
    public final int color;
    /** Peso base no sorteio (as porcentagens somam 100). */
    public final double weight;
    /** Mobs agressivos: vida e dano extras (0.10 = +10%). */
    public final double strength;
    /** Multiplicador dos drops empilháveis (agressivos e passivos). */
    public final double drops;
    /** Multiplicador do XP de orbe ao morrer. */
    public final double xp;
    /** Multiplicador de produtos dos animais (lã na tosquia, ovos). */
    public final double products;
    /** Multiplicador de esmeraldas ao vender um produto deste tier a aldeões. */
    public final double sale;
    /** Comida deste tier: nutrição extra (0.30 = +30%). */
    public final double food;
    /** Equipamento fabricado com material deste tier: armadura e durabilidade extras (0.20 = +20%). */
    public final double material;
    /** Cavalos, burros, lhamas e camelos: velocidade e pulo extras. */
    public final double mount;
    /** Vida extra de animais (e dano extra de lobos). */
    public final double pet;

    MobTier(int color, double weight, double strength, double drops, double xp, double products, double sale,
            double food, double material, double mount, double pet) {
        this.color = color;
        this.weight = weight;
        this.strength = strength;
        this.drops = drops;
        this.xp = xp;
        this.products = products;
        this.sale = sale;
        this.food = food;
        this.material = material;
        this.mount = mount;
        this.pet = pet;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String translationKey() {
        return "tier.leveledworld." + id();
    }

    public Component displayName() {
        return Component.translatable(translationKey()).withStyle(style());
    }

    public Style style() {
        return Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF));
    }

    public ChatFormatting chatColor() {
        return switch (this) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.DARK_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
            case EXOTIC -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    public MobTier up() {
        return this == EXOTIC ? EXOTIC : values()[ordinal() + 1];
    }

    public MobTier down() {
        return this == COMMON ? COMMON : values()[ordinal() - 1];
    }

    public static MobTier byIndex(int index) {
        MobTier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, index))];
    }

    public static MobTier byId(String id) {
        for (MobTier t : values()) if (t.id().equals(id)) return t;
        return null;
    }
}
