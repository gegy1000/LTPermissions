package com.lovetropics.perms.override;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.perms.LTPermissions;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class NameStyleOverride {
    public static final Codec<NameStyleOverride> CODEC = MoreCodecs.listOrUnit(Codec.STRING).xmap(
            formatKeys -> {
                List<ChatFormatting> formats = new ArrayList<>();
                TextColor color = null;

                for (String formatKey : formatKeys) {
                    ChatFormatting format = ChatFormatting.getByName(formatKey);
                    if (format != null) {
                        formats.add(format);
                    } else {
                        TextColor parsedColor = TextColor.parseColor(formatKey);
                        if (parsedColor != null) {
                            color = parsedColor;
                        }
                    }
                }

                return new NameStyleOverride(formats.toArray(new ChatFormatting[0]), color);
            },
            override -> {
                List<String> formatKeys = new ArrayList<>();
                if (override.color != null) {
                    formatKeys.add(override.color.serialize());
                }

                for (ChatFormatting format : override.formats) {
                    formatKeys.add(format.getName());
                }

                return formatKeys;
            }
    );

    private final ChatFormatting[] formats;
    private final TextColor color;

    NameStyleOverride(ChatFormatting[] formats, @Nullable TextColor color) {
        this.formats = formats;
        this.color = color;
    }

    public ChatFormatting[] formats() {
        return this.formats;
    }

    public TextColor color() {
        return this.color;
    }

    public MutableComponent apply(MutableComponent text) {
        return text.setStyle(this.applyStyle(text.getStyle()));
    }

    private Style applyStyle(Style style) {
        style = style.applyFormats(this.formats);
        if (this.color != null) {
            style = style.withColor(this.color);
        }
        return style;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // TODO: temporary patch to make sure name style updates! in cases like teams changing we aren't refreshing.
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
            if (event.player.tickCount % (20 * 10) == 0) {
                event.player.refreshDisplayName();
            }
        }
    }

    @SubscribeEvent
    public static void onFormatName(PlayerEvent.NameFormat event) {
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer) {
            Component displayName = event.getDisplayname();
            if (displayName.getStyle().getColor() == null && !hasTeamColor((ServerPlayer) player)) {
                RoleReader roles = PermissionsApi.lookup().byPlayer(player);

                NameStyleOverride nameStyle = roles.overrides().select(LTPermissions.NAME_STYLE);
                if (nameStyle != null) {
                    event.setDisplayname(nameStyle.apply(displayName.copy()));
                }
            }
        }
    }

    private static boolean hasTeamColor(ServerPlayer player) {
        Team team = player.getTeam();
        if (team instanceof PlayerTeam) {
            PlayerTeam scoreTeam = (PlayerTeam) team;
            ChatFormatting color = scoreTeam.getColor();
            return color != ChatFormatting.RESET;
        }
        return false;
    }
}
