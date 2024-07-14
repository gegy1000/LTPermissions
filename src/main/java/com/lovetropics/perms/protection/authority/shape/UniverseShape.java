package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.MapCodec;

public final class UniverseShape implements AuthorityShape {
    public static final UniverseShape INSTANCE = new UniverseShape();

    public static final MapCodec<UniverseShape> CODEC = MapCodec.unit(INSTANCE);

    private UniverseShape() {
    }

    @Override
    public boolean accepts(EventSource source) {
        return true;
    }

    @Override
    public MapCodec<UniverseShape> getCodec() {
        return CODEC;
    }
}
