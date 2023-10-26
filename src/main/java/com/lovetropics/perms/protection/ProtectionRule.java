package com.lovetropics.perms.protection;

import com.lovetropics.lib.codec.CodecRegistry;
import com.mojang.serialization.Codec;

import javax.annotation.Nullable;
import java.util.Set;

public final class ProtectionRule {
    public static final CodecRegistry<String, ProtectionRule> REGISTRY = CodecRegistry.stringKeys();

    public static final Codec<ProtectionRule> CODEC = REGISTRY;

    public static final ProtectionRule BREAK = register("break");
    public static final ProtectionRule PLACE = register("place");

    public static final ProtectionRule INTERACT_BLOCKS = register("interact_blocks");
    public static final ProtectionRule INTERACT_ENTITIES = register("interact_entities");
    public static final ProtectionRule INTERACT_ITEMS = register("interact_items");
    public static final ProtectionRule INTERACT = register("interact");
    public static final ProtectionRule MODIFY = register("modify");
    public static final ProtectionRule MODIFY_ITEM_FRAMES = register("modify_item_frames");
    public static final ProtectionRule MODIFY_ARMOR_STANDS = register("modify_armor_stands");
    public static final ProtectionRule MODIFY_LECTERNS = register("modify_lecterns");
    public static final ProtectionRule MODIFY_SIGNS = register("modify_signs");

    public static final ProtectionRule ATTACK = register("attack");
    public static final ProtectionRule PVP = register("pvp");

    public static final ProtectionRule PORTALS = register("portals");
    public static final ProtectionRule HUNGER = register("hunger");
    public static final ProtectionRule FALL_DAMAGE = register("fall_damage");
    public static final ProtectionRule DAMAGE = register("damage");
    public static final ProtectionRule PLAYER_DAMAGE = register("player_damage");

    private final String key;

    ProtectionRule(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Nullable
    public static ProtectionRule byKey(String key) {
        return REGISTRY.get(key);
    }

    public static Set<String> keySet() {
        return REGISTRY.keySet();
    }

    public static ProtectionRule register(String key) {
        ProtectionRule rule = new ProtectionRule(key);
        REGISTRY.register(key, rule);
        return rule;
    }
}
