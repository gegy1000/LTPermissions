package com.lovetropics.perms.protection.scope;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DimensionScope implements ProtectionScope {
    private final RegistryKey<World> dimension;

    public DimensionScope(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }

    @Override
    public boolean contains(RegistryKey<World> dimension) {
        return this.dimension == dimension;
    }

    @Override
    public boolean contains(RegistryKey<World> dimension, BlockPos pos) {
        return this.contains(dimension);
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        root.putString("type", "dimension");
        root.putString("dimension", this.dimension.getLocation().toString());
        return root;
    }
}
