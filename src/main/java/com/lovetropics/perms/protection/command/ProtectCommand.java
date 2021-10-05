package com.lovetropics.perms.protection.command;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.ProtectionManager;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.UserAuthority;
import com.lovetropics.perms.protection.authority.shape.AuthorityShape;
import com.lovetropics.perms.protection.authority.shape.WorldEditShapes;
import com.lovetropics.perms.protection.command.argument.AuthorityArgument;
import com.lovetropics.perms.protection.command.argument.AuthorityShapeArgument;
import com.lovetropics.perms.protection.command.argument.PermissionResultArgument;
import com.lovetropics.perms.protection.command.argument.ProtectionRuleArgument;
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
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

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

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // @formatter:off
        dispatcher.register(
            literal("protect")
                .requires(source -> source.hasPermissionLevel(3))
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
        );
        // @formatter:on
    }

    private static int addAuthority(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "authority");
        int level = IntegerArgumentType.getInteger(context, "level");

        ProtectionManager protection = protection(context);

        UserAuthority authority = UserAuthority.create(key).withLevel(level);
        if (protection.addAuthority(authority)) {
            ITextComponent message = new StringTextComponent("Added authority '")
                    .appendSibling(new StringTextComponent(key).mergeStyle(TextFormatting.AQUA))
                    .appendString("' ")
                    .appendSibling(new StringTextComponent("@" + level).mergeStyle(TextFormatting.AQUA))
                    .mergeStyle(TextFormatting.GREEN);
            context.getSource().sendFeedback(message, true);
        } else {
            throw AUTHORITY_ALREADY_EXISTS.create(key);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int removeAuthority(CommandContext<CommandSource> context) throws CommandSyntaxException {
        UserAuthority authority = AuthorityArgument.getUser(context, "authority");

        ProtectionManager protection = protection(context);
        if (protection.removeAuthority(authority)) {
            ITextComponent message = new StringTextComponent("Removed authority '")
                    .appendSibling(new StringTextComponent(authority.key()).mergeStyle(TextFormatting.AQUA))
                    .appendString("'")
                    .mergeStyle(TextFormatting.GOLD);
            context.getSource().sendFeedback(message, true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setShape(CommandContext<CommandSource> context) throws CommandSyntaxException {
        UserAuthority authority = AuthorityArgument.getUser(context, "authority");
        String shape = StringArgumentType.getString(context, "shape");

        AuthorityShape selection = getSelectionFor(context.getSource());

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.addShape(shape, selection));

        ITextComponent message = new StringTextComponent("Set shape on '")
                .appendSibling(new StringTextComponent(authority.key()).mergeStyle(TextFormatting.AQUA))
                .appendString("' with key '")
                .appendSibling(new StringTextComponent(shape).mergeStyle(TextFormatting.AQUA))
                .appendString("'")
                .mergeStyle(TextFormatting.GREEN);
        context.getSource().sendFeedback(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int removeShape(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Pair<UserAuthority, String> shape = AuthorityShapeArgument.get(context, "authority", "shape");
        UserAuthority authority = shape.getFirst();
        String shapeKey = shape.getSecond();

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.removeShape(shapeKey));

        ITextComponent message = new StringTextComponent("Removed shape from '")
                .appendSibling(new StringTextComponent(authority.key()).mergeStyle(TextFormatting.AQUA))
                .appendString("' with key '")
                .appendSibling(new StringTextComponent(shapeKey).mergeStyle(TextFormatting.AQUA))
                .appendString("'")
                .mergeStyle(TextFormatting.GOLD);
        context.getSource().sendFeedback(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int selectShape(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Pair<UserAuthority, String> shape = AuthorityShapeArgument.get(context, "authority", "shape");
        UserAuthority authority = shape.getFirst();
        String shapeKey = shape.getSecond();

        applySelectionFor(context.getSource(), authority.shape().get(shapeKey));

        ITextComponent message = new StringTextComponent("Selected shape from '")
                .appendSibling(new StringTextComponent(authority.key()).mergeStyle(TextFormatting.AQUA))
                .appendString("' with key '")
                .appendSibling(new StringTextComponent(shapeKey).mergeStyle(TextFormatting.AQUA))
                .appendString("'")
                .mergeStyle(TextFormatting.GREEN);
        context.getSource().sendFeedback(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static int setRule(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Authority authority = AuthorityArgument.getAll(context, "authority");
        ProtectionRule rule = ProtectionRuleArgument.get(context, "rule");
        PermissionResult result = PermissionResultArgument.get(context, "result");

        ProtectionManager protection = protection(context);
        protection.replaceAuthority(authority, authority.withRule(rule, result));

        ITextComponent message = new StringTextComponent("Set rule ")
                .appendSibling(new StringTextComponent(rule.key() + "=").appendSibling(result.getName()).mergeStyle(TextFormatting.AQUA))
                .appendString(" for '")
                .appendSibling(new StringTextComponent(authority.key()).mergeStyle(TextFormatting.AQUA))
                .appendString("'")
                .mergeStyle(TextFormatting.GREEN);
        context.getSource().sendFeedback(message, true);

        return Command.SINGLE_SUCCESS;
    }

    private static void applySelectionFor(CommandSource source, AuthorityShape shape) throws CommandSyntaxException {
        SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
        ForgePlayer player = ForgeAdapter.adaptPlayer(source.asPlayer());
        LocalSession session = sessionManager.get(player);

        RegionSelector selector = WorldEditShapes.tryIntoRegionSelector(source.getServer(), shape);
        if (selector == null) {
            throw UNREPRESENTABLE_REGION.create();
        }

        session.setRegionSelector(selector.getWorld(), selector);
        session.dispatchCUISelection(player);
    }

    private static AuthorityShape getSelectionFor(CommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.asPlayer();

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

    private static ProtectionManager protection(CommandContext<CommandSource> context) {
        return ProtectionManager.get(context.getSource().getServer());
    }
}
