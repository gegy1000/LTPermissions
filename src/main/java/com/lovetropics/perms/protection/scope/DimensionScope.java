package com.lovetropics.perms.protection.scope;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

public final class DimensionScope implements ProtectionScope {
    private final DimensionType dimension;

    public DimensionScope(DimensionType dimension) {
        this.dimension = dimension;
    }

    @Override
    public boolean contains(DimensionType dimension) {
        return this.dimension == dimension;
    }

    @Override
    public boolean contains(DimensionType dimension, BlockPos pos) {
        return this.contains(dimension);
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        root.putString("type", "dimension");
        root.putString("dimension", this.dimension.getRegistryName().toString());
        return root;
    }
}
