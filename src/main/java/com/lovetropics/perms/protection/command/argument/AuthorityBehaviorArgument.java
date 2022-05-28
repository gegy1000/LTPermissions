package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfig;
import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfigs;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public final class AuthorityBehaviorArgument {
    private static final DynamicCommandExceptionType BEHAVIOR_DOES_NOT_EXIST = new DynamicCommandExceptionType(arg ->
            new TranslationTextComponent("Behavior with id '%s' does not exist!", arg)
    );

    public static RequiredArgumentBuilder<CommandSource, ResourceLocation> argument(String name) {
        return Commands.argument(name, ResourceLocationArgument.id())
                .suggests((context, builder) -> {
                    return ISuggestionProvider.suggestResource(
                            AuthorityBehaviorConfigs.REGISTRY.keySet().stream(),
                            builder
                    );
                });
    }

    public static Pair<ResourceLocation, AuthorityBehaviorConfig> get(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, name);
        AuthorityBehaviorConfig config = AuthorityBehaviorConfigs.REGISTRY.get(id);
        if (config == null) {
            throw BEHAVIOR_DOES_NOT_EXIST.create(id);
        }

        return Pair.of(id, config);
    }
}
