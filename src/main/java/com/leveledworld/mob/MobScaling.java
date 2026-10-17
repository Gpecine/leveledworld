package com.leveledworld.mob;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.RpgAttachments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

/** Define o level de um mob a partir do jogador mais próximo e aplica os bônus de atributo. */
public final class MobScaling {
    private static final ResourceLocation HEALTH_ID = LeveledWorld.id("level_health");
    private static final ResourceLocation DAMAGE_ID = LeveledWorld.id("level_damage");
    private static final ResourceLocation ARMOR_ID = LeveledWorld.id("level_armor");

    public static int rollLevel(ServerLevel level, Mob mob) {
        int base = referencePlayerLevel(level, mob);
        double min = LWConfig.MOB_LEVEL_MIN_FACTOR.get();
        double max = Math.max(min, LWConfig.MOB_LEVEL_MAX_FACTOR.get());
        double factor = min + mob.getRandom().nextDouble() * (max - min);
        DimensionRules.Rule rule = DimensionRules.of(level);
        int mobLevel = (int) Math.round(base * factor) + rule.levelBonus();
        mobLevel = Math.max(mobLevel, rule.minLevel());
        if (isBoss(mob)) mobLevel = Math.max(mobLevel, LWConfig.BOSS_MIN_LEVEL.get());
        return Mth.clamp(mobLevel, 1, LWConfig.MOB_MAX_LEVEL.get());
    }

    /** Level do jogador mais próximo; sem ninguém por perto, usa a média dos jogadores da dimensão. */
    private static int referencePlayerLevel(ServerLevel level, Mob mob) {
        Player nearest = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(),
                LWConfig.PLAYER_SEARCH_RADIUS.get(), p -> !p.isSpectator());
        if (nearest instanceof ServerPlayer sp) {
            return LevelingService.get(sp).level();
        }
        int sum = 0;
        int count = 0;
        for (ServerPlayer p : level.players()) {
            sum += LevelingService.get(p).level();
            count++;
        }
        return count == 0 ? 1 : Math.round((float) sum / count);
    }

    public static void apply(Mob mob, int mobLevel) {
        mob.setData(RpgAttachments.MOB_LEVEL, mobLevel);
        int bonusLevels = mobLevel - 1;
        if (bonusLevels > 0) {
            addModifier(mob, Attributes.MAX_HEALTH, HEALTH_ID,
                    bonusLevels * LWConfig.HEALTH_PER_LEVEL.get(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            addModifier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID,
                    bonusLevels * LWConfig.DAMAGE_PER_LEVEL.get(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            addModifier(mob, Attributes.ARMOR, ARMOR_ID,
                    bonusLevels * LWConfig.ARMOR_PER_LEVEL.get(), AttributeModifier.Operation.ADD_VALUE);
        }
        mob.setHealth(mob.getMaxHealth());
    }

    public static int getLevel(Mob mob) {
        return mob.hasData(RpgAttachments.MOB_LEVEL) ? mob.getData(RpgAttachments.MOB_LEVEL) : 0;
    }

    private static void addModifier(Mob mob, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || amount == 0) return;
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static boolean isBoss(Mob mob) {
        return mob instanceof WitherBoss || mob instanceof EnderDragon
                || mob instanceof Warden || mob instanceof ElderGuardian;
    }

    private MobScaling() {}
}
