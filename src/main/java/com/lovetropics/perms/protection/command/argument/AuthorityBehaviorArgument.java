package com.lovetropics.perms.protection.command.argument;

import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfig;
import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfigs;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TranslatableComponent;

public final class AuthorityBehaviorArgument {
    private static final DynamicCommandExceptionType BEHAVIOR_DOES_NOT_EXIST = new DynamicCommandExceptionType(arg ->
            new TranslatableComponent("Behavior with id '%s' does not exist!", arg)
    );

    public static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> argument(String name) {
        return Commands.argument(name, ResourceLocationArgument.id())
                .suggests((context, builder) -> {
                    return SharedSuggestionProvider.suggestResource(
                            AuthorityBehaviorConfigs.REGISTRY.keySet().stream(),
                            builder
                    );
                });
    }

    public static Pair<ResourceLocation, AuthorityBehaviorConfig> get(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, name);
        AuthorityBehaviorConfig config = AuthorityBehaviorConfigs.REGISTRY.get(id);
        if (config == null) {
            throw BEHAVIOR_DOES_NOT_EXIST.create(id);
        }

        return Pair.of(id, config);
    }
}
