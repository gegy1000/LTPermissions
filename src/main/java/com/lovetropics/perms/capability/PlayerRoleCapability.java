package com.lovetropics.perms.capability;

import com.lovetropics.perms.LTPerms;
import com.lovetropics.perms.RoleConfiguration;
import com.lovetropics.perms.RoleSet;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerRoleCapability implements ICapabilitySerializable<CompoundNBT> {
    // TODO: These should be references so that when the roles reload it updates
    private RoleSet roleSet = new RoleSet();

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return LTPerms.playerRoleCap().orEmpty(cap, LazyOptional.of(() -> this));
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT compound = new CompoundNBT();
        if (!this.roleSet.isEmpty()) {
            compound.put("roles", this.roleSet.serialize());
        }
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        ListNBT roleList = nbt.getList("roles", Constants.NBT.TAG_STRING);
        this.roleSet = RoleConfiguration.get().readSet(roleList);
    }

    @Nonnull
    public RoleSet getRoles() {
        return this.roleSet;
    }
}
