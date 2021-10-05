package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public final class CylinderShape implements AuthorityShape {
    public static final Codec<CylinderShape> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                World.CODEC.fieldOf("dimension").forGetter(c -> c.dimension),
                Codec.INT.fieldOf("center_x").forGetter(c -> c.centerX),
                Codec.INT.fieldOf("center_z").forGetter(c -> c.centerZ),
                Codec.INT.fieldOf("min_y").forGetter(c -> c.minY),
                Codec.INT.fieldOf("max_y").forGetter(c -> c.maxY),
                Codec.INT.fieldOf("radius_x").forGetter(c -> c.radiusX),
                Codec.INT.fieldOf("radius_z").forGetter(c -> c.radiusZ)
        ).apply(instance, CylinderShape::new);
    });

    private final RegistryKey<World> dimension;
    private final int centerX;
    private final int centerZ;
    private final int minY;
    private final int maxY;
    private final int radiusX;
    private final int radiusZ;

    private final BlockBox bounds;

    public CylinderShape(RegistryKey<World> dimension, int centerX, int centerZ, int minY, int maxY, int radiusX, int radiusZ) {
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minY = minY;
        this.maxY = maxY;
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;

        this.bounds = BlockBox.of(
                new BlockPos(centerX - radiusX, minY, centerZ - radiusZ),
                new BlockPos(centerX + radiusX, maxY, centerZ + radiusZ)
        );
    }

    @Override
    public boolean accepts(EventSource source) {
        RegistryKey<World> dimension = source.getDimension();
        if (!this.acceptsDimension(dimension)) return false;

        BlockPos pos = source.getPos();
        if (pos == null) return true;

        if (this.bounds.contains(pos)) {
            float dx = (float) (pos.getX() - this.centerX) / this.radiusX;
            float dz = (float) (pos.getZ() - this.centerZ) / this.radiusZ;
            return dx * dx + dz * dz <= 1.0F;
        } else {
            return false;
        }
    }

    private boolean acceptsDimension(RegistryKey<World> dimension) {
        return dimension == null || dimension == this.dimension;
    }

    @Override
    public Codec<? extends AuthorityShape> getCodec() {
        return CODEC;
    }

    public static CylinderShape fromRegion(CylinderRegion region) {
        RegistryKey<World> dimension = WorldEditShapes.asDimension(region.getWorld());
        Vector3 center = region.getCenter();
        Vector2 radius = region.getRadius();
        int minY = region.getMinimumY();
        int maxY = region.getMaximumY();

        return new CylinderShape(
                dimension,
                MathHelper.floor(center.getX()), MathHelper.floor(center.getZ()),
                minY, maxY,
                MathHelper.floor(radius.getX()), MathHelper.floor(radius.getZ())
        );
    }

    @Override
    public Region tryIntoRegion(MinecraftServer server) {
        ServerWorld world = server.getWorld(this.dimension);
        return new CylinderRegion(
                ForgeAdapter.adapt(world),
                BlockVector3.at(this.centerX, 0, this.centerZ),
                Vector2.at(this.radiusX, this.radiusZ),
                this.minY, this.maxY
        );
    }
}
