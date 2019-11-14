package com.lovetropics.perms.capability;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

public class DelegatedCapStorage<N extends INBT, T extends ICapabilitySerializable<N>> implements Capability.IStorage<T> {
    @Nullable
    @Override
    public INBT writeNBT(Capability<T> capability, T instance, Direction side) {
        return instance.serializeNBT();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readNBT(Capability<T> capability, T instance, Direction side, INBT nbt) {
        instance.deserializeNBT((N) nbt);
    }
}
