package com.lovetropics.perms.override.command;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.role.RoleReader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;

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
    public static void onServerStarted(FMLServerStartedEvent event) {
        hookCommands(event.getServer().getCommandManager().getDispatcher());
    }

    @SubscribeEvent
    public static void onDataPacksReload(TagsUpdatedEvent.CustomTagTypes event) {
        // hack to listen to data pack reload so that we can re-hook commands
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        hookCommands(server.getCommandManager().getDispatcher());
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
