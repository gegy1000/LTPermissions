package com.lovetropics.perms.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import static net.minecraft.commands.Commands.literal;

public class FlyCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("fly")
				.requires(ctx -> ctx.hasPermission(2))
				.then(literal("enable").executes(context -> updateFly(context, true)))
				.then(literal("disable").executes(context -> updateFly(context, false)))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					player.getAbilities().mayfly = !player.getAbilities().mayfly;
					player.onUpdateAbilities();
					return Command.SINGLE_SUCCESS;
				}));
	}

	private static int updateFly(CommandContext<CommandSourceStack> context, boolean canFly) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Abilities abilities = player.getAbilities();
		canFly |= player.isCreative();
		abilities.mayfly = canFly;
		if (!canFly) {
			abilities.flying = false;
		}
		player.onUpdateAbilities();
		return Command.SINGLE_SUCCESS;
	}
}
