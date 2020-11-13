package com.lovetropics.perms.override.command;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.perms.override.RoleOverride;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.JSONUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CommandPermOverride implements RoleOverride {
    private final Collection<Command> commands;

    private CommandPermOverride(List<Command> commands) {
        this.commands = commands;
    }

    public PermissionResult test(MatchableCommand command) {
        for (Command permission : this.commands) {
            PermissionResult result = permission.test(command);
            if (result.isDefinitive()) return result;
        }
        return PermissionResult.PASS;
    }

    public static CommandPermOverride parse(JsonObject root) {
        ImmutableList.Builder<Command> commands = ImmutableList.builder();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String[] patternStrings = entry.getKey().split(" ");
            String ruleName = JSONUtils.getString(entry.getValue(), "rule");

            Pattern[] patterns = Arrays.stream(patternStrings).map(Pattern::compile).toArray(Pattern[]::new);
            PermissionResult rule = PermissionResult.byName(ruleName);
            commands.add(new Command(patterns, rule));
        }

        return new CommandPermOverride(commands.build());
    }

    @Override
    public void notifyChange(MinecraftServer server, UUID id) {
        ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(id);
        if (player != null) {
            server.getCommandManager().send(player);
        }
    }

    @Override
    public String toString() {
        return "CommandPermOverride[" + this.commands.toString() + "]";
    }

    private static class Command {
        final Pattern[] patterns;
        final PermissionResult rule;

        Command(Pattern[] patterns, PermissionResult rule) {
            this.patterns = patterns;
            this.rule = rule;
        }

        PermissionResult test(MatchableCommand command) {
            return command.matches(this.patterns) ? this.rule : PermissionResult.PASS;
        }

        @Override
        public String toString() {
            return "\"" + Arrays.toString(this.patterns) + "\"=" + this.rule;
        }
    }
}
