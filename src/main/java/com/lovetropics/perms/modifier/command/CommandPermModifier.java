package com.lovetropics.perms.modifier.command;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.perms.modifier.RoleModifier;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.util.JSONUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CommandPermModifier implements RoleModifier {
    private final Collection<Command> commands;

    private CommandPermModifier(List<Command> commands) {
        this.commands = commands;
    }

    public PermissionResult test(CommandNode<CommandSource> node) {
        for (Command permission : this.commands) {
            PermissionResult result = permission.test(node);
            if (result.isDefinitive()) return result;
        }
        return PermissionResult.PASS;
    }

    public static CommandPermModifier parse(JsonObject root) {
        ImmutableList.Builder<Command> commands = ImmutableList.builder();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String patternString = entry.getKey();
            String ruleName = JSONUtils.getString(entry.getValue(), "rule");

            Pattern pattern = Pattern.compile(patternString);
            PermissionResult rule = PermissionResult.byName(ruleName);
            commands.add(new Command(pattern, rule));
        }

        return new CommandPermModifier(commands.build());
    }

    @Override
    public String toString() {
        return "CommandPermModifier[" + this.commands.toString() + "]";
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
