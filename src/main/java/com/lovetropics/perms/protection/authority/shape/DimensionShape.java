package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.Codec;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

public final class DimensionShape implements AuthorityShape {
    public static final Codec<DimensionShape> CODEC = World.RESOURCE_KEY_CODEC.xmap(DimensionShape::new, shape -> shape.dimension);

    private final RegistryKey<World> dimension;

    public DimensionShape(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }

    @Override
    public boolean accepts(EventSource source) {
        RegistryKey<World> dimension = source.getDimension();
        return dimension == null || dimension == this.dimension;
    }

    @Override
    public Codec<? extends AuthorityShape> getCodec() {
        return CODEC;
    }
}
