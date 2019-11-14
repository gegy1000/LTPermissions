package com.lovetropics.perms.modifier;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.lovetropics.perms.modifier.command.CommandPermModifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class RoleModifierType<T extends RoleModifier> {
    private static final Map<String, RoleModifierType<?>> REGISTRY = new HashMap<>();

    public static final RoleModifierType<CommandPermModifier> COMMANDS = RoleModifierType.<CommandPermModifier>builder()
            .key("commands")
            .parse(element -> CommandPermModifier.parse(element.getAsJsonObject()))
            .register();

    public static final RoleModifierType<ChatStyleModifier> CHAT_STYLE = RoleModifierType.<ChatStyleModifier>builder()
            .key("chat_style")
            .parse(element -> new ChatStyleModifier(element.getAsString()))
            .register();

    private final String key;
    private final Function<JsonElement, T> parse;

    private RoleModifierType(String key, Function<JsonElement, T> parse) {
        this.key = key;
        this.parse = parse;
    }

    public static <T extends RoleModifier> Builder<T> builder() {
        return new Builder<>();
    }

    public String getKey() {
        return this.key;
    }

    public T parse(JsonElement element) {
        return this.parse.apply(element);
    }

    @Nullable
    public static RoleModifierType<?> byKey(String key) {
        return REGISTRY.get(key);
    }

    public static class Builder<T extends RoleModifier> {
        private String key;
        private Function<JsonElement, T> parse;

        private Builder() {
        }

        public Builder<T> key(String key) {
            this.key = key;
            return this;
        }

        public Builder<T> parse(Function<JsonElement, T> deserialize) {
            this.parse = deserialize;
            return this;
        }

        public RoleModifierType<T> register() {
            Preconditions.checkNotNull(this.key, "key not set");
            Preconditions.checkNotNull(this.parse, "parser not set");

            Preconditions.checkState(!REGISTRY.containsKey(this.key), "modifier with key already exists");

            RoleModifierType<T> modifierType = new RoleModifierType<>(this.key, this.parse);
            REGISTRY.put(this.key, modifierType);

            return modifierType;
        }
    }
}
