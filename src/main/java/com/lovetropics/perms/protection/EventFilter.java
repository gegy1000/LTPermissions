package com.lovetropics.perms.protection;

import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

public interface EventFilter {
    static EventFilter universe() {
        return source -> true;
    }

    static EventFilter dimension(RegistryKey<World> dimension) {
        return source -> {
            RegistryKey<World> sourceDimension = source.getDimension();
            return sourceDimension == null || sourceDimension == dimension;
        };
    }

    static EventFilter and(EventFilter left, EventFilter right) {
        return source -> left.accepts(source) && right.accepts(source);
    }

    boolean accepts(EventSource source);
}
