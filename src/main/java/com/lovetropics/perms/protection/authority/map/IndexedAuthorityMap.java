package com.lovetropics.perms.protection.authority.map;

import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.EventSource;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;
import com.lovetropics.perms.protection.authority.Authority;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class IndexedAuthorityMap<A extends Authority> implements AuthorityMap<A> {
    private final AuthorityMap<A> main = new SortedAuthorityHashMap<>();
    private final Index<A> globalIndex = new Index<>();
    private final Reference2ObjectMap<ResourceKey<Level>, Index<A>> dimensionIndex = new Reference2ObjectOpenHashMap<>();

    public void addDimensionIndex(ResourceKey<Level> dimension) {
        EventSource source = EventSource.allOf(dimension);
        Index<A> index = new Index<>();
        for (A authority : this.main) {
            if (authority.eventFilter().accepts(source)) {
                index.add(authority);
            }
        }

        this.dimensionIndex.put(dimension, index);
    }

    public void removeDimensionIndex(ResourceKey<Level> dimension) {
        this.dimensionIndex.remove(dimension);
    }

    public Iterable<A> selectByDimension(EventSource source, ProtectionRule rule) {
        ResourceKey<Level> dimension = source.getDimension();
        if (dimension == null) {
            AuthorityMap<A> map = this.globalIndex.byRule.get(rule);
            if (map != null) {
                return map;
            }
        }

        Index<A> dimensionIndex = this.dimensionIndex.get(dimension);
        if (dimensionIndex != null) {
            AuthorityMap<A> map = dimensionIndex.byRule.get(rule);
            if (map != null) {
                return map;
            }
        }

        return Collections.emptyList();
    }

    @Nullable
    public AuthorityMap<A> selectWithBehavior(ResourceKey<Level> dimension) {
        Index<A> dimensionIndex = this.dimensionIndex.get(dimension);
        if (dimensionIndex != null) {
            return dimensionIndex.allWithBehavior;
        }
        return null;
    }

    @Override
    public void clear() {
        this.main.clear();
        this.globalIndex.clear();
        this.dimensionIndex.clear();
    }

    @Override
    public boolean add(A authority) {
        if (this.main.add(authority)) {
            this.globalIndex.add(authority);
            this.addToDimension(authority);
            return true;
        }

        return false;
    }

    @Override
    public boolean replace(A from, A to) {
        if (this.main.replace(from, to)) {
            this.globalIndex.replace(from, to);
            this.replaceInDimension(from, to);
            return true;
        }

        return false;
    }

    @Override
    @Nullable
    public A remove(String key) {
        A authority = this.main.remove(key);
        if (authority != null) {
            this.globalIndex.remove(key);
            this.removeFromDimension(key);
            return authority;
        }
        return null;
    }

    @Override
    @Nullable
    public A byKey(String key) {
        return this.main.byKey(key);
    }

    @Override
    public boolean contains(String key) {
        return this.main.contains(key);
    }

    @Override
    public Set<String> keySet() {
        return this.main.keySet();
    }

    @Override
    public int size() {
        return this.main.size();
    }

    @Override
    public Iterable<Object2ObjectMap.Entry<String, A>> entries() {
        return this.main.entries();
    }

    @Nonnull
    @Override
    public Iterator<A> iterator() {
        return this.main.iterator();
    }

    @Override
    public Stream<A> stream() {
        return this.main.stream();
    }

    private void addToDimension(A authority) {
        EventFilter filter = authority.eventFilter();
        for (Map.Entry<ResourceKey<Level>, Index<A>> entry : Reference2ObjectMaps.fastIterable(this.dimensionIndex)) {
            ResourceKey<Level> dimension = entry.getKey();
            if (filter.accepts(EventSource.allOf(dimension))) {
                Index<A> dimensionIndex = entry.getValue();
                dimensionIndex.add(authority);
            }
        }
    }

    private void replaceInDimension(A from, A to) {
        EventFilter fromFilter = from.eventFilter();
        EventFilter toFilter = to.eventFilter();

        for (Map.Entry<ResourceKey<Level>, Index<A>> entry : this.dimensionIndex.entrySet()) {
            ResourceKey<Level> dimension = entry.getKey();
            EventSource source = EventSource.allOf(dimension);
            Index<A> dimensionIndex = entry.getValue();

            boolean fromIncluded = fromFilter.accepts(source);
            boolean toIncluded = toFilter.accepts(source);
            if (fromIncluded && toIncluded) {
                dimensionIndex.replace(from, to);
            } else if (fromIncluded) {
                dimensionIndex.remove(from.key());
            } else if (toIncluded) {
                dimensionIndex.add(to);
            }
        }
    }

    private void removeFromDimension(String key) {
        for (Index<A> authorities : this.dimensionIndex.values()) {
            authorities.remove(key);
        }
    }

    static final class Index<A extends Authority> {
        final AuthorityMap<A> allWithBehavior = new SortedAuthorityHashMap<>();
        final Map<ProtectionRule, AuthorityMap<A>> byRule = new Reference2ObjectOpenHashMap<>();

        void clear() {
            this.byRule.clear();
            this.allWithBehavior.clear();
        }

        void add(A authority) {
            if (authority.hasBehavior()) {
                this.allWithBehavior.add(authority);
            }

            ProtectionRuleMap rules = authority.rules();
            for (ProtectionRule rule : rules.keySet()) {
                this.getMapForRule(rule).add(authority);
            }
        }

        void replace(A from, A to) {
            if (from.hasBehavior() && !to.hasBehavior()) {
                this.allWithBehavior.remove(from);
            } else if (to.hasBehavior() && !from.hasBehavior()) {
                this.allWithBehavior.add(to);
            } else {
                this.allWithBehavior.replace(from, to);
            }

            Set<ProtectionRule> fromRules = from.rules().keySet();
            Set<ProtectionRule> toRules = to.rules().keySet();

            for (ProtectionRule rule : fromRules) {
                AuthorityMap<A> map = this.getMapForRule(rule);
                if (toRules.contains(rule)) {
                    map.replace(from, to);
                } else {
                    map.remove(from.key());
                }
            }

            for (ProtectionRule rule : toRules) {
                if (!fromRules.contains(rule)) {
                    this.getMapForRule(rule).add(to);
                }
            }
        }

        void remove(String key) {
            this.allWithBehavior.remove(key);
            for (AuthorityMap<A> map : this.byRule.values()) {
                map.remove(key);
            }
        }

        private AuthorityMap<A> getMapForRule(ProtectionRule rule) {
            return this.byRule.computeIfAbsent(rule, r -> new SortedAuthorityHashMap<>());
        }
    }
}
