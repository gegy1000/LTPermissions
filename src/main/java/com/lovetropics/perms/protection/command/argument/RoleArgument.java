package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.role.Role;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;

public final class RoleArgument {
    private static final DynamicCommandExceptionType ROLE_DOES_NOT_EXIST = new DynamicCommandExceptionType(arg ->
            new TranslationTextComponent("Role with id '%s' does not exist!", arg)
    );

    public static RequiredArgumentBuilder<CommandSource, String> argument(String name) {
        return Commands.argument(name, StringArgumentType.string())
                .suggests((context, builder) -> {
                    return ISuggestionProvider.suggest(
                            LTPermissions.roles().stream().map(Role::id),
                            builder
                    );
                });
    }

    @Nonnull
    public static Role get(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, name);
        Role role = LTPermissions.roles().get(id);
        if (role == null) {
            throw ROLE_DOES_NOT_EXIST.create(id);
        }

        return role;
    }
}
