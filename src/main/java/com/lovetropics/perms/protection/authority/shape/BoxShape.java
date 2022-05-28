package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public final class BoxShape implements AuthorityShape {
    public static final Codec<BoxShape> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(c -> c.dimension),
                BlockBox.CODEC.fieldOf("box").forGetter(c -> c.box)
        ).apply(instance, BoxShape::new);
    });

    private final ResourceKey<Level> dimension;
    private final BlockBox box;

    public BoxShape(ResourceKey<Level> dimension, BlockBox box) {
        this.dimension = dimension;
        this.box = box;
    }

    @Override
    public boolean accepts(EventSource source) {
        BlockPos pos = source.getPos();
        ResourceKey<Level> dimension = source.getDimension();
        return (dimension == null || dimension == this.dimension) && (pos == null || this.box.contains(pos));
    }

    @Override
    public Codec<? extends AuthorityShape> getCodec() {
        return CODEC;
    }

    public static BoxShape fromRegion(Region region) {
        ResourceKey<Level> dimension = WorldEditShapes.asDimension(region.getWorld());
        BlockPos min = ForgeAdapter.toBlockPos(region.getMinimumPoint());
        BlockPos max = ForgeAdapter.toBlockPos(region.getMaximumPoint());
        return new BoxShape(dimension, BlockBox.of(min, max));
    }

    @Nullable
    @Override
    public Region tryIntoRegion(MinecraftServer server) {
        ServerLevel world = server.getLevel(this.dimension);
        return new CuboidRegion(
                ForgeAdapter.adapt(world),
                ForgeAdapter.adapt(this.box.min),
                ForgeAdapter.adapt(this.box.max)
        );
    }
}
