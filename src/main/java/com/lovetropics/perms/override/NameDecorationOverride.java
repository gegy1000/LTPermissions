package com.lovetropics.perms.override;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.perms.LTPermissions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public record NameDecorationOverride(
		Optional<AddPrefix> prefix,
		Optional<AddSuffix> suffix,
		Optional<ApplyStyle> applyStyle
) {
	private static final NameDecorationOverride EMPTY = new NameDecorationOverride(Optional.empty(), Optional.empty(), Optional.empty());

	public static final Codec<NameDecorationOverride> CODEC = RecordCodecBuilder.create(i -> i.group(
			AddPrefix.CODEC.optionalFieldOf("prefix").forGetter(NameDecorationOverride::prefix),
			AddSuffix.CODEC.optionalFieldOf("suffix").forGetter(NameDecorationOverride::suffix),
			ApplyStyle.CODEC.optionalFieldOf("style").forGetter(NameDecorationOverride::applyStyle)
	).apply(i, NameDecorationOverride::new));

	public Component apply(MutableComponent name) {
		if (applyStyle.isPresent()) {
			name = applyStyle.get().apply(name);
		}
		if (prefix.isPresent()) {
			name = prefix.get().apply(name);
		}
		if (suffix.isPresent()) {
			name = suffix.get().apply(name);
		}
		return name;
	}

	public static NameDecorationOverride build(List<NameDecorationOverride> overrides) {
		if (overrides.isEmpty()) {
			return EMPTY;
		}

		Optional<AddPrefix> prefix = join(overrides.stream().flatMap(override -> override.prefix().stream()).map(AddPrefix::prefix)).map(AddPrefix::new);
		Optional<AddSuffix> suffix = join(overrides.stream().flatMap(override -> override.suffix().stream()).map(AddSuffix::suffix)).map(AddSuffix::new);
		Optional<ApplyStyle> style = overrides.get(0).applyStyle();
		return new NameDecorationOverride(prefix, suffix, style);
	}

	private static Optional<Component> join(Stream<Component> stream) {
		List<Component> components = stream.toList();
		if (components.isEmpty()) {
			return Optional.empty();
		}
		MutableComponent result = new TextComponent("");
		components.forEach(result::append);
		return Optional.of(result);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		// TODO: temporary patch to make sure name style updates! in cases like teams changing we aren't refreshing.
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer) {
			if (event.player.tickCount % (SharedConstants.TICKS_PER_SECOND * 10) == 0) {
				event.player.refreshDisplayName();
			}
		}
	}

	@SubscribeEvent
	public static void onFormatName(PlayerEvent.NameFormat event) {
		final Player player = event.getPlayer();
		if (player instanceof ServerPlayer) {
			final Component displayName = event.getDisplayname();
			if (displayName.getStyle().getColor() == null && !hasTeamColor((ServerPlayer) player)) {
				final RoleReader roles = PermissionsApi.lookup().byPlayer(player);

				final NameDecorationOverride nameDecoration = roles.overrides().getOrNull(LTPermissions.NAME_DECORATION);
				if (nameDecoration != null) {
					event.setDisplayname(nameDecoration.apply(displayName.copy()));
				}
			}
		}
	}

	private static boolean hasTeamColor(ServerPlayer player) {
		if (player.getTeam() instanceof PlayerTeam team) {
			return team.getColor() != ChatFormatting.RESET;
		}
		return false;
	}

	public record AddPrefix(Component prefix) {
		public static final Codec<AddPrefix> CODEC = MoreCodecs.TEXT.xmap(AddPrefix::new, AddPrefix::prefix);

		public MutableComponent apply(final MutableComponent name) {
			return new TextComponent("").append(this.prefix).append(name);
		}
	}

	public record AddSuffix(Component suffix) {
		public static final Codec<AddSuffix> CODEC = MoreCodecs.TEXT.xmap(AddSuffix::new, AddSuffix::suffix);

		public MutableComponent apply(final MutableComponent name) {
			return name.append(suffix);
		}
	}

	public record ApplyStyle(ChatFormatting[] formats, @Nullable TextColor color) {
		public static final Codec<ApplyStyle> CODEC = MoreCodecs.listOrUnit(Codec.STRING).xmap(
				formatKeys -> {
					final List<ChatFormatting> formats = new ArrayList<>();
					TextColor color = null;

					for (String formatKey : formatKeys) {
						final ChatFormatting format = ChatFormatting.getByName(formatKey);
						if (format != null) {
							formats.add(format);
						} else {
							final TextColor parsedColor = TextColor.parseColor(formatKey);
							if (parsedColor != null) {
								color = parsedColor;
							}
						}
					}

					return new ApplyStyle(formats.toArray(new ChatFormatting[0]), color);
				},
				override -> {
					final List<String> formatKeys = new ArrayList<>();
					if (override.color != null) {
						formatKeys.add(override.color.serialize());
					}

					for (final ChatFormatting format : override.formats) {
						formatKeys.add(format.getName());
					}

					return formatKeys;
				}
		);

		public MutableComponent apply(final MutableComponent text) {
			return text.setStyle(applyStyle(text.getStyle()));
		}

		private Style applyStyle(Style style) {
			style = style.applyFormats(formats);
			if (color != null) {
				style = style.withColor(color);
			}
			return style;
		}
	}
}
