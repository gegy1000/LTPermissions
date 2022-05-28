package com.lovetropics.perms.protection.authority;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.ProtectionExclusions;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;
import com.lovetropics.perms.protection.authority.behavior.AuthorityBehaviorMap;
import com.lovetropics.perms.role.Role;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;

public interface Authority extends Comparable<Authority> {
    String key();

    int level();

    ProtectionRuleMap rules();

    EventFilter eventFilter();

    ProtectionExclusions exclusions();

    AuthorityBehaviorMap behavior();

    default boolean hasBehavior() {
        return !this.behavior().isEmpty();
    }

    Authority withRule(ProtectionRule rule, PermissionResult result);

    Authority withExclusions(ProtectionExclusions exclusions);

    Authority withBehavior(AuthorityBehaviorMap behavior);

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

    default Authority excludeOperators(boolean operators) {
        return this.withExclusions(this.exclusions().withOperators(operators));
    }

    default Authority addBehavior(ResourceLocation id) {
        return this.withBehavior(this.behavior().addBehavior(id));
    }

    default Authority removeBehavior(ResourceLocation id) {
        return this.withBehavior(this.behavior().removeBehavior(id));
    }

    default boolean isEmpty() {
        return this.rules().isEmpty() && this.exclusions().isEmpty() && this.behavior().isEmpty();
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
