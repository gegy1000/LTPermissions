package com.lovetropics.perms.override;

import com.google.common.collect.ImmutableList;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.lib.permission.role.RoleOverrideReader;
import com.lovetropics.lib.permission.role.RoleOverrideType;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RoleOverrideMap implements RoleOverrideReader {
    public static final RoleOverrideMap EMPTY = new RoleOverrideMap(Map.of());

    @SuppressWarnings("unchecked")
    public static final Codec<RoleOverrideMap> CODEC = MoreCodecs.dispatchByMapKey(RoleOverrideType.REGISTRY, t -> MoreCodecs.listOrUnit((Codec<Object>) t.getCodec()))
            .xmap(RoleOverrideMap::new, m -> m.overrides);

    private final Map<RoleOverrideType<?>, List<Object>> overrides;

    private RoleOverrideMap(Map<RoleOverrideType<?>, List<Object>> overrides) {
        this.overrides = new Reference2ObjectOpenHashMap<>(overrides);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void notifyInitialize(ServerPlayer player) {
        for (RoleOverrideType<?> override : overrides.keySet()) {
            override.notifyInitialize(player);
        }
    }

    public void notifyChange(ServerPlayer player) {
        for (RoleOverrideType<?> override : overrides.keySet()) {
            override.notifyChange(player);
        }
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> List<T> get(RoleOverrideType<T> type) {
        return (List<T>) overrides.getOrDefault(type, ImmutableList.of());
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> List<T> getOrNull(RoleOverrideType<T> type) {
        return (List<T>) overrides.get(type);
    }

    @Override
    public Set<RoleOverrideType<?>> typeSet() {
        return overrides.keySet();
    }

    public static class Builder {
        private final Map<RoleOverrideType<?>, List<Object>> overrides = new Reference2ObjectOpenHashMap<>();

        private Builder() {
        }

        public Builder addAll(RoleOverrideReader overrides) {
            for (RoleOverrideType<?> type : overrides.typeSet()) {
                addAllUnchecked(type, overrides.get(type));
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        private <T> Builder addAllUnchecked(RoleOverrideType<T> type, Collection<?> overrides) {
            getOrCreateOverrides(type).addAll((Collection<T>) overrides);
            return this;
        }

        public <T> Builder addAll(RoleOverrideType<T> type, Collection<T> overrides) {
            getOrCreateOverrides(type).addAll(overrides);
            return this;
        }

        public <T> Builder add(RoleOverrideType<T> type, T override) {
            getOrCreateOverrides(type).add(override);
            return this;
        }

        @SuppressWarnings("unchecked")
        private <T> List<T> getOrCreateOverrides(RoleOverrideType<T> type) {
            return (List<T>) overrides.computeIfAbsent(type, t -> new ArrayList<>());
        }

        public RoleOverrideMap build() {
            return new RoleOverrideMap(overrides);
        }
    }
}
