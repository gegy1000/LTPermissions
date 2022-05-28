package com.lovetropics.perms.override;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.role.RoleReader;
import com.mojang.serialization.Codec;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatFormatOverride {
    public static final Codec<ChatFormatOverride> CODEC = Codec.STRING.xmap(
            ChatFormatOverride::new,
            override -> override.formatString
    );

    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private static final Object NAME_MARKER = new Object();
    private static final Object CONTENT_MARKER = new Object();

    private final Object[] format;
    private final String formatString;
    private final Builder builder = new Builder();

    public ChatFormatOverride(String format) {
        this.format = parseFormat(format);
        this.formatString = format;
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayerEntity player = event.getPlayer();
        RoleReader roles = LTPermissions.lookup().byPlayer(player);

        ChatFormatOverride chatFormat = roles.overrides().select(LTPermissions.CHAT_FORMAT);
        if (chatFormat != null) {
            event.setComponent(chatFormat.make(player.getDisplayName(), event.getMessage()));
        }
    }

    private static Object[] parseFormat(String formatString) {
        Parser parser = new Parser();

        int lastIdx = 0;
        int currentArgumentIdx = 0;

        Matcher argumentMatcher = ARGUMENT_PATTERN.matcher(formatString);
        while (argumentMatcher.find(lastIdx)) {
            int argumentStart = argumentMatcher.start();
            int argumentEnd = argumentMatcher.end();

            if (argumentStart > lastIdx) {
                parser.add(formatString.substring(lastIdx, argumentStart));
            }

            String index = argumentMatcher.group(1);
            String type = argumentMatcher.group(2);
            if (type.equals("s")) {
                int argumentIdx;
                if ("1".equals(index)) {
                    argumentIdx = 0;
                } else if ("2".equals(index)) {
                    argumentIdx = 1;
                } else {
                    argumentIdx = currentArgumentIdx++;
                }
                if (argumentIdx == 0) {
                    parser.markName();
                } else if (argumentIdx == 1) {
                    parser.markContent();
                } else {
                    parser.add(formatString.substring(argumentStart, argumentEnd));
                }
            }

            lastIdx = argumentEnd;
        }

        if (lastIdx < formatString.length()) {
            parser.add(formatString.substring(lastIdx));
        }

        return parser.getFormat();
    }

    public ITextComponent make(ITextComponent name, String content) {
        Builder builder = this.builder;
        for (Object format : this.format) {
            if (format == NAME_MARKER) {
                builder.pushText(name);
            } else if (format == CONTENT_MARKER) {
                builder.pushString(content);
            } else {
                builder.pushString((String) format);
            }
        }
        return builder.get();
    }

    static final class Builder {
        private final StringBuilder builder = new StringBuilder();
        private IFormattableTextComponent result;

        void pushText(ITextComponent text) {
            this.flushStringBuilder();
            this.appendText(text.copy());
        }

        void pushString(String string) {
            this.builder.append(string);
        }

        private void flushStringBuilder() {
            StringBuilder builder = this.builder;
            if (builder.length() > 0) {
                IFormattableTextComponent text = new StringTextComponent(builder.toString());
                builder.setLength(0);
                this.appendText(text);
            }
        }

        private void appendText(IFormattableTextComponent text) {
            if (this.result == null) {
                this.result = text;
            } else {
                this.result = this.result.append(text);
            }
        }

        IFormattableTextComponent get() {
            this.flushStringBuilder();

            IFormattableTextComponent result = this.result;
            this.result = null;
            this.builder.setLength(0);
            return result;
        }
    }

    static final class Parser {
        private final List<Object> format = new ArrayList<>();

        void add(String text) {
            this.format.add(text);
        }

        void markName() {
            this.format.add(NAME_MARKER);
        }

        void markContent() {
            this.format.add(CONTENT_MARKER);
        }

        Object[] getFormat() {
            return this.format.toArray(new Object[0]);
        }
    }
}
