package com.lovetropics.perms.override;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.role.RoleReader;
import com.mojang.serialization.Codec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
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
                List<TextFormatting> formats = new ArrayList<>();
                Color color = null;

                for (String formatKey : formatKeys) {
                    TextFormatting format = TextFormatting.getValueByName(formatKey);
                    if (format != null) {
                        formats.add(format);
                    } else {
                        Color parsedColor = Color.fromHex(formatKey);
                        if (parsedColor != null) {
                            color = parsedColor;
                        }
                    }
                }

                return new NameStyleOverride(formats.toArray(new TextFormatting[0]), color);
            },
            override -> {
                List<String> formatKeys = new ArrayList<>();
                if (override.color != null) {
                    formatKeys.add(override.color.getName());
                }

                for (TextFormatting format : override.formats) {
                    formatKeys.add(format.getFriendlyName());
                }

                return formatKeys;
            }
    );

    private final TextFormatting[] formats;
    private final Color color;

    NameStyleOverride(TextFormatting[] formats, @Nullable Color color) {
        this.formats = formats;
        this.color = color;
    }

    public TextFormatting[] formats() {
        return this.formats;
    }

    public Color color() {
        return this.color;
    }

    public IFormattableTextComponent apply(IFormattableTextComponent text) {
        return text.setStyle(this.applyStyle(text.getStyle()));
    }

    private Style applyStyle(Style style) {
        style = style.mergeWithFormatting(this.formats);
        if (this.color != null) {
            style = style.setColor(this.color);
        }
        return style;
    }

    @SubscribeEvent
    public static void onFormatName(PlayerEvent.NameFormat event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            ITextComponent displayName = event.getDisplayname();
            if (displayName.getStyle().getColor() == null && !hasTeamColor((ServerPlayerEntity) player)) {
                RoleReader roles = LTPermissions.lookup().byPlayer(player);

                NameStyleOverride nameStyle = roles.overrides().select(LTPermissions.NAME_STYLE);
                if (nameStyle != null) {
                    event.setDisplayname(nameStyle.apply(displayName.deepCopy()));
                }
            }
        }
    }

    private static boolean hasTeamColor(ServerPlayerEntity player) {
        Team team = player.getTeam();
        if (team instanceof ScorePlayerTeam) {
            ScorePlayerTeam scoreTeam = (ScorePlayerTeam) team;
            TextFormatting color = scoreTeam.getColor();
            return color != TextFormatting.RESET;
        }
        return false;
    }
}
