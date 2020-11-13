package com.lovetropics.perms.protection.scope;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

public final class GlobalScope implements ProtectionScope {
    public static final GlobalScope INSTANCE = new GlobalScope();

    private GlobalScope() {
    }

    @Override
    public boolean contains(DimensionType dimension) {
        return true;
    }

    @Override
    public boolean contains(DimensionType dimension, BlockPos pos) {
        return true;
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        root.putString("type", "global");
        return root;
    }
}
