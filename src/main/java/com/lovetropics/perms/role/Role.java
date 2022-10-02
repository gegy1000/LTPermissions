package com.lovetropics.perms.role;

import com.lovetropics.perms.override.RoleOverrideMap;

public record Role(String id, RoleOverrideMap overrides, int index) implements Comparable<Role> {
    public static final String EVERYONE = "everyone";

    public static Role empty(String id) {
        return new Role(id, new RoleOverrideMap(), 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return obj instanceof Role role && this.index == role.index && role.id.equalsIgnoreCase(this.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "\"" + this.id + "\" (" + this.index + ")";
    }

    @Override
    public int compareTo(Role role) {
        int compareIndex = Integer.compare(role.index(), this.index());
        return compareIndex != 0 ? compareIndex : this.id().compareTo(role.id());
    }
}
