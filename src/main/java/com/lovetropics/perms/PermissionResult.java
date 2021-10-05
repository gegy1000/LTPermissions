package com.lovetropics.perms;

import com.mojang.serialization.Codec;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public enum PermissionResult implements IStringSerializable {
    PASS("pass", TextFormatting.AQUA),
    ALLOW("allow", TextFormatting.GREEN),
    DENY("deny", TextFormatting.RED);

    public static final Codec<PermissionResult> CODEC = IStringSerializable.createEnumCodec(PermissionResult::values, PermissionResult::byKey);

    private static final String[] KEYS = { "pass", "allow", "deny" };

    private final String key;
    private final ITextComponent name;

    PermissionResult(String key, TextFormatting color) {
        this.key = key;
        this.name = new StringTextComponent(key).mergeStyle(color);
    }

    public boolean isTerminator() {
        return this != PASS;
    }

    public boolean isAllowed() {
        return this == ALLOW;
    }

    public boolean isDenied() {
        return this == DENY;
    }

    public static PermissionResult byKey(String key) {
        switch (key.toLowerCase(Locale.ROOT)) {
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
        return this.key;
    }

    public ITextComponent getName() {
        return this.name;
    }

    public static Stream<String> keysStream() {
        return Arrays.stream(KEYS);
    }
}
