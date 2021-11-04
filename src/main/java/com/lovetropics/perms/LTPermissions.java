package com.lovetropics.perms;

import com.lovetropics.perms.command.FlyCommand;
import com.lovetropics.perms.command.RoleCommand;
import com.lovetropics.perms.config.RolesConfig;
import com.lovetropics.perms.override.ChatFormatOverride;
import com.lovetropics.perms.override.NameStyleOverride;
import com.lovetropics.perms.override.RoleOverrideType;
import com.lovetropics.perms.override.command.CommandOverride;
import com.lovetropics.perms.protection.authority.shape.AuthorityShape;
import com.lovetropics.perms.protection.command.ProtectCommand;
import com.lovetropics.perms.role.RoleLookup;
import com.lovetropics.perms.role.RoleProvider;
import com.lovetropics.perms.role.RoleReader;
import com.lovetropics.perms.store.PlayerRoleManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
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
            .withChangeListener(player -> {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    server.getCommandManager().send(player);
                }
            });

    public static final RoleOverrideType<ChatFormatOverride> CHAT_FORMAT = RoleOverrideType.register("chat_format", ChatFormatOverride.CODEC);
    public static final RoleOverrideType<NameStyleOverride> NAME_STYLE = RoleOverrideType.register("name_style", NameStyleOverride.CODEC)
            .withChangeListener(PlayerEntity::refreshDisplayName);
    public static final RoleOverrideType<Boolean> MUTE = RoleOverrideType.register("mute", Codec.BOOL);

    private static final RoleLookup LOOKUP = new RoleLookup() {
        @Override
        @Nonnull
        public RoleReader byEntity(Entity entity) {
            if (entity instanceof ServerPlayerEntity) {
                RoleReader roles = PlayerRoleManager.get().getRolesForOnline(((ServerPlayerEntity) entity));
                return roles != null ? roles : RoleReader.EMPTY;
            }
            return RoleReader.EMPTY;
        }

        @Override
        @Nonnull
        public RoleReader bySource(CommandSource source) {
            Entity entity = source.getEntity();
            return entity != null ? this.byEntity(entity) : RoleReader.EMPTY;
        }
    };

    public LTPermissions() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerChat);

        // Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
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
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        RoleCommand.register(dispatcher);
        FlyCommand.register(dispatcher);
        ProtectCommand.register(dispatcher);

        CommandAliasConfiguration aliasConfig = CommandAliasConfiguration.load();
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

    private void onServerChat(ServerChatEvent event) {
        ServerPlayerEntity player = event.getPlayer();

        RoleReader roles = LTPermissions.lookup().byPlayer(player);
        if (roles.overrides().test(MUTE)) {
            player.sendStatusMessage(new StringTextComponent("You are muted!").mergeStyle(TextFormatting.RED), true);
            event.setCanceled(true);
        }
    }

    public static RoleProvider roles() {
        return RolesConfig.get();
    }

    public static RoleLookup lookup() {
        return LOOKUP;
    }
}
