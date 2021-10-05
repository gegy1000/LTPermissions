package com.lovetropics.perms.protection.authority.map;

import com.lovetropics.perms.protection.authority.Authority;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface AuthorityMap<A extends Authority> extends Iterable<A> {
    void clear();

    boolean add(A authority);

    boolean replace(A from, A to);

    @Nullable
    A remove(String key);

    default boolean remove(A authority) {
        return this.remove(authority.key()) != null;
    }

    @Nullable
    A byKey(String key);

    boolean contains(String key);

    Set<String> keySet();

    int size();

    default boolean isEmpty() {
        return this.size() == 0;
    }

    Iterable<Object2ObjectMap.Entry<String, A>> entries();

    default Stream<A> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }
}
