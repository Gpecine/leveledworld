package com.leveledworld.event;

import com.leveledworld.LeveledWorld;
import com.leveledworld.config.LWConfig;
import com.leveledworld.item.RpgItems;
import com.leveledworld.item.SkillTomeItem;
import com.leveledworld.skill.Skill;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.List;
import java.util.Map;

/**
 * Treinadores: aldeões vendem lições das skills ligadas à profissão. O nível do aldeão limita até onde ele ensina
 * (Novato 25 … Mestre 75) e o preço sobe junto.
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class TrainerEvents {
    private static final int[] MAX_LEVEL = {25, 40, 55, 65, 75};
    private static final int[] PRICE = {6, 10, 16, 24, 32};

    private static final Map<VillagerProfession, List<Skill>> TRAINERS = Map.of(
            VillagerProfession.WEAPONSMITH, List.of(Skill.ONE_HANDED, Skill.TWO_HANDED),
            VillagerProfession.ARMORER, List.of(Skill.HEAVY_ARMOR, Skill.LIGHT_ARMOR, Skill.BLOCK),
            VillagerProfession.FLETCHER, List.of(Skill.ARCHERY),
            VillagerProfession.TOOLSMITH, List.of(Skill.MINING, Skill.WOODCUTTING, Skill.SMITHING),
            VillagerProfession.FARMER, List.of(Skill.FARMING),
            VillagerProfession.BUTCHER, List.of(Skill.FARMING),
            VillagerProfession.CARTOGRAPHER, List.of(Skill.ATHLETICS));

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!tradesEnabled()) return;
        List<Skill> skills = TRAINERS.get(event.getType());
        if (skills == null) return;
        for (int tier = 1; tier <= 5; tier++) {
            final int t = tier - 1;
            for (Skill skill : skills) {
                event.getTrades().get(tier).add(lesson(skill, MAX_LEVEL[t], PRICE[t]));
            }
        }
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        if (!tradesEnabled()) return;
        event.getRareTrades().add(lesson(Skill.SNEAK, 75, 20));
        event.getGenericTrades().add(lesson(Skill.SNEAK, 40, 10));
    }

    /** As trocas são montadas antes da config do servidor carregar; nesse caso vale o padrão (ligado). */
    private static boolean tradesEnabled() {
        return !LWConfig.SPEC.isLoaded() || LWConfig.TRAINER_TRADES.get();
    }

    private static VillagerTrades.ItemListing lesson(Skill skill, int maxLevel, int price) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, price),
                SkillTomeItem.create(RpgItems.TRAINING_LESSON.get(), skill, 1, maxLevel, true), 3, 8, 0.05f);
    }

    private TrainerEvents() {}
}
