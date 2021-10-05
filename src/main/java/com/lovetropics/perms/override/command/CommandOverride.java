package com.lovetropics.perms.override.command;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.role.RoleReader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class CommandOverride {
    public static final Codec<CommandOverride> CODEC = CommandOverrideRules.CODEC.xmap(CommandOverride::new, CommandOverride::rules);

    private final CommandOverrideRules rules;

    CommandOverride(CommandOverrideRules rules) {
        this.rules = rules;
    }

    public CommandOverrideRules rules() {
        return this.rules;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        hookCommands(event.getDispatcher());
    }

    private static void hookCommands(CommandDispatcher<CommandSource> dispatcher) {
        try {
            CommandRequirementHooks<CommandSource> hooks = CommandRequirementHooks.tryCreate((nodes, parent) -> {
                MatchableCommand command = MatchableCommand.compile(nodes);

                return source -> {
                    switch (canUseCommand(source, command)) {
                        case ALLOW: return true;
                        case DENY: return false;
                        default: return parent.test(source);
                    }
                };
            });

            hooks.applyTo(dispatcher);
        } catch (ReflectiveOperationException e) {
            LTPermissions.LOGGER.error("Failed to reflect into command requirements!", e);
        }
    }

    private static PermissionResult canUseCommand(CommandSource source, MatchableCommand command) {
        if (doesBypassPermissions(source)) return PermissionResult.PASS;

        RoleReader roles = LTPermissions.lookup().bySource(source);
        return roles.overrides().test(LTPermissions.COMMANDS, m -> m.test(command));
    }

    public static boolean doesBypassPermissions(CommandSource source) {
        return source.hasPermissionLevel(4);
    }

    public PermissionResult test(MatchableCommand command) {
        return this.rules.test(command);
    }
}
