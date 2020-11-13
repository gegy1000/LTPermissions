package com.lovetropics.perms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.perms.override.RoleOverride;
import com.lovetropics.perms.override.RoleOverrideType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Role implements Comparable<Role> {
    public static final String EVERYONE = "everyone";

    private final String name;
    private int level;

    private final Map<RoleOverrideType<?>, RoleOverride> overrides = new HashMap<>();

    Role(String name) {
        this.name = name;
    }

    public static Role empty(String name) {
        return new Role(name);
    }

    public static Role parse(String name, JsonObject root) {
        Role role = new Role(name);

        role.level = JSONUtils.getInt(root, "level", 0);

        if (root.has("overrides")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("overrides").entrySet()) {
                RoleOverrideType<?> overrideType = RoleOverrideType.byKey(entry.getKey());
                if (overrideType != null) {
                    JsonElement element = entry.getValue();
                    role.overrides.put(overrideType, overrideType.parse(element));
                } else {
                    LTPerms.LOGGER.warn("Encountered invalid override type: '{}'", entry.getKey());
                }
            }
        }

        return role;
    }

    public void notifyChange(MinecraftServer server, UUID player) {
        for (RoleOverride override : this.overrides.values()) {
            override.notifyChange(server, player);
        }
    }

    public String getName() {
        return this.name;
    }

    public int getLevel() {
        return this.level;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends RoleOverride> T getOverride(RoleOverrideType<T> type) {
        return (T) this.overrides.get(type);
    }

    @Override
    public int compareTo(Role role) {
        return Integer.compare(role.level, this.level);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof Role) {
            Role role = (Role) obj;
            return role.name.equalsIgnoreCase(this.name);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "\"" + this.name + "\" (" + this.level + ")";
    }
}
