package com.lovetropics.perms;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class RoleConfiguration {
    private static final JsonParser JSON = new JsonParser();

    private static RoleConfiguration instance = new RoleConfiguration(ImmutableMap.of(), Role.empty(Role.EVERYONE));

    private final ImmutableMap<String, Role> roles;
    private final Role everyone;

    private RoleConfiguration(ImmutableMap<String, Role> roles, Role everyone) {
        this.roles = roles;
        this.everyone = everyone;
    }

    public static RoleConfiguration get() {
        return instance;
    }

    public static void setup() {
        Path path = Paths.get("roles.json");
        if (!Files.exists(path)) {
            if (!setupDefaultConfig(path)) return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject root = JSON.parse(reader).getAsJsonObject();
            instance = parse(root);
        } catch (IOException e) {
            LTPerms.LOGGER.warn("Failed to load roles.json configuration", e);
        }
    }

    private static boolean setupDefaultConfig(Path path) {
        try (InputStream input = LTPerms.class.getResourceAsStream("/data/ltperms/default_roles.json")) {
            Files.copy(input, path);
            return true;
        } catch (IOException e) {
            LTPerms.LOGGER.warn("Failed to load default roles.json configuration", e);
            return false;
        }
    }

    private static RoleConfiguration parse(JsonObject root) {
        ImmutableMap.Builder<String, Role> roles = ImmutableMap.builder();
        Role everyone = Role.empty(Role.EVERYONE);

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            JsonObject roleRoot = entry.getValue().getAsJsonObject();

            Role role = Role.parse(name, roleRoot);
            if (!name.equalsIgnoreCase(Role.EVERYONE)) {
                roles.put(name, role);
            } else {
                everyone = role;
            }
        }

        if (everyone.getLevel() != 0) throw new JsonSyntaxException("'everyone' role level must = 0");

        return new RoleConfiguration(roles.build(), everyone);
    }

    @Nullable
    public Role get(String name) {
        return this.roles.get(name);
    }

    @Nonnull
    public Role everyone() {
        return this.everyone;
    }

    public Stream<Role> stream() {
        return this.roles.values().stream();
    }
}
