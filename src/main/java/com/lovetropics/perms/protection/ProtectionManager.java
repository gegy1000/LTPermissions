package com.lovetropics.perms.protection;

import com.lovetropics.perms.PermissionResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProtectionManager extends WorldSavedData {
    private static final String KEY = "protection";

    private final RegionMap regions = new RegionMap();
    private final Map<RegistryKey<World>, RegionMap> regionsByDimension = new Reference2ObjectOpenHashMap<>();

    private final MinecraftServer server;

    private ProtectionManager(MinecraftServer server) {
        super(KEY);
        this.server = server;
    }

    public static ProtectionManager get(MinecraftServer server) {
        return server.func_241755_D_().getSavedData().getOrCreate(() -> new ProtectionManager(server), KEY);
    }

    public boolean add(ProtectionRegion region) {
        boolean added = this.regions.add(region);

        for (ServerWorld world : this.server.getWorlds()) {
            RegistryKey<World> dimension = world.getDimensionKey();
            if (region.scope.contains(dimension)) {
                this.regionsByDimension(dimension).add(region);
            }
        }

        return added;
    }

    public boolean remove(String key) {
        boolean removed = this.regions.remove(key) != null;

        for (ServerWorld world : this.server.getWorlds()) {
            RegistryKey<World> dimension = world.getDimensionKey();
            RegionMap regions = this.regionsByDimension.get(dimension);
            if (regions != null) {
                regions.remove(key);
            }
        }

        return removed;
    }

    @Nullable
    public ProtectionRegion byKey(String key) {
        return this.regions.byKey(key);
    }

    private RegionMap regionsByDimension(RegistryKey<World> dimension) {
        RegionMap regionsByDimension = this.regionsByDimension.get(dimension);
        if (regionsByDimension == null) {
            this.regionsByDimension.put(dimension, regionsByDimension = new RegionMap());
        }
        return regionsByDimension;
    }

    public Iterable<ProtectionRegion> sample(RegistryKey<World> dimension) {
        return this.regionsByDimension(dimension);
    }

    public Iterable<ProtectionRegion> sample(World world, BlockPos pos) {
        return this.sample(world.getDimensionKey(), pos);
    }

    public Iterable<ProtectionRegion> sample(RegistryKey<World> dimension, BlockPos pos) {
        List<ProtectionRegion> regions = new ArrayList<>();
        for (ProtectionRegion region : this.regionsByDimension(dimension)) {
            if (region.scope.contains(dimension, pos)) {
                regions.add(region);
            }
        }

        return regions;
    }

    public PermissionResult test(RegistryKey<World> dimension, ProtectionRule rule) {
        for (ProtectionRegion region : this.regionsByDimension(dimension)) {
            PermissionResult result = region.rules.test(rule);
            if (result != PermissionResult.PASS) {
                return result;
            }
        }
        return PermissionResult.PASS;
    }

    public PermissionResult test(World world, BlockPos pos, ProtectionRule rule) {
        return this.test(world.getDimensionKey(), pos, rule);
    }

    public PermissionResult test(RegistryKey<World> dimension, BlockPos pos, ProtectionRule rule) {
        for (ProtectionRegion region : this.regionsByDimension(dimension)) {
            if (region.scope.contains(dimension, pos)) {
                PermissionResult result = region.rules.test(rule);
                if (result != PermissionResult.PASS) {
                    return result;
                }
            }
        }
        return PermissionResult.PASS;
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        ListNBT regionList = new ListNBT();
        for (ProtectionRegion region : this.regions) {
            regionList.add(region.write(new CompoundNBT()));
        }

        root.put("regions", regionList);

        return root;
    }

    @Override
    public void read(CompoundNBT root) {
        this.regions.clear();
        this.regionsByDimension.clear();

        ListNBT regionsList = root.getList("regions", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < regionsList.size(); i++) {
            ProtectionRegion region = ProtectionRegion.read(regionsList.getCompound(i));
            this.add(region);
        }
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public Set<String> getRegionKeys() {
        return this.regions.getKeys();
    }
}
