package com.lovetropics.perms.command;

import com.lovetropics.perms.Role;
import com.lovetropics.perms.RoleConfiguration;
import com.lovetropics.perms.override.command.CommandPermEvaluator;
import com.lovetropics.perms.storage.PlayerRoleStorage;
import com.lovetropics.perms.storage.PlayerRoles;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class RoleCommand {
    public static final DynamicCommandExceptionType ROLE_NOT_FOUND = new DynamicCommandExceptionType(arg -> {
        return new TranslationTextComponent("Role with name '%s' was not found!", arg);
    });

    public static final SimpleCommandExceptionType ROLE_POWER_TOO_LOW = new SimpleCommandExceptionType(
            new StringTextComponent("You do not have sufficient power to manage this role")
    );

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("role")
                .requires(s -> s.hasPermissionLevel(4))
                .then(literal("assign")
                        .then(argument("targets", GameProfileArgument.gameProfile())
                                .then(argument("role", StringArgumentType.word()).suggests(roleSuggestions())
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "targets");
                                            String roleName = StringArgumentType.getString(ctx, "role");
                                            return updateRoles(source, targets, roleName, PlayerRoles::add, "'%s' assigned to %s players");
                                        }))))
                .then(literal("remove")
                        .then(argument("targets", GameProfileArgument.gameProfile())
                                .then(argument("role", StringArgumentType.word()).suggests(roleSuggestions())
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "targets");
                                            String roleName = StringArgumentType.getString(ctx, "role");
                                            return updateRoles(source, targets, roleName, PlayerRoles::remove, "'%s' removed from %s players");
                                        }))))
                .then(literal("list")
                        .then(argument("target", GameProfileArgument.gameProfile()).executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "target");
                            return listRoles(source, targets);
                        }))
                )
                .then(literal("reload").executes(ctx -> reloadRoles(ctx.getSource())))
        );
    }

    private static int updateRoles(CommandSource source, Collection<GameProfile> players, String roleName, BiPredicate<PlayerRoles, Role> apply, String success) throws CommandSyntaxException {
        Role role = getRole(roleName);
        assertHasPower(source, role);

        PlayerRoleStorage storage = PlayerRoleStorage.forServer(source.getServer());

        MutableInt count = new MutableInt();
        for (GameProfile player : players) {
            PlayerRoles roles = storage.getOrCreate(player.getId());
            if (apply.test(roles, role)) {
                count.increment();
            }
        }

        source.sendFeedback(new TranslationTextComponent(success, roleName, count.intValue()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listRoles(CommandSource source, Collection<GameProfile> players) {
        PlayerRoleStorage storage = PlayerRoleStorage.forServer(source.getServer());

        Set<Role> roles = new ObjectOpenHashSet<>();

        for (GameProfile player : players) {
            PlayerRoles playerRoles = storage.getOrCreate(player.getId());
            if (playerRoles != null) {
                playerRoles.roles().forEach(roles::add);
            }
        }

        ITextComponent rolesComponent = TextComponentUtils.makeList(roles, role -> new StringTextComponent(TextFormatting.GRAY + role.getName()));
        source.sendFeedback(new TranslationTextComponent("Found %s roles on players: %s", roles.size(), rolesComponent), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int reloadRoles(CommandSource source) {
        source.getServer().execute(() -> {
            RoleConfiguration.setup();

            PlayerRoleStorage storage = PlayerRoleStorage.forServer(source.getServer());

            List<ServerPlayerEntity> players = source.getServer().getPlayerList().getPlayers();
            for (ServerPlayerEntity player : players) {
                PlayerRoles roles = storage.getOrCreate(player);
                if (roles != null) {
                    roles.notifyReload();
                }
            }

            source.sendFeedback(new TranslationTextComponent("Role configuration successfully reloaded"), false);
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
        return (ctx, builder) -> {
            int highestPowerLevel = getHighestPowerLevel(ctx.getSource());
            return ISuggestionProvider.suggest(
                    RoleConfiguration.get().stream()
                            .filter(role -> role.getLevel() < highestPowerLevel)
                            .map(Role::getName),
                    builder
            );
        };
    }

    private static int getHighestPowerLevel(CommandSource source) {
        Entity entity = source.getEntity();
        if (entity == null || CommandPermEvaluator.doesBypassPermissions(source)) return Integer.MAX_VALUE;

        PlayerRoles roles = PlayerRoleStorage.forServer(source.getServer()).getOrCreate(entity);
        if (roles != null) {
            IntStream levels = roles.roles().mapToInt(Role::getLevel);
            return levels.max().orElse(0);
        } else {
            return 0;
        }
    }
}
