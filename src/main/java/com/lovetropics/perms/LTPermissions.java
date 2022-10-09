package com.lovetropics.perms;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.RoleLookup;
import com.lovetropics.lib.permission.role.RoleOverrideType;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.perms.command.FlyCommand;
import com.lovetropics.perms.command.RoleCommand;
import com.lovetropics.perms.config.RolesConfig;
import com.lovetropics.perms.override.ChatFormatOverride;
import com.lovetropics.perms.override.NameDecorationOverride;
import com.lovetropics.perms.override.command.CommandOverride;
import com.lovetropics.perms.protection.authority.shape.AuthorityShape;
import com.lovetropics.perms.protection.command.ProtectCommand;
import com.lovetropics.perms.store.PlayerRoleManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@Mod(LTPermissions.ID)
public class LTPermissions {
    public static final String ID = "ltperms";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final RoleOverrideType<CommandOverride> COMMANDS = RoleOverrideType.register("commands", CommandOverride.CODEC)
            .withBuilder(CommandOverride::build)
            .withChangeListener(player -> {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    server.getCommands().sendCommands(player);
                }
            });

    public static final RoleOverrideType<ChatFormatOverride> CHAT_FORMAT = RoleOverrideType.register("chat_format", ChatFormatOverride.CODEC);

    public static final RoleOverrideType<NameDecorationOverride> NAME_DECORATION = RoleOverrideType.register("name_decoration", NameDecorationOverride.CODEC)
            .withBuilder(NameDecorationOverride::build)
            .withInitializeListener(Player::refreshDisplayName)
            .withChangeListener(Player::refreshDisplayName);

    public static final RoleOverrideType<Boolean> MUTE = RoleOverrideType.register("mute", Codec.BOOL);

    private static final RoleLookup LOOKUP = new RoleLookup() {
        @Override
        @Nonnull
        public RoleReader byEntity(Entity entity) {
            if (entity instanceof ServerPlayer) {
                RoleReader roles = PlayerRoleManager.get().getRolesForOnline(((ServerPlayer) entity));
                return roles != null ? roles : RoleReader.EMPTY;
            }
            return RoleReader.EMPTY;
        }

        @Override
        @Nonnull
        public RoleReader bySource(CommandSourceStack source) {
            Entity entity = source.getEntity();
            return entity != null ? this.byEntity(entity) : RoleReader.EMPTY;
        }
    };

    public LTPermissions() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerChat);

        // Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        PermissionsApi.setRoleLookup(LOOKUP);
    }

    private void setup(FMLCommonSetupEvent event) {
        List<String> errors = RolesConfig.setup();
        if (!errors.isEmpty()) {
            LOGGER.warn("Failed to load roles config! ({} errors)", errors.size());
            for (String error : errors) {
                LOGGER.warn(" - {}", error);
            }
        }

        CommandAliasConfiguration.load();

        AuthorityShape.register();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        RoleCommand.register(dispatcher);
        FlyCommand.register(dispatcher);
        ProtectCommand.register(dispatcher);

        CommandAliasConfiguration aliasConfig = CommandAliasConfiguration.load();
        for (Map.Entry<String, String[]> entry : aliasConfig.aliases().entrySet()) {
            String[] literals = entry.getKey().split(" ");

            LiteralArgumentBuilder<CommandSourceStack>[] nodes = new LiteralArgumentBuilder[literals.length];
            for (int i = 0; i < literals.length; i++) {
                nodes[i] = Commands.literal(literals[i]);
            }

            String[] commands = entry.getValue();
            nodes[nodes.length - 1].executes(context -> {
                CommandSourceStack source = context.getSource().withPermission(4).withSuppressedOutput();
                int result = Command.SINGLE_SUCCESS;
                for (String command : commands) {
                    result = dispatcher.execute(command, source);
                }
                return result;
            });

            LiteralArgumentBuilder<CommandSourceStack> chain = nodes[0];
            for (int i = 1; i < nodes.length; i++) {
                LiteralArgumentBuilder<CommandSourceStack> next = nodes[i];
                chain.then(next);
                chain = next;
            }

            dispatcher.register(nodes[0]);
        }
    }

    private void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();

        RoleReader roles = PermissionsApi.lookup().byPlayer(player);
        if (roles.overrides().test(MUTE)) {
            player.displayClientMessage(new TextComponent("You are muted!").withStyle(ChatFormatting.RED), true);
            event.setCanceled(true);
        }
    }
}
