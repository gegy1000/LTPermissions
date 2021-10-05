package com.lovetropics.perms.protection.authority;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.ProtectionExclusions;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;
import com.lovetropics.perms.protection.authority.shape.AuthorityShape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class UserAuthority implements Authority {
    public static final Codec<UserAuthority> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Codec.STRING.fieldOf("key").forGetter(authority -> authority.key),
                Codec.INT.fieldOf("level").forGetter(authority -> authority.level),
                AuthorityShapes.CODEC.fieldOf("shapes").forGetter(authority -> authority.shapes),
                ProtectionRuleMap.CODEC.fieldOf("rules").forGetter(authority -> authority.rules),
                ProtectionExclusions.CODEC.fieldOf("exclusions").forGetter(authority -> authority.exclusions)
        ).apply(instance, UserAuthority::new);
    });

    private final String key;
    private final int level;
    private final AuthorityShapes shapes;
    private final ProtectionRuleMap rules;
    private final ProtectionExclusions exclusions;

    private final EventFilter filterWithExclusions;

    UserAuthority(String key, int level, AuthorityShapes shapes, ProtectionRuleMap rules, ProtectionExclusions exclusions) {
        this.key = key;
        this.level = level;
        this.shapes = shapes;
        this.rules = rules;
        this.exclusions = exclusions;

        this.filterWithExclusions = EventFilter.and(shapes, exclusions);
    }

    UserAuthority(String key, int level, AuthorityShapes shapes) {
        this(key, level, shapes, new ProtectionRuleMap(), new ProtectionExclusions());
    }

    public static UserAuthority create(String key) {
        return new UserAuthority(key, 0, new AuthorityShapes());
    }

    @Override
    public UserAuthority withRule(ProtectionRule rule, PermissionResult result) {
        return new UserAuthority(this.key, this.level, this.shapes, this.rules.with(rule, result), this.exclusions.copy());
    }

    public UserAuthority withLevel(int level) {
        return new UserAuthority(this.key, level, this.shapes, this.rules, this.exclusions.copy());
    }

    public UserAuthority addShape(String key, AuthorityShape shape) {
        return this.withShape(this.shapes.addShape(key, shape));
    }

    public UserAuthority removeShape(String key) {
        return this.withShape(this.shapes.removeShape(key));
    }

    private UserAuthority withShape(AuthorityShapes shape) {
        if (this.shapes == shape) return this;
        return new UserAuthority(this.key, this.level, shape, this.rules, this.exclusions.copy());
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

    public AuthorityShapes shape() {
        return this.shapes;
    }

    public ProtectionExclusions exclusions() {
        return this.exclusions;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
