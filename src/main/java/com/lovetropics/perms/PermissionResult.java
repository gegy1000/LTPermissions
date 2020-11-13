package com.lovetropics.perms;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public enum PermissionResult {
    PASS,
    ALLOW,
    DENY;

    private static final String[] KEYS = { "pass", "allow", "deny" };

    public boolean isDefinitive() {
        return this == ALLOW || this == DENY;
    }

    public static PermissionResult byName(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "allow":
            case "yes":
                return PermissionResult.ALLOW;
            case "deny":
            case "no":
                return PermissionResult.DENY;
            case "pass":
            default:
                return PermissionResult.PASS;
        }
    }

    public String getName() {
        switch (this) {
            case PASS: return "pass";
            case DENY: return "deny";
            case ALLOW: return "allow";
            default: return "pass";
        }
    }

    public static Stream<String> keysStream() {
        return Arrays.stream(KEYS);
    }
}
