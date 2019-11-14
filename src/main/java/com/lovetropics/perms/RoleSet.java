package com.lovetropics.perms;

import com.lovetropics.perms.modifier.RoleModifier;
import com.lovetropics.perms.modifier.RoleModifierType;
import com.lovetropics.perms.modifier.command.PermissionResult;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.function.Function;

public final class RoleSet implements Iterable<Role> {
    public static final RoleSet EMPTY = new RoleSet();

    private final TreeSet<Role> roles;

    public RoleSet() {
        this.roles = new TreeSet<>();
    }

    public RoleSet(Collection<Role> roles) {
        this.roles = new TreeSet<>(roles);
    }

    public boolean add(Role role) {
        return this.roles.add(role);
    }

    public boolean remove(Role role) {
        return this.roles.remove(role);
    }

    public <T extends RoleModifier> PermissionResult test(RoleModifierType<T> type, Function<T, PermissionResult> function) {
        for (Role role : this.roles) {
            T modifier = role.getModifier(type);
            if (modifier != null) {
                PermissionResult result = function.apply(modifier);
                if (result.isDefinitive()) return result;
            }
        }
        return PermissionResult.PASS;
    }

    @Nullable
    public <T extends RoleModifier> T getTop(RoleModifierType<T> type) {
        for (Role role : this.roles) {
            T modifier = role.getModifier(type);
            if (modifier != null) return modifier;
        }
        return null;
    }

    public ListNBT serialize() {
        ListNBT list = new ListNBT();
        for (Role role : this.roles) {
            list.add(new StringNBT(role.getName()));
        }
        return list;
    }

    public boolean isEmpty() {
        return this.roles.isEmpty();
    }

    @Override
    public Iterator<Role> iterator() {
        return this.roles.iterator();
    }

    public Collection<Role> asCollection() {
        return Collections.unmodifiableCollection(this.roles);
    }
}
