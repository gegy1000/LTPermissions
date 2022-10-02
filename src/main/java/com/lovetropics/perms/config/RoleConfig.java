package com.lovetropics.perms.config;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.perms.override.RoleOverrideMap;
import com.lovetropics.perms.role.SimpleRole;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RoleConfig(RoleOverrideMap overrides, String[] includes) {
    public static final Codec<RoleConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            RoleOverrideMap.CODEC.fieldOf("overrides").orElseGet(RoleOverrideMap::new).forGetter(c -> c.overrides),
            MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("includes", new String[0]).forGetter(c -> c.includes)
    ).apply(i, RoleConfig::new));

    public Role create(String name, int index) {
        return new SimpleRole(name, this.overrides, index);
    }
}
