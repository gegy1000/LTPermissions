package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class DimensionShape implements AuthorityShape {
    public static final Codec<DimensionShape> CODEC = Level.RESOURCE_KEY_CODEC.xmap(DimensionShape::new, shape -> shape.dimension);

    private final ResourceKey<Level> dimension;

    public DimensionShape(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    @Override
    public boolean accepts(EventSource source) {
        ResourceKey<Level> dimension = source.getDimension();
        return dimension == null || dimension == this.dimension;
    }

    @Override
    public Codec<? extends AuthorityShape> getCodec() {
        return CODEC;
    }
}
