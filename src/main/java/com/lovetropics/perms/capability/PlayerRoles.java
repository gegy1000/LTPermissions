package com.lovetropics.perms.capability;

import com.lovetropics.perms.LTPerms;
import com.lovetropics.perms.Role;
import com.lovetropics.perms.RoleConfiguration;
import com.lovetropics.perms.override.RoleOverride;
import com.lovetropics.perms.override.RoleOverrideType;
import com.lovetropics.perms.override.command.PermissionResult;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

public final class PlayerRoles implements ICapabilitySerializable<ListNBT> {
    private final ServerPlayerEntity player;

    private TreeSet<String> roleIds = new TreeSet<>((n1, n2) -> {
        RoleConfiguration config = RoleConfiguration.get();
        Role r1 = config.get(n1);
        Role r2 = config.get(n2);
        if (r1 == null || r2 == null) return 0;
        return r1.compareTo(r2);
    });

    public PlayerRoles(ServerPlayerEntity player) {
        this.player = player;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return LTPerms.playerRolesCap().orEmpty(cap, LazyOptional.of(() -> this));
    }

    public void notifyReload() {
        this.removeInvalidRoles();
        this.roles().forEach(role -> role.notifyChange(this.player));
    }

    public boolean add(Role role) {
        if (this.roleIds.add(role.getName())) {
            role.notifyChange(this.player);
            return true;
        }
        return false;
    }

    public boolean remove(Role role) {
        if (this.roleIds.remove(role.getName())) {
            role.notifyChange(this.player);
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

    @Override
    public ListNBT serializeNBT() {
        ListNBT list = new ListNBT();
        for (String role : this.roleIds) {
            list.add(new StringNBT(role));
        }
        return list;
    }

    @Override
    public void deserializeNBT(ListNBT list) {
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
}
