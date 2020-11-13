package com.lovetropics.perms;

import com.lovetropics.perms.command.FlyCommand;
import com.lovetropics.perms.command.RoleCommand;
import com.lovetropics.perms.override.ChatStyleOverride;
import com.lovetropics.perms.override.RoleOverrideType;
import com.lovetropics.perms.override.command.CommandPermEvaluator;
import com.lovetropics.perms.override.command.CommandRequirementHooks;
import com.lovetropics.perms.override.command.MatchableCommand;
import com.lovetropics.perms.override.command.PermissionResult;
import com.lovetropics.perms.storage.PlayerRoleStorage;
import com.lovetropics.perms.storage.PlayerRoles;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@Mod(LTPerms.ID)
public class LTPerms {
    public static final String ID = "ltperms";

    public static final Logger LOGGER = LogManager.getLogger(ID);

    public LTPerms() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onChat);

        // Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void setup(FMLCommonSetupEvent event) {
        RoleConfiguration.setup();
        CommandAliasConfiguration.setup();
    }

    private void serverStarting(FMLServerStartingEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getCommandDispatcher();

        RoleCommand.register(dispatcher);
        FlyCommand.register(dispatcher);

        CommandAliasConfiguration aliasConfig = CommandAliasConfiguration.get();
        for (Map.Entry<String, String[]> entry : aliasConfig.getAliases().entrySet()) {
            String[] literals = entry.getKey().split(" ");

            LiteralArgumentBuilder<CommandSource>[] nodes = new LiteralArgumentBuilder[literals.length];
            for (int i = 0; i < literals.length; i++) {
                nodes[i] = Commands.literal(literals[i]);
            }

            String[] commands = entry.getValue();
            nodes[nodes.length - 1].executes(context -> {
                CommandSource source = context.getSource().withPermissionLevel(4);
                int result = Command.SINGLE_SUCCESS;
                for (String command : commands) {
                    result = dispatcher.execute(command, source);
                }
                return result;
            });

            LiteralArgumentBuilder<CommandSource> chain = nodes[0];
            for (int i = 1; i < nodes.length; i++) {
                LiteralArgumentBuilder<CommandSource> next = nodes[i];
                chain.then(next);
                chain = next;
            }

            dispatcher.register(nodes[0]);
        }
    }

    private void serverStarted(FMLServerStartedEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getServer().getCommandManager().getDispatcher();

        try {
            CommandRequirementHooks<CommandSource> hooks = CommandRequirementHooks.tryCreate((nodes, predicate) -> {
                MatchableCommand command = MatchableCommand.compile(nodes);

                return source -> {
                    PermissionResult result = CommandPermEvaluator.canUseCommand(source, command);
                    if (result == PermissionResult.ALLOW) return true;
                    if (result == PermissionResult.DENY) return false;

                    return predicate.test(source);
                };
            });

            hooks.hookAll(dispatcher);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to hook command requirements", e);
        }
    }

    private void onChat(ServerChatEvent event) {
        ServerPlayerEntity player = event.getPlayer();

        PlayerRoleStorage storage = PlayerRoleStorage.forServer(player.server);
        PlayerRoles roles = storage.getOrNull(player);
        if (roles != null) {
            ChatStyleOverride chatStyle = roles.getHighest(RoleOverrideType.CHAT_STYLE);
            if (chatStyle != null) {
                event.setComponent(chatStyle.make(player.getDisplayName(), event.getMessage()));
            }
        }
    }
}
