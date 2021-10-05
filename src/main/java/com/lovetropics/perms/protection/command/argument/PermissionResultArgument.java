package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.PermissionResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;

public final class PermissionResultArgument {
    public static RequiredArgumentBuilder<CommandSource, String> argument(String name) {
        return Commands.argument(name, StringArgumentType.string())
                .suggests((context, builder) -> {
                    return ISuggestionProvider.suggest(
                            PermissionResult.keysStream(),
                            builder
                    );
                });
    }

    public static PermissionResult get(CommandContext<CommandSource> context, String name) {
        String key = StringArgumentType.getString(context, name);
        return PermissionResult.byKey(key);
    }
}
