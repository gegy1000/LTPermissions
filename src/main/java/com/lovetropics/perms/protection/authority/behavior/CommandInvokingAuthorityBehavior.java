package com.lovetropics.perms.protection.authority.behavior;

import com.lovetropics.perms.LTPermissions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;

public final class CommandInvokingAuthorityBehavior implements AuthorityBehavior {
    private final String[] enter;
    private final String[] exit;
    private final boolean commandFeedback;

    public CommandInvokingAuthorityBehavior(String[] enter, String[] exit, boolean commandFeedback) {
        this.enter = enter;
        this.exit = exit;
        this.commandFeedback = commandFeedback;
    }

    @Override
    public void onPlayerEnter(ServerPlayerEntity player) {
        this.invokeCommands(player, this.enter);
    }

    @Override
    public void onPlayerExit(ServerPlayerEntity player) {
        this.invokeCommands(player, this.exit);
    }

    private void invokeCommands(ServerPlayerEntity player, String[] commands) {
        if (commands.length == 0) {
            return;
        }

        CommandSource source = this.getSource(player);
        Commands commandManager = player.server.getCommandManager();
        for (String command : commands) {
            try {
                commandManager.getDispatcher().execute(command, source);
            } catch (CommandSyntaxException e) {
                LTPermissions.LOGGER.error("Failed to execute command `{}`", command, e);
            }
        }
    }

    private CommandSource getSource(ServerPlayerEntity player) {
        CommandSource source = player.getCommandSource().withPermissionLevel(4);
        if (!this.commandFeedback) {
            source = source.withFeedbackDisabled();
        }
        return source;
    }
}
