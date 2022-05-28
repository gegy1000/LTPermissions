package com.lovetropics.perms.protection.command;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.ProtectionManager;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.UserAuthority;
import com.lovetropics.perms.protection.authority.shape.AuthorityShape;
import com.lovetropics.perms.protection.authority.shape.WorldEditShapes;
import com.lovetropics.perms.protection.command.argument.AuthorityArgument;
import com.lovetropics.perms.protection.command.argument.AuthorityBehaviorArgument;
import com.lovetropics.perms.protection.command.argument.AuthorityShapeArgument;
import com.lovetropics.perms.protection.command.argument.PermissionResultArgument;
import com.lovetropics.perms.protection.command.argument.ProtectionRuleArgument;
import com.lovetropics.perms.protection.command.argument.RoleArgument;
import com.lovetropics.perms.role.Role;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.forge.ForgePlayer;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.session.SessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.UnaryOperator;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ProtectCommand {
    private static final DynamicCommandExceptionType AUTHORITY_ALREADY_EXISTS = new DynamicCommandExceptionType(key -> {
        return new LiteralMessage("Authority with the key '" + key + "' already exists!");
    });

    private static final SimpleCommandExceptionType INCOMPLETE_SELECTION = new SimpleCommandExceptionType(new LiteralMessage(
            "Your current selection is incomplete!"
    ));

    private static final SimpleCommandExceptionType UNREPRESENTABLE_REGION = new SimpleCommandExceptionType(new LiteralMessage(
            "This kind of region cannot be handled by the world protector!"
    ));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // @formatter:off
        dispatcher.register(
            literal("protect")
                .requires(source -> source.hasPermission(3))
                .then(literal("add")
                    .then(argument("authority", StringArgumentType.string())
                    .then(argument("level", IntegerArgumentType.integer())
                    .executes(ProtectCommand::addAuthority)
                )))
                .then(literal("shape")
                    .then(literal("add")
                        .then(AuthorityArgument.argumentUser("authority")
                        .then(argument("shape", StringArgumentType.string())
                        .executes(ProtectCommand::setShape)
                    )))
                    .then(literal("set")
                        .then(AuthorityArgument.argumentUser("authority")
                        .then(AuthorityShapeArgument.argument("authority", "shape")
                        .executes(ProtectCommand::setShape)
                    )))
                    .then(literal("remove")
                        .then(AuthorityArgument.argumentUser("authority")
                        .then(AuthorityShapeArgument.argument("authority", "shape")
                        .executes(ProtectCommand::removeShape)
                    )))
                    .then(literal("select")
                        .then(AuthorityArgument.argumentUser("authority")
                        .then(AuthorityShapeArgument.argument("authority", "shape")
                        .executes(ProtectCommand::selectShape)
                    )))
                )
                .then(literal("remove")
                    .then(AuthorityArgument.argumentUser("authority")
                    .executes(ProtectCommand::removeAuthority)
                ))
                .then(literal("rule")
                    .then(literal("set")
                        .then(AuthorityArgument.argumentAll("authority")
                        .then(ProtectionRuleArgument.argument("rule")
                        .then(PermissionResultArgument.argument("result")
                        .executes(ProtectCommand::setRule)
                    ))))
                )
                .then(literal("exclusion")
                    .then(literal("add")
                        .then(literal("players")
                            .then(AuthorityArgument.argumentAll("authority")
                            .then(argument("players", GameProfileArgument.gameProfile())
                            .executes(ProtectCommand::addPlayerExclusion)
                        )))
                        .then(literal("role")
                            .then(AuthorityArgument.argumentAll("authority")
                            .then(RoleArgument.argument("role")
                            .executes(ProtectCommand::addRoleExclusion)
                        )))
                        .then(literal("operators")
                            .then(AuthorityArgument.argumentAll("authority")
                            .executes(context -> ProtectCommand.updateOperatorExclusion(context, true))
                        ))
                    )
                    .then(literal("remove")
                        .then(literal("players")
                            .then(AuthorityArgument.argumentAll("authority")
                            .then(argument("players", GameProfileArgument.gameProfile())
                            .executes(ProtectCommand::removePlayerExclusion)
                        )))
                        .then(literal("role")
                            .then(AuthorityArgument.argumentAll("authority")
                            .then(RoleArgument.argument("role")
                            .executes(ProtectCommand::removeRoleExclusion)
                        )))
                        .then(literal("operators")
                            .then(AuthorityArgument.argumentAll("authority")
                            .executes(context -> ProtectCommand.updateOperatorExclusion(context, false))
                        ))
                    )
                )
                .then(literal("behavior")
                    .then(literal("add")
                        .then(AuthorityArgument.argumentAll("authority")
                        .then(AuthorityBehaviorArgument.argument("behavior")
                        .executes(ProtectCommand::addBehavior)
                    )))
                    .then(literal("remove")
                        .then(AuthorityArgument.argumentAll("authority")
                        .then(AuthorityBehaviorArgument.argument("behavior")
                        .executes(ProtectCommand::removeBehavior)
                    )))
                )
        );
        // @formatter:on
    }

    private static int addAuthority(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "authority");
        int level = IntegerArgumentType.getInteger(context, "level");

        ProtectionManager protection = protection(context);

        UserAuthority authority = UserAuthority.create(key).withLevel(level);
        if (protection.addAuthority(authority)) {
            Component message = new TextComponent("Added authority '")
                    .append(new TextComponent(key).withStyle(ChatFormatting.AQUA))
                    .append("' ")
                    .append(new TextComponent("@" + level).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.GREEN);
            context.getSource().sendSuccess(message, true);
        } else {
            throw AUTHORITY_ALREADY_EXISTS.create(key);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int removeAuthority(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UserAuthority authority = AuthorityArgument.getUser(context, "authority");

        ProtectionManager protection = protection(context);
        if (protection.removeAuthority(authority)) {
            Component message = new TextComponent("Removed authority '")
                    .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                    .append("'")
                    .withStyle(ChatFormatting.GOLD);
            context.getSource().sendSuccess(message, true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setShape(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UserAuthority authority = AuthorityArgument.getUser(context, "authority");
        String shape = StringArgumentType.getString(context, "shape");

        AuthorityShape selection = getSelectionFor(context.getSource());

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.addShape(shape, selection));

        Component message = new TextComponent("Set shape on '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("' with key '")
                .append(new TextComponent(shape).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GREEN);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int removeShape(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Pair<UserAuthority, String> shape = AuthorityShapeArgument.get(context, "authority", "shape");
        UserAuthority authority = shape.getFirst();
        String shapeKey = shape.getSecond();

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.removeShape(shapeKey));

        Component message = new TextComponent("Removed shape from '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("' with key '")
                .append(new TextComponent(shapeKey).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GOLD);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int selectShape(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Pair<UserAuthority, String> shape = AuthorityShapeArgument.get(context, "authority", "shape");
        UserAuthority authority = shape.getFirst();
        String shapeKey = shape.getSecond();

        applySelectionFor(context.getSource(), authority.shape().get(shapeKey));

        Component message = new TextComponent("Selected shape from '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("' with key '")
                .append(new TextComponent(shapeKey).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GREEN);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int setRule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Authority authority = AuthorityArgument.getAll(context, "authority");
        ProtectionRule rule = ProtectionRuleArgument.get(context, "rule");
        PermissionResult result = PermissionResultArgument.get(context, "result");

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.withRule(rule, result));

        Component message = new TextComponent("Set rule ")
                .append(new TextComponent(rule.key() + "=").append(result.getName()).withStyle(ChatFormatting.AQUA))
                .append(" for '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GREEN);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayerExclusion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
        return modifyExclusions(context, authority -> {
            for (GameProfile player : players) {
                authority = authority.addExclusion(player);
            }
            return authority;
        });
    }

    private static int addRoleExclusion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Role role = RoleArgument.get(context, "role");
        return modifyExclusions(context, authority -> authority.addExclusion(role));
    }

    private static int removePlayerExclusion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(context, "players");
        return modifyExclusions(context, authority -> {
            for (GameProfile player : players) {
                authority = authority.removeExclusion(player);
            }
            return authority;
        });
    }

    private static int removeRoleExclusion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Role role = RoleArgument.get(context, "role");
        return modifyExclusions(context, authority -> authority.removeExclusion(role));
    }

    private static int updateOperatorExclusion(CommandContext<CommandSourceStack> context, boolean operators) throws CommandSyntaxException {
        return modifyExclusions(context, authority -> authority.excludeOperators(operators));
    }

    private static int modifyExclusions(CommandContext<CommandSourceStack> context, UnaryOperator<Authority> operator) throws CommandSyntaxException {
        Authority authority = AuthorityArgument.getAll(context, "authority");
        Authority newAuthority = operator.apply(authority);

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, newAuthority);

        Component message = new TextComponent("Updated exclusions for '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GREEN);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static void applySelectionFor(CommandSourceStack source, AuthorityShape shape) throws CommandSyntaxException {
        SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
        ForgePlayer player = ForgeAdapter.adaptPlayer(source.getPlayerOrException());
        LocalSession session = sessionManager.get(player);

        RegionSelector selector = WorldEditShapes.tryIntoRegionSelector(source.getServer(), shape);
        if (selector == null) {
            throw UNREPRESENTABLE_REGION.create();
        }

        session.setRegionSelector(selector.getWorld(), selector);
        session.dispatchCUISelection(player);
    }

    private static AuthorityShape getSelectionFor(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
        LocalSession session = sessionManager.get(ForgeAdapter.adaptPlayer(player));

        try {
            Region selection = session.getSelection();
            AuthorityShape shape = WorldEditShapes.tryFromRegion(selection);
            if (shape != null) {
                return shape;
            } else {
                throw UNREPRESENTABLE_REGION.create();
            }
        } catch (IncompleteRegionException e) {
            throw INCOMPLETE_SELECTION.create();
        }
    }

    private static int addBehavior(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Authority authority = AuthorityArgument.getAll(context, "authority");
        ResourceLocation behaviorId = AuthorityBehaviorArgument.get(context, "behavior").getFirst();

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.addBehavior(behaviorId));

        Component message = new TextComponent("Added behavior '")
                .append(new TextComponent(behaviorId.toString()).withStyle(ChatFormatting.AQUA))
                .append("' to '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GREEN);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int removeBehavior(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Authority authority = AuthorityArgument.getAll(context, "authority");
        ResourceLocation behaviorId = AuthorityBehaviorArgument.get(context, "behavior").getFirst();

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.removeBehavior(behaviorId));

        Component message = new TextComponent("Removed behavior '")
                .append(new TextComponent(behaviorId.toString()).withStyle(ChatFormatting.AQUA))
                .append("' from '")
                .append(new TextComponent(authority.key()).withStyle(ChatFormatting.AQUA))
                .append("'")
                .withStyle(ChatFormatting.GOLD);
        context.getSource().sendSuccess(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static ProtectionManager protection(CommandContext<CommandSourceStack> context) {
        return ProtectionManager.get(context.getSource().getServer());
    }
}
