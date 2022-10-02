package com.lovetropics.perms.store;

import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleOverrideReader;
import com.lovetropics.lib.permission.role.RoleProvider;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.override.RoleOverrideMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.stream.Stream;

public final class PlayerRoleSet implements RoleReader {
    private final Role everyoneRole;

    @Nullable
    private final ServerPlayer player;

    private final ObjectSortedSet<Role> roles = new ObjectAVLTreeSet<>();
    private final RoleOverrideMap overrides = new RoleOverrideMap();

    private boolean dirty;

    public PlayerRoleSet(Role everyoneRole, @Nullable ServerPlayer player) {
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

    public ListTag serialize() {
        ListTag list = new ListTag();
        for (Role role : this.roles) {
            list.add(StringTag.valueOf(role.id()));
        }
        return list;
    }

    public void deserialize(RoleProvider roleProvider, ListTag list) {
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
        ListTag nbt = roles.serialize();
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
