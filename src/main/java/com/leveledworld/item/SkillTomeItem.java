package com.leveledworld.item;

import com.leveledworld.config.LWConfig;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.PlayerRpgData;
import com.leveledworld.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Tomo de skill / lição de treinador: clique direito para ler e ganhar níveis na skill. */
public class SkillTomeItem extends Item {
    private final boolean lessonItem;

    public SkillTomeItem(Properties properties, boolean lessonItem) {
        super(properties);
        this.lessonItem = lessonItem;
    }

    public static ItemStack create(Item item, Skill skill, int levels, int maxLevel, boolean lesson) {
        ItemStack stack = new ItemStack(item);
        stack.set(RpgItems.TOME.get(), new TomeData(skill.id(), levels, maxLevel, lesson));
        return stack;
    }

    /** Tomos do loot vêm sem skill: sorteia na primeira vez que entram no inventário. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || lessonItem) return;
        TomeData data = stack.get(RpgItems.TOME.get());
        if (data == null || data.skill().isEmpty()) {
            Skill[] skills = Skill.values();
            Skill skill = skills[level.random.nextInt(skills.length)];
            int levels = level.random.nextFloat() < 0.15f ? 2 : 1;  // Tomo Antigo
            stack.set(RpgItems.TOME.get(), new TomeData(skill.id(), levels, 0, false));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TomeData data = stack.get(RpgItems.TOME.get());
        Skill skill = data == null ? null : Skill.byId(data.skill());
        if (skill == null) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);

        PlayerRpgData rpg = LevelingService.get(sp);
        int current = rpg.skill(skill).level();
        if (current >= LWConfig.SKILL_MAX_LEVEL.get()) {
            sp.displayClientMessage(Component.translatable("message.leveledworld.tome_max").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (data.lesson()) {
            if (data.maxLevel() > 0 && current >= data.maxLevel()) {
                sp.displayClientMessage(Component.translatable("message.leveledworld.lesson_too_high", data.maxLevel())
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            int limit = LWConfig.TRAININGS_PER_LEVEL.get();
            if (rpg.trainingsThisLevel() >= limit) {
                sp.displayClientMessage(Component.translatable("message.leveledworld.lesson_limit", limit)
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            rpg.setTrainingsThisLevel(rpg.trainingsThisLevel() + 1);
        }
        LevelingService.grantSkillLevels(sp, skill, data.levels());
        level.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        level.playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        TomeData data = stack.get(RpgItems.TOME.get());
        Skill skill = data == null ? null : Skill.byId(data.skill());
        if (skill == null) return Component.translatable("item.leveledworld.skill_tome.unknown");
        String key = data.lesson() ? "item.leveledworld.training_lesson.named"
                : data.levels() > 1 ? "item.leveledworld.skill_tome.ancient" : "item.leveledworld.skill_tome.named";
        return Component.translatable(key, Component.translatable(skill.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TomeData data = stack.get(RpgItems.TOME.get());
        if (data == null || data.skill().isEmpty()) {
            tooltip.add(Component.translatable("item.leveledworld.skill_tome.unknown.desc").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.leveledworld.skill_tome.desc", data.levels()).withStyle(ChatFormatting.GRAY));
        if (data.lesson()) {
            if (data.maxLevel() > 0) {
                tooltip.add(Component.translatable("item.leveledworld.training_lesson.max", data.maxLevel()).withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatable("item.leveledworld.training_lesson.limit", LWConfig.SPEC.isLoaded()
                    ? LWConfig.TRAININGS_PER_LEVEL.get() : 5).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        TomeData data = stack.get(RpgItems.TOME.get());
        return data != null && data.levels() > 1;
    }
}
