package com.lovetropics.perms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.perms.modifier.RoleModifier;
import com.lovetropics.perms.modifier.RoleModifierType;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

// TODO: Default roles? How should that be handled?
public final class Role implements Comparable<Role> {
    private final String name;
    private int level;

    private final Map<RoleModifierType<?>, RoleModifier> modifiers = new HashMap<>();

    Role(String name) {
        this.name = name;
    }

    public static Role parse(String name, JsonObject root) {
        Role role = new Role(name);

        role.level = JSONUtils.getInt(root, "level", 0);

        if (root.has("modifiers")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("modifiers").entrySet()) {
                RoleModifierType<?> modifierType = RoleModifierType.byKey(entry.getKey());
                if (modifierType != null) {
                    JsonElement element = entry.getValue();
                    role.modifiers.put(modifierType, modifierType.parse(element));
                } else {
                    LTPerms.LOGGER.warn("Encountered invalid modifier type: '{}'", entry.getKey());
                }
            }
        }

        return role;
    }

    public String getName() {
        return this.name;
    }

    public int getLevel() {
        return this.level;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends RoleModifier> T getModifier(RoleModifierType<T> type) {
        return (T) this.modifiers.get(type);
    }

    @Override
    public int compareTo(Role role) {
        return Integer.compare(role.level, this.level);
    }

    @Override
    public String toString() {
        return "\"" + this.name + "\" (" + this.level + ")";
    }
}
