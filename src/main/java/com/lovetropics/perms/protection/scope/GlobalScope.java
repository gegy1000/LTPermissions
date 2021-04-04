package com.lovetropics.perms.protection.scope;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class GlobalScope implements ProtectionScope {
    public static final GlobalScope INSTANCE = new GlobalScope();

    private GlobalScope() {
    }

    @Override
    public boolean contains(RegistryKey<World> dimension) {
        return true;
    }

    @Override
    public boolean contains(RegistryKey<World> dimension, BlockPos pos) {
        return true;
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        root.putString("type", "global");
        return root;
    }
}
