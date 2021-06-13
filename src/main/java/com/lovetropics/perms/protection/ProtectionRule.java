package com.lovetropics.perms.protection;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public enum ProtectionRule {
    BREAK("break"),
    PLACE("place"),
    INTERACT("interact"),
    INTERACT_ENTITIES("interact_entities"),
    ATTACK("attack"),
    HUNGER("hunger");

    private static final Map<String, ProtectionRule> BY_KEY = new Object2ObjectOpenHashMap<>();

    private final String key;

    ProtectionRule(String key) {
        this.key = key;
    }

    @Nullable
    public static ProtectionRule byKey(String key) {
        return BY_KEY.get(key);
    }

    public String getKey() {
        return this.key;
    }

    public static Collection<String> keys() {
        return BY_KEY.keySet();
    }

    static {
        for (ProtectionRule rule : values()) {
            BY_KEY.put(rule.key, rule);
        }
    }
}
