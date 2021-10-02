package com.lovetropics.perms.config;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.perms.override.RoleOverrideMap;
import com.lovetropics.perms.role.Role;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class RoleConfig {
    public static final Codec<RoleConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("level", 0).forGetter(c -> c.level),
                RoleOverrideMap.CODEC.fieldOf("overrides").orElseGet(RoleOverrideMap::new).forGetter(c -> c.overrides),
                MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("includes", new String[0]).forGetter(c -> c.includes)
        ).apply(instance, RoleConfig::new);
    });

    public final int level;
    public final RoleOverrideMap overrides;
    public final String[] includes;

    public RoleConfig(int level, RoleOverrideMap overrides, String[] includes) {
        this.level = level;
        this.overrides = overrides;
        this.includes = includes;
    }

    public Role create(String name, int index) {
        return new Role(name, this.overrides, index);
    }
}
