package com.leveledworld.command;

import com.leveledworld.LeveledWorld;
import com.leveledworld.mob.MobScaling;
import com.leveledworld.player.LevelingService;
import com.leveledworld.player.PlayerRpgData;
import com.leveledworld.skill.Skill;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * /rpg info [jogador]
 * /rpg level set <jogador> <level>
 * /rpg skill set <jogador> <skill> <level>
 * /rpg skill addxp <jogador> <skill> <xp>
 * /rpg mob <entidade>
 */
@EventBusSubscriber(modid = LeveledWorld.MODID)
public final class RpgCommand {
    private static final DynamicCommandExceptionType UNKNOWN_SKILL =
            new DynamicCommandExceptionType(id -> Component.translatable("command.leveledworld.unknown_skill", id));

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rpg")
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(c -> {
                                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                                    LevelingService.setCharacterLevel(p, IntegerArgumentType.getInteger(c, "level"));
                                                    return info(c, p);
                                                })))))
                .then(Commands.literal("skill").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(skillArg()
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                        .executes(c -> {
                                                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                                            LevelingService.setSkillLevel(p, getSkill(c), IntegerArgumentType.getInteger(c, "level"));
                                                            return info(c, p);
                                                        })))))
                        .then(Commands.literal("addxp")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(skillArg()
                                                .then(Commands.argument("xp", DoubleArgumentType.doubleArg(0))
                                                        .executes(c -> {
                                                            ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                                            LevelingService.addSkillXp(p, getSkill(c), DoubleArgumentType.getDouble(c, "xp"));
                                                            return info(c, p);
                                                        }))))))
                .then(Commands.literal("mob").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(c -> {
                                    Entity e = EntityArgument.getEntity(c, "target");
                                    int lvl = e instanceof Mob m ? MobScaling.getLevel(m) : 0;
                                    c.getSource().sendSuccess(() -> Component.translatable("command.leveledworld.mob",
                                            e.getDisplayName(), lvl), false);
                                    return lvl;
                                }))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> skillArg() {
        return Commands.argument("skill", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Skill.values()).map(Skill::id), b));
    }

    private static Skill getSkill(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "skill");
        Skill skill = Skill.byId(id);
        if (skill == null) throw UNKNOWN_SKILL.create(id);
        return skill;
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        PlayerRpgData data = LevelingService.get(p);
        CommandSourceStack src = c.getSource();
        src.sendSuccess(() -> Component.translatable("command.leveledworld.info", p.getDisplayName(), data.level(),
                (int) data.xp(), (int) LevelingService.characterXpToNext(data.level())), false);
        for (Skill s : Skill.values()) {
            var sp = data.skill(s);
            src.sendSuccess(() -> Component.literal("  ").append(Component.translatable(s.translationKey()))
                    .append(": " + sp.level() + " (" + (int) sp.xp() + "/" + (int) LevelingService.skillXpToNext(sp.level()) + ")"), false);
        }
        return data.level();
    }

    private RpgCommand() {}
}
