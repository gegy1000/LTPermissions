package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.PermissionResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public final class PermissionResultArgument {
    public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String name) {
        return Commands.argument(name, StringArgumentType.string())
                .suggests((context, builder) -> {
                    return SharedSuggestionProvider.suggest(
                            PermissionResult.keysStream(),
                            builder
                    );
                });
    }

    public static PermissionResult get(CommandContext<CommandSourceStack> context, String name) {
        String key = StringArgumentType.getString(context, name);
        return PermissionResult.byKey(key);
    }
}
