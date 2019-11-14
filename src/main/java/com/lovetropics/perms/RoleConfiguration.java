package com.lovetropics.perms;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.ListNBT;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public final class RoleConfiguration {
    private static final JsonParser JSON = new JsonParser();

    private static RoleConfiguration instance = new RoleConfiguration(ImmutableMap.of());

    private final ImmutableMap<String, Role> roles;

    private RoleConfiguration(ImmutableMap<String, Role> roles) {
        this.roles = roles;
    }

    public static RoleConfiguration get() {
        return instance;
    }

    public static void setup() {
        Path path = Paths.get("roles.json");
        if (!Files.exists(path)) return;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject root = JSON.parse(reader).getAsJsonObject();
            instance = parse(root);
        } catch (IOException e) {
            LTPerms.LOGGER.warn("Failed to load roles.json configuration", e);
        }
    }

    private static RoleConfiguration parse(JsonObject root) {
        ImmutableMap.Builder<String, Role> roles = ImmutableMap.builder();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String name = entry.getKey();
            JsonObject roleRoot = entry.getValue().getAsJsonObject();
            roles.put(name, Role.parse(name, roleRoot));
        }

        return new RoleConfiguration(roles.build());
    }

    @Nullable
    public Role get(String name) {
        return this.roles.get(name);
    }

    public Stream<Role> stream() {
        return this.roles.values().stream();
    }

    public RoleSet readSet(ListNBT list) {
        Collection<Role> roles = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String name = list.getString(i);
            Role role = this.roles.get(name);
            if (role != null) {
                roles.add(role);
            } else {
                LTPerms.LOGGER.warn("Encountered invalid role '{}' in nbt", name);
            }
        }
        return new RoleSet(roles);
    }
}
