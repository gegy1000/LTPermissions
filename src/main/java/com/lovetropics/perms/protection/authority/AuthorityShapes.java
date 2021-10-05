package com.lovetropics.perms.protection.authority;

import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.EventSource;
import com.lovetropics.perms.protection.authority.shape.AuthorityShape;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public final class AuthorityShapes implements EventFilter {
    public static final Codec<AuthorityShapes> CODEC = Codec.unboundedMap(Codec.STRING, AuthorityShape.CODEC)
            .xmap(AuthorityShapes::new, shapes -> shapes.map);

    private final Map<String, AuthorityShape> map;
    private final AuthorityShape[] array;

    private AuthorityShapes(Map<String, AuthorityShape> map) {
        this.map = map;
        this.array = map.values().toArray(new AuthorityShape[0]);
    }

    public AuthorityShapes() {
        this(new Object2ObjectOpenHashMap<>());
    }

    public AuthorityShapes addShape(String key, AuthorityShape shape) {
        Map<String, AuthorityShape> newMap = new Object2ObjectOpenHashMap<>(this.map);
        newMap.put(key, shape);
        return new AuthorityShapes(newMap);
    }

    public AuthorityShapes removeShape(String key) {
        Map<String, AuthorityShape> newMap = new Object2ObjectOpenHashMap<>(this.map);
        if (newMap.remove(key) != null) {
            return new AuthorityShapes(newMap);
        } else {
            return this;
        }
    }

    @Nullable
    public AuthorityShape get(String key) {
        return this.map.get(key);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean accepts(EventSource source) {
        for (AuthorityShape shape : this.array) {
            if (shape.accepts(source)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> keySet() {
        return this.map.keySet();
    }
}
