package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.protection.ProtectionRule;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ProtectionRuleArgument {
    private static final DynamicCommandExceptionType RULE_DOES_NOT_EXIST = new DynamicCommandExceptionType(arg ->
            Component.translatable("Rule with key '%s' does not exist!", arg)
    );

    public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String name) {
        return Commands.argument(name, StringArgumentType.string())
                .suggests((context, builder) -> {
                    return SharedSuggestionProvider.suggest(
                            ProtectionRule.keySet().stream(),
                            builder
                    );
                });
    }

    public static ProtectionRule get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, name);
        ProtectionRule rule = ProtectionRule.byKey(key);
        if (rule == null) {
            throw RULE_DOES_NOT_EXIST.create(key);
        }

        return rule;
    }
}
