package com.lovetropics.perms.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.role.Role;
import com.lovetropics.perms.role.RoleProvider;
import com.lovetropics.perms.role.SimpleRole;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class RolesConfig implements RoleProvider {
    private static RolesConfig instance = new RolesConfig(Collections.emptyList(), SimpleRole.empty(Role.EVERYONE));

    private final ImmutableMap<String, Role> roles;
    private final Role everyone;

    private RolesConfig(List<Role> roles, Role everyone) {
        ImmutableMap.Builder<String, Role> roleMap = ImmutableMap.builder();
        for (Role role : roles) {
            roleMap.put(role.id(), role);
        }
        this.roles = roleMap.build();

        this.everyone = everyone;
    }

    public static RolesConfig get() {
        return instance;
    }

    public static List<String> setup() {
        Path path = Paths.get("config/roles.json");
        if (!Files.exists(path)) {
            if (!createDefaultConfig(path)) {
                return ImmutableList.of();
            }
        }

        List<String> errors = new ArrayList<>();
        ConfigErrorConsumer errorConsumer = errors::add;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonElement root = JsonParser.parseReader(reader);
            instance = parse(new Dynamic<>(JsonOps.INSTANCE, root), errorConsumer);
        } catch (IOException e) {
            errorConsumer.report("Failed to read roles.json configuration", e);
            LTPermissions.LOGGER.warn("Failed to load roles.json configuration", e);
        } catch (JsonSyntaxException e) {
            errorConsumer.report("Malformed syntax in roles.json configuration", e);
            LTPermissions.LOGGER.warn("Malformed syntax in roles.json configuration", e);
        }

        return errors;
    }

    private static boolean createDefaultConfig(Path path) {
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            try (InputStream input = LTPermissions.class.getResourceAsStream("/data/ltperms/default_roles.json")) {
                Files.copy(input, path);
                return true;
            }
        } catch (IOException e) {
            LTPermissions.LOGGER.warn("Failed to load default roles.json configuration", e);
            return false;
        }
    }

    private static <T> RolesConfig parse(Dynamic<T> root, ConfigErrorConsumer error) {
        RoleConfigMap roleConfigs = RoleConfigMap.parse(root, error);

        Role everyone = SimpleRole.empty(Role.EVERYONE);
        List<Role> roles = new ArrayList<>();

        int index = 1;
        for (Pair<String, RoleConfig> entry : roleConfigs) {
            String name = entry.getFirst();
            RoleConfig roleConfig = entry.getSecond();

            if (!name.equalsIgnoreCase(Role.EVERYONE)) {
                roles.add(roleConfig.create(name, index++));
            } else {
                everyone = roleConfig.create(name, 0);
            }
        }

        return new RolesConfig(roles, everyone);
    }

    @Override
    @Nullable
    public Role get(String name) {
        return this.roles.get(name);
    }

    @Nonnull
    public Role everyone() {
        return this.everyone;
    }

    @Nonnull
    @Override
    public Iterator<Role> iterator() {
        return this.roles.values().iterator();
    }

    @Override
    public Stream<Role> stream() {
        return this.roles.values().stream();
    }
}
