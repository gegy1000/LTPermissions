package com.lovetropics.perms.protection.authority;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.ProtectionExclusions;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;
import com.lovetropics.perms.protection.EventFilter;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

public final class BuiltinAuthority implements Authority {
    public static final int UNIVERSE_LEVEL = Integer.MIN_VALUE;
    public static final int DIMENSION_LEVEL = Integer.MIN_VALUE + 1;

    private final String key;
    private final int level;
    private final EventFilter filter;
    private final ProtectionRuleMap rules;
    private final ProtectionExclusions exclusions;

    private final EventFilter filterWithExclusions;

    private BuiltinAuthority(String key, int level, EventFilter filter, ProtectionRuleMap rules, ProtectionExclusions exclusions) {
        this.key = key;
        this.level = level;
        this.filter = filter;
        this.rules = rules;
        this.exclusions = exclusions;

        this.filterWithExclusions = EventFilter.and(filter, exclusions);
    }

    private BuiltinAuthority(String key, int level, EventFilter filter) {
        this(key, level, filter, new ProtectionRuleMap(), new ProtectionExclusions());
    }

    public static BuiltinAuthority universe() {
        return new BuiltinAuthority("universe", UNIVERSE_LEVEL, EventFilter.universe());
    }

    public static BuiltinAuthority dimension(RegistryKey<World> dimension) {
        String key = dimension.getLocation().getPath();
        return new BuiltinAuthority(key, DIMENSION_LEVEL, EventFilter.dimension(dimension));
    }

    @Override
    public BuiltinAuthority withRule(ProtectionRule rule, PermissionResult result) {
        return new BuiltinAuthority(this.key, this.level, this.filter, this.rules.with(rule, result), this.exclusions.copy());
    }

    @Override
    public String key() {
        return this.key;
    }

    @Override
    public int level() {
        return this.level;
    }

    @Override
    public ProtectionRuleMap rules() {
        return this.rules;
    }

    @Override
    public EventFilter eventFilter() {
        return this.filterWithExclusions;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
