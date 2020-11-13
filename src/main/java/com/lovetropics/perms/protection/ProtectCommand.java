package com.lovetropics.perms.protection;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.scope.ProtectionScope;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public final class ProtectCommand {
    private static final DynamicCommandExceptionType NO_RULE = new DynamicCommandExceptionType(id -> {
        return new LiteralMessage("There is no rule with the id '" + id + "'");
    });

    private static final DynamicCommandExceptionType NO_RESULT = new DynamicCommandExceptionType(id -> {
        return new LiteralMessage("There is no permission result with the id '" + id + "'");
    });

    private static final DynamicCommandExceptionType NO_REGION = new DynamicCommandExceptionType(id -> {
        return new LiteralMessage("There is no region with the id '" + id + "'");
    });

    private static final DynamicCommandExceptionType REGION_ALREADY_EXISTS = new DynamicCommandExceptionType(id -> {
        return new LiteralMessage("Region with the id '" + id + "' already exists!");
    });

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // @formatter:off
        dispatcher.register(
            literal("protect")
                .requires(source -> source.hasPermissionLevel(4))
                .then(literal("add")
                    .then(argument("key", StringArgumentType.string())
                    .then(argument("level", IntegerArgumentType.integer())
                    .executes(ProtectCommand::addGlobal)
                        .then(argument("dimension", DimensionArgument.getDimension())
                            .executes(ProtectCommand::addDimension)
                                .then(argument("min", BlockPosArgument.blockPos())
                                .then(argument("max", BlockPosArgument.blockPos())
                                .executes(ProtectCommand::addBox)
                        ))
                    )
                )))
                .then(literal("remove")
                    .then(argument("key", StringArgumentType.string()).suggests(regionSuggestions())
                    .executes(ProtectCommand::remove)
                ))
                .then(literal("rule")
                    .then(literal("set")
                        .then(argument("key", StringArgumentType.string()).suggests(regionSuggestions())
                        .then(argument("rule", StringArgumentType.string()).suggests(ruleSuggestions())
                        .then(argument("result", StringArgumentType.string()).suggests(ruleResultSuggestions())
                        .executes(ProtectCommand::setRule)
                    ))))
                )
        );
        // @formatter:on
    }

    private static int addBox(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        int level = IntegerArgumentType.getInteger(context, "level");
        DimensionType dimension = DimensionArgument.getDimensionArgument(context, "dimension");
        BlockPos min = BlockPosArgument.getBlockPos(context, "min");
        BlockPos max = BlockPosArgument.getBlockPos(context, "max");

        ProtectionManager protection = ProtectionManager.get(context.getSource().getServer());
        if (protection.add(new ProtectionRegion(key, ProtectionScope.box(dimension, min, max), level))) {
            context.getSource().sendFeedback(new StringTextComponent("Added region in " + dimension + " " + key + "@" + level), true);
        } else {
            throw REGION_ALREADY_EXISTS.create(key);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int addDimension(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        int level = IntegerArgumentType.getInteger(context, "level");
        DimensionType dimension = DimensionArgument.getDimensionArgument(context, "dimension");

        ProtectionManager protection = ProtectionManager.get(context.getSource().getServer());
        if (protection.add(new ProtectionRegion(key, ProtectionScope.dimension(dimension), level))) {
            context.getSource().sendFeedback(new StringTextComponent("Added region in " + dimension + " " + key + "@" + level), true);
        } else {
            throw REGION_ALREADY_EXISTS.create(key);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int addGlobal(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        int level = IntegerArgumentType.getInteger(context, "level");

        ProtectionManager protection = ProtectionManager.get(context.getSource().getServer());
        if (protection.add(new ProtectionRegion(key, ProtectionScope.global(), level))) {
            context.getSource().sendFeedback(new StringTextComponent("Added global region " + key + "@" + level), true);
        } else {
            throw REGION_ALREADY_EXISTS.create(key);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int remove(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");

        ProtectionManager protection = ProtectionManager.get(context.getSource().getServer());
        if (protection.remove(key)) {
            context.getSource().sendFeedback(new StringTextComponent("Removed region " + key), true);
        } else {
            throw NO_REGION.create(key);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setRule(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");

        String ruleId = StringArgumentType.getString(context, "rule");
        ProtectionRule rule = ProtectionRule.byKey(ruleId);
        if (rule == null) {
            throw NO_RULE.create(ruleId);
        }

        String resultId = StringArgumentType.getString(context, "result");
        PermissionResult result = PermissionResult.byName(resultId);
        if (result == null) {
            throw NO_RESULT.create(ruleId);
        }

        ProtectionManager protection = ProtectionManager.get(context.getSource().getServer());
        ProtectionRegion region = protection.byKey(key);
        if (region == null) {
            throw NO_REGION.create(key);
        }

        region.rules.put(rule, result);
        context.getSource().sendFeedback(new StringTextComponent("Set rule " + ruleId + " = " + resultId + " for " + key), true);

        return Command.SINGLE_SUCCESS;
    }

    private static SuggestionProvider<CommandSource> ruleSuggestions() {
        return (ctx, builder) -> {
            return ISuggestionProvider.suggest(
                    ProtectionRule.keys().stream(),
                    builder
            );
        };
    }

    private static SuggestionProvider<CommandSource> ruleResultSuggestions() {
        return (ctx, builder) -> {
            return ISuggestionProvider.suggest(
                    PermissionResult.keysStream(),
                    builder
            );
        };
    }

    private static SuggestionProvider<CommandSource> regionSuggestions() {
        return (ctx, builder) -> {
            ProtectionManager protection = ProtectionManager.get(ctx.getSource().getServer());
            return ISuggestionProvider.suggest(
                    protection.getRegionKeys().stream(),
                    builder
            );
        };
    }
}
