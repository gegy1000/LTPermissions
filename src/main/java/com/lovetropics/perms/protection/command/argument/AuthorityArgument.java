package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.protection.ProtectionManager;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.UserAuthority;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public final class AuthorityArgument {
    private static final DynamicCommandExceptionType AUTHORITY_NOT_FOUND = new DynamicCommandExceptionType(arg ->
            new TranslationTextComponent("Authority with key '%s' was not found!", arg)
    );

    public static RequiredArgumentBuilder<CommandSource, String> argumentAll(String name) {
        return argument(name, ProtectionManager::allAuthorities);
    }

    public static RequiredArgumentBuilder<CommandSource, String> argumentUser(String name) {
        return argument(name, ProtectionManager::userAuthorities);
    }

    public static Authority getAll(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return get(context, name, ProtectionManager::getAuthorityByKey);
    }

    public static UserAuthority getUser(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return get(context, name, ProtectionManager::getUserAuthorityByKey);
    }

    private static RequiredArgumentBuilder<CommandSource, String> argument(String name, Function<ProtectionManager, Stream<? extends Authority>> authorities) {
        return Commands.argument(name, StringArgumentType.string())
                .suggests((context, builder) -> {
                    CommandSource source = context.getSource();
                    MinecraftServer server = source.getServer();
                    return ISuggestionProvider.suggest(
                            authorities.apply(ProtectionManager.get(server)).map(Authority::key),
                            builder
                    );
                });
    }

    private static <A extends Authority> A get(
            CommandContext<CommandSource> context, String name,
            BiFunction<ProtectionManager, String, A> getter
    ) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, name);

        CommandSource source = context.getSource();
        ProtectionManager protect = ProtectionManager.get(source.getServer());

        A authority = getter.apply(protect, key);
        if (authority == null) {
            throw AUTHORITY_NOT_FOUND.create(key);
        }

        return authority;
    }
}
