package com.lovetropics.perms.command;

import static net.minecraft.command.Commands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;

public class FlyCommand {
	
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("fly")
        		  .requires(ctx -> ctx.hasPermissionLevel(2))
        		  .executes(ctx -> {
			        	ServerPlayerEntity player = ctx.getSource().asPlayer();
			        	player.abilities.allowFlying = !player.abilities.allowFlying;
			        	player.sendPlayerAbilities();
			        	return Command.SINGLE_SUCCESS;
        		  }));
    }
}
