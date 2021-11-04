package com.lovetropics.perms.store;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.override.RoleOverrideMap;
import com.lovetropics.perms.override.RoleOverrideReader;
import com.lovetropics.perms.role.Role;
import com.lovetropics.perms.role.RoleProvider;
import com.lovetropics.perms.role.RoleReader;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.stream.Stream;

public final class PlayerRoleSet implements RoleReader {
    private final Role everyoneRole;

    @Nullable
    private final ServerPlayerEntity player;

    private final ObjectSortedSet<Role> roles = new ObjectAVLTreeSet<>();
    private final RoleOverrideMap overrides = new RoleOverrideMap();

    private boolean dirty;

    public PlayerRoleSet(Role everyoneRole, @Nullable ServerPlayerEntity player) {
        this.everyoneRole = everyoneRole;
        this.player = player;

        this.rebuildOverridesAndInitialize();
    }

    public void rebuildOverridesAndInitialize() {
        this.rebuildOverrides();
        if (this.player != null) {
            this.overrides.notifyInitialize(this.player);
        }
    }

    public void rebuildOverridesAndNotify() {
        this.rebuildOverrides();
        if (this.player != null) {
            this.overrides.notifyChange(this.player);
        }
    }

    private void rebuildOverrides() {
        this.overrides.clear();
        this.stream().forEach(role -> this.overrides.addAll(role.overrides()));
    }

    public boolean add(Role role) {
        if (this.roles.add(role)) {
            this.dirty = true;
            this.rebuildOverridesAndNotify();
            return true;
        }

        return false;
    }

    public boolean remove(Role role) {
        if (this.roles.remove(role)) {
            this.dirty = true;
            this.rebuildOverridesAndNotify();
            return true;
        }

        return false;
    }

    @Override
    public Iterator<Role> iterator() {
        return this.roles.iterator();
    }

    @Override
    public Stream<Role> stream() {
        return Stream.concat(
                this.roles.stream(),
                Stream.of(this.everyoneRole)
        );
    }

    @Override
    public boolean has(Role role) {
        return role == this.everyoneRole || this.roles.contains(role);
    }

    @Override
    public RoleOverrideReader overrides() {
        return this.overrides;
    }

    public ListNBT serialize() {
        ListNBT list = new ListNBT();
        for (Role role : this.roles) {
            list.add(StringNBT.valueOf(role.id()));
        }
        return list;
    }

    public void deserialize(RoleProvider roleProvider, ListNBT list) {
        this.roles.clear();

        for (int i = 0; i < list.size(); i++) {
            String name = list.getString(i);
            Role role = roleProvider.get(name);
            if (role == null || name.equalsIgnoreCase(Role.EVERYONE)) {
                this.dirty = true;
                LTPermissions.LOGGER.warn("Encountered invalid role '{}'", name);
                continue;
            }

            this.roles.add(role);
        }

        this.rebuildOverrides();
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isEmpty() {
        return this.roles.isEmpty();
    }

    public void reloadFrom(RoleProvider roleProvider, PlayerRoleSet roles) {
        ListNBT nbt = roles.serialize();
        this.deserialize(roleProvider, nbt);

        this.dirty |= roles.dirty;
    }

    public void copyFrom(PlayerRoleSet roles) {
        this.roles.clear();
        this.roles.addAll(roles.roles);
        this.dirty = roles.dirty;

        this.rebuildOverrides();
    }

    public PlayerRoleSet copy() {
        PlayerRoleSet copy = new PlayerRoleSet(this.everyoneRole, this.player);
        copy.copyFrom(this);
        return copy;
    }
}
