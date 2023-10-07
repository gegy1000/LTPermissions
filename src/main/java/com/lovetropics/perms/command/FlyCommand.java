package com.lovetropics.perms.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import static net.minecraft.commands.Commands.literal;

public class FlyCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("fly")
				.requires(ctx -> ctx.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(literal("enable").executes(context -> setFlight(context, true)))
				.then(literal("disable").executes(context -> setFlight(context, false)))
				.executes(FlyCommand::toggleFlight));
	}

	private static int toggleFlight(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		setFlight(player, !player.getAbilities().mayfly);
		return Command.SINGLE_SUCCESS;
	}

	private static int setFlight(CommandContext<CommandSourceStack> context, boolean canFly) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		setFlight(player, canFly);
		return Command.SINGLE_SUCCESS;
	}

	private static void setFlight(final ServerPlayer player, final boolean canFly) {
		Abilities abilities = player.getAbilities();
		abilities.mayfly = canFly || player.isCreative();
		abilities.flying &= abilities.mayfly;
		player.onUpdateAbilities();
	}
}
