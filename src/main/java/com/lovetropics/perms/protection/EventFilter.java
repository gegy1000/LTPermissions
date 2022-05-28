package com.lovetropics.perms.protection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface EventFilter {
    static EventFilter universe() {
        return source -> true;
    }

    static EventFilter dimension(ResourceKey<Level> dimension) {
        return source -> {
            ResourceKey<Level> sourceDimension = source.getDimension();
            return sourceDimension == null || sourceDimension == dimension;
        };
    }

    static EventFilter and(EventFilter left, EventFilter right) {
        return source -> left.accepts(source) && right.accepts(source);
    }

    boolean accepts(EventSource source);
}
