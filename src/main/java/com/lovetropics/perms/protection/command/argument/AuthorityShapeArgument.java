package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.protection.authority.UserAuthority;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class AuthorityShapeArgument {
    private static final DynamicCommandExceptionType SHAPE_NOT_FOUND = new DynamicCommandExceptionType(arg ->
            Component.translatable("Shape with key '%s' was not found!", arg)
    );

    public static RequiredArgumentBuilder<CommandSourceStack, String> argument(String authorityName, String shapeName) {
        return Commands.argument(shapeName, StringArgumentType.string())
                .suggests((context, builder) -> {
                    UserAuthority authority = AuthorityArgument.getUser(context, authorityName);
                    return SharedSuggestionProvider.suggest(
                            authority.shape().keySet().stream(),
                            builder
                    );
                });
    }

    public static Pair<UserAuthority, String> get(CommandContext<CommandSourceStack> context, String authorityName, String shapeName) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, shapeName);
        UserAuthority authority = AuthorityArgument.getUser(context, authorityName);

        if (authority.shape().get(key) == null) {
            throw SHAPE_NOT_FOUND.create(key);
        }

        return Pair.of(authority, key);
    }
}
