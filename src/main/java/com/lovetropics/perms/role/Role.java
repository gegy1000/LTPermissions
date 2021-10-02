package com.lovetropics.perms.role;

import com.lovetropics.perms.override.RoleOverrideMap;

public final class Role implements Comparable<Role> {
    public static final String EVERYONE = "everyone";

    private final String id;
    private final RoleOverrideMap overrides;
    private final int index;

    public Role(String id, RoleOverrideMap overrides, int index) {
        this.id = id;
        this.overrides = overrides;
        this.index = index;
    }

    public static Role empty(String id) {
        return new Role(id, new RoleOverrideMap(), 0);
    }

    public String id() {
        return this.id;
    }

    public int index() {
        return this.index;
    }

    public RoleOverrideMap overrides() {
        return this.overrides;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof Role) {
            Role role = (Role) obj;
            return this.index == role.index && role.id.equalsIgnoreCase(this.id);
        }

        return false;
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
