package com.lovetropics.perms.protection.authority.map;

import com.lovetropics.perms.protection.EventSource;
import com.lovetropics.perms.protection.ProtectionRule;
import com.lovetropics.perms.protection.ProtectionRuleMap;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.EventFilter;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class IndexedAuthorityMap<A extends Authority> implements AuthorityMap<A> {
    private final AuthorityMap<A> main = new SortedAuthorityHashMap<>();
    private final Reference2ObjectMap<RegistryKey<World>, DimensionMap<A>> byDimension = new Reference2ObjectOpenHashMap<>();

    public void addDimension(RegistryKey<World> dimension) {
        EventSource source = EventSource.allOf(dimension);

        DimensionMap<A> dimensionMap = new DimensionMap<>(dimension);
        for (A authority : this.main) {
            if (authority.eventFilter().accepts(source)) {
                dimensionMap.add(authority);
            }
        }

        this.byDimension.put(dimension, dimensionMap);
    }

    public void removeDimension(RegistryKey<World> dimension) {
        this.byDimension.remove(dimension);
    }

    public Iterable<A> select(RegistryKey<World> dimension, ProtectionRule rule) {
        DimensionMap<A> dimensionMap = this.byDimension.get(dimension);
        if (dimensionMap != null) {
            AuthorityMap<A> map = dimensionMap.byRule.get(rule);
            if (map != null) {
                return map;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void clear() {
        this.main.clear();
        this.byDimension.clear();
    }

    @Override
    public boolean add(A authority) {
        if (this.main.add(authority)) {
            this.addToDimension(authority);
            return true;
        }

        return false;
    }

    @Override
    public boolean replace(A from, A to) {
        if (this.main.replace(from, to)) {
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
        for (Map.Entry<RegistryKey<World>, DimensionMap<A>> entry : Reference2ObjectMaps.fastIterable(this.byDimension)) {
            RegistryKey<World> dimension = entry.getKey();
            if (filter.accepts(EventSource.allOf(dimension))) {
                DimensionMap<A> dimensionMap = entry.getValue();
                dimensionMap.add(authority);
            }
        }
    }

    private void replaceInDimension(A from, A to) {
        EventFilter fromFilter = from.eventFilter();
        EventFilter toFilter = to.eventFilter();

        for (DimensionMap<A> dimensionMap : this.byDimension.values()) {
            boolean fromIncluded = fromFilter.accepts(dimensionMap.eventSource);
            boolean toIncluded = toFilter.accepts(dimensionMap.eventSource);
            if (fromIncluded && toIncluded) {
                dimensionMap.replace(from, to);
            } else if (fromIncluded) {
                dimensionMap.remove(from.key());
            } else if (toIncluded) {
                dimensionMap.add(to);
            }
        }
    }

    private void removeFromDimension(String key) {
        for (DimensionMap<A> authorities : this.byDimension.values()) {
            authorities.remove(key);
        }
    }

    static final class DimensionMap<A extends Authority> {
        final EventSource eventSource;
        final Map<ProtectionRule, AuthorityMap<A>> byRule = new Reference2ObjectOpenHashMap<>();

        DimensionMap(RegistryKey<World> dimension) {
            this.eventSource = EventSource.allOf(dimension);
        }

        void add(A authority) {
            ProtectionRuleMap rules = authority.rules();
            for (ProtectionRule rule : rules.keySet()) {
                this.getMapForRule(rule).add(authority);
            }
        }

        void replace(A from, A to) {
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
            for (AuthorityMap<A> map : this.byRule.values()) {
                map.remove(key);
            }
        }

        private AuthorityMap<A> getMapForRule(ProtectionRule rule) {
            return this.byRule.computeIfAbsent(rule, r -> new SortedAuthorityHashMap<>());
        }
    }
}
