package com.lovetropics.perms.protection.authority;

import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;

public interface Authority extends Comparable<Authority> {
    String key();

    int level();

    ProtectionRuleMap rules();

    EventFilter eventFilter();

    Authority withRule(ProtectionRule rule, PermissionResult result);

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
