package com.lovetropics.perms.capability;

import com.google.common.collect.Iterables;
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
import java.util.Collections;
import java.util.TreeSet;
import java.util.function.Function;

public final class PlayerRoles implements ICapabilitySerializable<ListNBT> {
    private final ServerPlayerEntity player;

    // TODO: These should be references so that when the roles reload it updates
    //  also on reload we need to notifyChange on everything

    private TreeSet<Role> roles = new TreeSet<>();
    private Role everyone = Role.empty(Role.EVERYONE);

    public PlayerRoles(ServerPlayerEntity player) {
        this.player = player;
        // TODO: this duplication we need to fix
        this.everyone = RoleConfiguration.get().everyone();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return LTPerms.playerRolesCap().orEmpty(cap, LazyOptional.of(() -> this));
    }

    public boolean add(Role role) {
        if (this.roles.add(role)) {
            role.notifyChange(this.player);
            return true;
        }
        return false;
    }

    public boolean remove(Role role) {
        if (this.roles.remove(role)) {
            role.notifyChange(this.player);
            return true;
        }
        return false;
    }

    public <T extends RoleOverride> PermissionResult test(RoleOverrideType<T> type, Function<T, PermissionResult> function) {
        for (Role role : this.asIterable()) {
            T override = role.getOverride(type);
            if (override != null) {
                PermissionResult result = function.apply(override);
                if (result.isDefinitive()) return result;
            }
        }
        return PermissionResult.PASS;
    }

    @Nullable
    public <T extends RoleOverride> T getHighest(RoleOverrideType<T> type) {
        for (Role role : this.asIterable()) {
            T override = role.getOverride(type);
            if (override != null) return override;
        }
        return null;
    }

    public Iterable<Role> asIterable() {
        return Iterables.concat(this.roles, Collections.singleton(this.everyone));
    }

    @Override
    public ListNBT serializeNBT() {
        ListNBT list = new ListNBT();
        for (Role role : this.roles) {
            list.add(new StringNBT(role.getName()));
        }
        return list;
    }

    @Override
    public void deserializeNBT(ListNBT list) {
        RoleConfiguration roleConfig = RoleConfiguration.get();

        this.roles.clear();
        this.everyone = roleConfig.everyone();

        for (int i = 0; i < list.size(); i++) {
            String name = list.getString(i);
            Role role = roleConfig.get(name);

            if (role == null || role.getName().equalsIgnoreCase(Role.EVERYONE)) {
                LTPerms.LOGGER.warn("Encountered invalid role '{}' in nbt", name);
                continue;
            }

            this.roles.add(role);
        }
    }
}
