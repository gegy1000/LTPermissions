package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.Codec;

public final class UniverseShape implements AuthorityShape {
    public static final UniverseShape INSTANCE = new UniverseShape();

    public static final Codec<UniverseShape> CODEC = Codec.unit(INSTANCE);

    private UniverseShape() {
    }

    @Override
    public boolean accepts(EventSource source) {
        return true;
    }

    @Override
    public Codec<? extends AuthorityShape> getCodec() {
        return CODEC;
    }
}
