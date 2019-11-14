package com.lovetropics.perms.override.command;

import com.lovetropics.perms.LTPerms;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.Predicate;

public final class CommandRequirementHooks<S> {
    private final RequirementOverride<S> override;
    private final Field requirementField;

    private CommandRequirementHooks(RequirementOverride<S> override, Field requirementField) {
        this.override = override;
        this.requirementField = requirementField;
    }

    public static <S> CommandRequirementHooks<S> tryCreate(RequirementOverride<S> override) throws ReflectiveOperationException {
        Field requirementField = CommandNode.class.getDeclaredField("requirement");
        requirementField.setAccessible(true);
        return new CommandRequirementHooks<>(override, requirementField);
    }

    public void hookAll(CommandDispatcher<S> dispatcher) {
        Collection<CommandNode<S>> nodes = dispatcher.getRoot().getChildren();
        nodes.forEach(this::hookCommand);
    }

    public void hookCommand(CommandNode<S> node) {
        try {
            Predicate<S> requirement = node.getRequirement();
            this.requirementField.set(node, this.override.apply(node, requirement));
        } catch (IllegalAccessException e) {
            LTPerms.LOGGER.error("Failed to hook command node {}", node, e);
        }
    }

    public interface RequirementOverride<S> {
        Predicate<S> apply(CommandNode<S> node, Predicate<S> predicate);
    }
}
