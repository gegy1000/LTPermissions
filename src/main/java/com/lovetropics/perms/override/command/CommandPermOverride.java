package com.lovetropics.perms.override.command;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.perms.override.RoleOverride;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.JSONUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CommandPermOverride implements RoleOverride {
    private final Collection<Command> commands;

    private CommandPermOverride(List<Command> commands) {
        this.commands = commands;
    }

    public PermissionResult test(CommandNode<CommandSource> node) {
        for (Command permission : this.commands) {
            PermissionResult result = permission.test(node);
            if (result.isDefinitive()) return result;
        }
        return PermissionResult.PASS;
    }

    public static CommandPermOverride parse(JsonObject root) {
        ImmutableList.Builder<Command> commands = ImmutableList.builder();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String patternString = entry.getKey();
            String ruleName = JSONUtils.getString(entry.getValue(), "rule");

            Pattern pattern = Pattern.compile(patternString);
            PermissionResult rule = PermissionResult.byName(ruleName);
            commands.add(new Command(pattern, rule));
        }

        return new CommandPermOverride(commands.build());
    }

    @Override
    public void notifyChange(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server != null) server.getCommandManager().send(player);
    }

    @Override
    public String toString() {
        return "CommandPermOverride[" + this.commands.toString() + "]";
    }

    private static class Command {
        final Pattern pattern;
        final PermissionResult rule;

        Command(Pattern pattern, PermissionResult rule) {
            this.pattern = pattern;
            this.rule = rule;
        }

        // TODO: can't support specific sub-commands yet because only called on roots
        PermissionResult test(CommandNode<CommandSource> node) {
            if (this.pattern.matcher(node.getName()).matches()) {
                return this.rule;
            }
            return PermissionResult.PASS;
        }

        @Override
        public String toString() {
            return "\"" + this.pattern.pattern() + "\"=" + this.rule;
        }
    }
}
