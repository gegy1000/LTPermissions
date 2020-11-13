package com.lovetropics.perms.storage;

import com.lovetropics.perms.LTPerms;
import com.lovetropics.perms.Role;
import com.lovetropics.perms.RoleConfiguration;
import com.lovetropics.perms.override.RoleOverride;
import com.lovetropics.perms.override.RoleOverrideType;
import com.lovetropics.perms.PermissionResult;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

public final class PlayerRoles {
    private final MinecraftServer server;
    private final UUID player;

    private final TreeSet<String> roleIds = new TreeSet<>((n1, n2) -> {
        RoleConfiguration config = RoleConfiguration.get();
        Role r1 = config.get(n1);
        Role r2 = config.get(n2);
        if (r1 == null || r2 == null) return 0;
        return r1.compareTo(r2);
    });

    public PlayerRoles(MinecraftServer server, UUID player) {
        this.server = server;
        this.player = player;
    }

    public void notifyReload() {
        this.removeInvalidRoles();
        this.roles().forEach(role -> role.notifyChange(this.server, this.player));
    }

    public boolean add(Role role) {
        if (this.roleIds.add(role.getName())) {
            role.notifyChange(this.server, this.player);
            return true;
        }
        return false;
    }

    public boolean remove(Role role) {
        if (this.roleIds.remove(role.getName())) {
            role.notifyChange(this.server, this.player);
            return true;
        }
        return false;
    }

    public Stream<Role> roles() {
        RoleConfiguration roleConfig = RoleConfiguration.get();
        return Stream.concat(
                this.roleIds.stream().map(roleConfig::get).filter(Objects::nonNull),
                Stream.of(roleConfig.everyone())
        );
    }

    public <T extends RoleOverride> Stream<T> overrides(RoleOverrideType<T> type) {
        return this.roles().map(role -> role.getOverride(type)).filter(Objects::nonNull);
    }

    public <T extends RoleOverride> PermissionResult test(RoleOverrideType<T> type, Function<T, PermissionResult> function) {
        return this.overrides(type).map(function)
                .filter(PermissionResult::isDefinitive)
                .findFirst().orElse(PermissionResult.PASS);
    }

    @Nullable
    public <T extends RoleOverride> T getHighest(RoleOverrideType<T> type) {
        return this.overrides(type).findFirst().orElse(null);
    }

    public ListNBT serialize() {
        ListNBT list = new ListNBT();
        for (String role : this.roleIds) {
            list.add(StringNBT.valueOf(role));
        }
        return list;
    }

    public void deserialize(ListNBT list) {
        this.roleIds.clear();
        for (int i = 0; i < list.size(); i++) {
            this.roleIds.add(list.getString(i));
        }

        this.removeInvalidRoles();
    }

    private void removeInvalidRoles() {
        this.roleIds.removeIf(name -> {
            Role role = RoleConfiguration.get().get(name);
            if (role == null || role.getName().equalsIgnoreCase(Role.EVERYONE)) {
                LTPerms.LOGGER.warn("Encountered invalid role '{}'", name);
                return true;
            }
            return false;
        });
    }

    public void copyFrom(PlayerRoles old) {
        this.deserialize(old.serialize());
    }
}
