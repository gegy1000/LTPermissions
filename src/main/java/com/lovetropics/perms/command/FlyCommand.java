package com.lovetropics.perms.command;

import static net.minecraft.command.Commands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;

public class FlyCommand {
	
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("fly")
        		  .requires(ctx -> ctx.hasPermission(2))
        		  .executes(ctx -> {
			        	ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
			        	player.abilities.mayfly = !player.abilities.mayfly;
			        	player.onUpdateAbilities();
			        	return Command.SINGLE_SUCCESS;
        		  }));
    }
}
