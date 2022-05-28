package com.lovetropics.perms.protection.authority.behavior;

import com.google.common.collect.ImmutableList;
import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfig;
import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfigs;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class AuthorityBehaviorMap {
    public static final AuthorityBehaviorMap EMPTY = new AuthorityBehaviorMap(ImmutableList.of());

    public static final Codec<AuthorityBehaviorMap> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(AuthorityBehaviorMap::new, map -> map.behaviorIds);

    private final List<ResourceLocation> behaviorIds;
    private final AuthorityBehavior behavior;

    private AuthorityBehaviorMap(List<ResourceLocation> behaviorIds) {
        this.behaviorIds = behaviorIds;
        this.behavior = this.buildBehavior();
    }

    private AuthorityBehavior buildBehavior() {
        if (!this.behaviorIds.isEmpty()) {
            List<AuthorityBehavior> behaviors = new ArrayList<>();
            for (ResourceLocation id : this.behaviorIds) {
                AuthorityBehaviorConfig config = AuthorityBehaviorConfigs.REGISTRY.get(id);
                if (config != null) {
                    behaviors.add(config.createBehavior());
                }
            }

            if (behaviors.size() == 1) {
                return behaviors.get(0);
            } else if (!behaviors.isEmpty()) {
                return AuthorityBehavior.compose(behaviors.toArray(new AuthorityBehavior[0]));
            }
        }

        return AuthorityBehavior.EMPTY;
    }

    public AuthorityBehaviorMap addBehavior(ResourceLocation id) {
        if (!this.behaviorIds.contains(id)) {
            List<ResourceLocation> behaviorIds = new ArrayList<>(this.behaviorIds);
            behaviorIds.add(id);
            return new AuthorityBehaviorMap(behaviorIds);
        } else {
            return this;
        }
    }

    public AuthorityBehaviorMap removeBehavior(ResourceLocation id) {
        if (this.behaviorIds.contains(id)) {
            List<ResourceLocation> behaviorIds = new ArrayList<>(this.behaviorIds);
            behaviorIds.remove(id);
            return new AuthorityBehaviorMap(behaviorIds);
        } else {
            return this;
        }
    }

    public AuthorityBehaviorMap rebuild() {
        return new AuthorityBehaviorMap(this.behaviorIds);
    }

    public AuthorityBehavior getBehavior() {
        return this.behavior;
    }

    public List<ResourceLocation> getBehaviorIds() {
        return this.behaviorIds;
    }

    public boolean isEmpty() {
        return this.behaviorIds.isEmpty();
    }
}
