package com.lovetropics.perms.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class FlyCommand {
	
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("fly")
        		  .requires(ctx -> ctx.hasPermission(2))
        		  .executes(ctx -> {
			        	ServerPlayer player = ctx.getSource().getPlayerOrException();
			        	player.getAbilities().mayfly = !player.getAbilities().mayfly;
			        	player.onUpdateAbilities();
			        	return Command.SINGLE_SUCCESS;
        		  }));
    }
}
