package com.lovetropics.perms.command;

import com.google.common.collect.Lists;
import com.lovetropics.perms.LTPerms;
import com.lovetropics.perms.Role;
import com.lovetropics.perms.RoleConfiguration;
import com.lovetropics.perms.capability.PlayerRoles;
import com.lovetropics.perms.modifier.command.CommandPermEvaluator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Collection;
import java.util.function.BiPredicate;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class RoleCommand {
    public static final DynamicCommandExceptionType ROLE_NOT_FOUND = new DynamicCommandExceptionType(arg -> {
        return new TranslationTextComponent("commands." + LTPerms.ID + ".role.not_found", arg);
    });

    public static final SimpleCommandExceptionType ROLE_POWER_TOO_LOW = new SimpleCommandExceptionType(
            new TranslationTextComponent("commands." + LTPerms.ID + ".role.power_too_low")
    );

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("role")
                .requires(s -> s.hasPermissionLevel(4))
                .then(literal("assign")
                        .then(argument("targets", EntityArgument.players())
                                .then(argument("role", StringArgumentType.word()).suggests(roleSuggestions())
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                                            String roleName = StringArgumentType.getString(ctx, "role");
                                            return updateRoles(source, targets, roleName, PlayerRoles::add, "commands." + LTPerms.ID + ".role.assign_success");
                                        }))))
                .then(literal("remove")
                        .then(argument("targets", EntityArgument.players())
                                .then(argument("role", StringArgumentType.word()).suggests(roleSuggestions())
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                                            String roleName = StringArgumentType.getString(ctx, "role");
                                            return updateRoles(source, targets, roleName, PlayerRoles::remove, "commands." + LTPerms.ID + ".role.remove_success");
                                        }))))
                .then(literal("list")
                        .then(argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    ServerPlayerEntity target = EntityArgument.getPlayer(ctx, "target");
                                    return listRoles(source, target);
                                }))
                )
                .then(literal("reload")
                        .executes(ctx -> {
                            ctx.getSource().getServer().execute(() -> {
                                RoleConfiguration.setup();
                                ctx.getSource().sendFeedback(new TranslationTextComponent("commands." + LTPerms.ID + ".role.reloaded"), false);
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }

    private static int updateRoles(CommandSource source, Collection<ServerPlayerEntity> players, String roleName, BiPredicate<PlayerRoles, Role> apply, String successKey) throws CommandSyntaxException {
        Role role = getRole(roleName);
        assertHasPower(source, role);

        MutableInt count = new MutableInt();
        for (ServerPlayerEntity player : players) {
            player.getCapability(LTPerms.playerRolesCap()).ifPresent(roles -> {
                if (apply.test(roles, role)) {
                    count.increment();
                }
            });
        }

        source.sendFeedback(new TranslationTextComponent(successKey, roleName, count.intValue()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listRoles(CommandSource source, ServerPlayerEntity player) {
        player.getCapability(LTPerms.playerRolesCap()).ifPresent(cap -> {
            Collection<Role> roles = Lists.newArrayList(cap.asIterable());
            ITextComponent rolesComponent = TextComponentUtils.makeList(roles, role -> new StringTextComponent(TextFormatting.GRAY + role.getName()));
            source.sendFeedback(new TranslationTextComponent("commands." + LTPerms.ID + ".role.list", roles.size(), rolesComponent), false);
        });

        return Command.SINGLE_SUCCESS;
    }

    private static void assertHasPower(CommandSource source, Role role) throws CommandSyntaxException {
        if (CommandPermEvaluator.doesBypassPermissions(source)) return;

        int highestPower = getHighestPowerLevel(source);
        if (highestPower <= role.getLevel()) {
            throw ROLE_POWER_TOO_LOW.create();
        }
    }

    private static Role getRole(String roleName) throws CommandSyntaxException {
        Role role = RoleConfiguration.get().get(roleName);
        if (role == null) throw ROLE_NOT_FOUND.create(roleName);
        return role;
    }

    private static SuggestionProvider<CommandSource> roleSuggestions() {
        return (ctx, builder) -> ISuggestionProvider.suggest(RoleConfiguration.get().stream().map(Role::getName), builder);
    }

    private static int getHighestPowerLevel(CommandSource source) {
        Entity entity = source.getEntity();
        if (entity == null) return 0;

        return entity.getCapability(LTPerms.playerRolesCap()).map(roles -> {
            int maxLevel = 0;
            for (Role role : roles.asIterable()) {
                int level = role.getLevel();
                if (level > maxLevel) {
                    maxLevel = level;
                }
            }
            return maxLevel;
        }).orElse(0);
    }
}
