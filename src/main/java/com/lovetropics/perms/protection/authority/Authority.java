package com.lovetropics.perms.protection.authority;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.ProtectionExclusions;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;
import com.lovetropics.perms.role.Role;
import com.mojang.authlib.GameProfile;

public interface Authority extends Comparable<Authority> {
    String key();

    int level();

    ProtectionRuleMap rules();

    EventFilter eventFilter();

    ProtectionExclusions exclusions();

    Authority withRule(ProtectionRule rule, PermissionResult result);

    Authority withExclusions(ProtectionExclusions exclusions);

    default Authority addExclusion(GameProfile player) {
        return this.withExclusions(this.exclusions().addPlayer(player));
    }

    default Authority addExclusion(Role role) {
        return this.withExclusions(this.exclusions().addRole(role));
    }

    default Authority removeExclusion(GameProfile player) {
        return this.withExclusions(this.exclusions().removePlayer(player));
    }

    default Authority removeExclusion(Role role) {
        return this.withExclusions(this.exclusions().removeRole(role));
    }

    @Override
    default int compareTo(Authority other) {
        int levelCompare = Integer.compare(other.level(), this.level());
        if (levelCompare != 0) {
            return levelCompare;
        } else {
            return this.key().compareTo(other.key());
        }
    }
}
