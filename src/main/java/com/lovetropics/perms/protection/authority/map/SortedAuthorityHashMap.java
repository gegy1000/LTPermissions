package com.lovetropics.perms.protection.authority.map;

import com.lovetropics.perms.protection.authority.Authority;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;

public final class SortedAuthorityHashMap<A extends Authority> implements AuthorityMap<A> {
    private final SortedSet<A> authorities = new ObjectRBTreeSet<>();
    private final Object2ObjectMap<String, A> byKey = new Object2ObjectOpenHashMap<>();

    @Override
    public void clear() {
        this.authorities.clear();
        this.byKey.clear();
    }

    @Override
    public boolean add(A authority) {
        if (this.byKey.put(authority.key(), authority) == null) {
            this.authorities.add(authority);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(A from, A to) {
        if (from.key().equals(to.key()) && this.byKey.replace(from.key(), from, to)) {
            this.authorities.remove(from);
            this.authorities.add(to);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public A remove(String key) {
        A authority = this.byKey.remove(key);
        if (authority != null) {
            this.authorities.remove(authority);
            return authority;
        }
        return null;
    }

    @Override
    @Nullable
    public A byKey(String key) {
        return this.byKey.get(key);
    }

    @Override
    public boolean contains(String key) {
        return this.byKey.containsKey(key);
    }

    @Override
    public Set<String> keySet() {
        return this.byKey.keySet();
    }

    @Override
    public Iterable<Object2ObjectMap.Entry<String, A>> entries() {
        return Object2ObjectMaps.fastIterable(this.byKey);
    }

    @Override
    public int size() {
        return this.authorities.size();
    }

    @Override
    public boolean isEmpty() {
        return this.authorities.isEmpty();
    }

    @Override
    @Nonnull
    public Iterator<A> iterator() {
        return this.authorities.iterator();
    }

    @Override
    public Stream<A> stream() {
        return this.authorities.stream();
    }
}
