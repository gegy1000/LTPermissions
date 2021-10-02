package com.lovetropics.perms;

import com.mojang.serialization.Codec;
import net.minecraft.util.IStringSerializable;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public enum PermissionResult implements IStringSerializable {
    PASS,
    ALLOW,
    DENY;

    public static final Codec<PermissionResult> CODEC = IStringSerializable.createEnumCodec(PermissionResult::values, PermissionResult::byName);

    private static final String[] KEYS = { "pass", "allow", "deny" };

    public boolean isDefinitive() {
        return this != PASS;
    }

    public boolean isAllowed() {
        return this == ALLOW;
    }

    public boolean isDenied() {
        return this == DENY;
    }

    public static PermissionResult byName(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "allow":
            case "yes":
            case "true": return PermissionResult.ALLOW;
            case "deny":
            case "no":
            case "false": return PermissionResult.DENY;
            default: return PermissionResult.PASS;
        }
    }

    @Override
    public String getString() {
        switch (this) {
            case ALLOW: return "allow";
            case DENY: return "deny";
            default: return "pass";
        }
    }

    public static Stream<String> keysStream() {
        return Arrays.stream(KEYS);
    }
}
