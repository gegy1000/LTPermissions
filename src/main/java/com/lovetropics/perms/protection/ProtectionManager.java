package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.BuiltinAuthority;
import com.lovetropics.perms.protection.authority.UserAuthority;
import com.lovetropics.perms.protection.authority.behavior.AuthorityBehaviorMap;
import com.lovetropics.perms.protection.authority.behavior.config.AuthorityBehaviorConfigs;
import com.lovetropics.perms.protection.authority.map.AuthorityMap;
import com.lovetropics.perms.protection.authority.map.IndexedAuthorityMap;
import com.lovetropics.perms.protection.authority.map.SortedAuthorityHashMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class ProtectionManager extends SavedData {
    private static final String KEY = "protection";

    private final SortedAuthorityHashMap<UserAuthority> userAuthorities = new SortedAuthorityHashMap<>();
    private final IndexedAuthorityMap<Authority> allAuthorities = new IndexedAuthorityMap<>();

    private BuiltinAuthority builtinUniverse = BuiltinAuthority.universe();
    private final Reference2ObjectMap<ResourceKey<Level>, BuiltinAuthority> builtinDimensions = new Reference2ObjectOpenHashMap<>();

    private ProtectionManager() {
        super(KEY);
        this.allAuthorities.add(this.builtinUniverse);
    }

    public static ProtectionManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ProtectionManager::new, KEY);
    }

    public PermissionResult test(EventSource source, ProtectionRule rule) {
        Iterable<Authority> authorities = this.allAuthorities.selectByDimension(source, rule);
        for (Authority authority : authorities) {
            if (!authority.eventFilter().accepts(source)) {
                continue;
            }

            PermissionResult result = authority.rules().test(rule);
            if (result.isTerminator()) {
                return result;
            }
        }

        return PermissionResult.PASS;
    }

    public boolean denies(EventSource source, ProtectionRule rule) {
        return this.test(source, rule).isDenied();
    }

    @Nullable
    public AuthorityMap<Authority> selectWithBehavior(ResourceKey<Level> dimension) {
        return this.allAuthorities.selectWithBehavior(dimension);
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) world;
            ProtectionManager protection = get(serverWorld.getServer());
            protection.onWorldLoad(serverWorld);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel) {
            ServerLevel serverWorld = (ServerLevel) world;
            ProtectionManager protection = get(serverWorld.getServer());
            protection.onWorldUnload(serverWorld);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world instanceof ServerLevel) {
            if (AuthorityBehaviorConfigs.hasReloaded()) {
                ProtectionManager protection = get(event.world.getServer());
                protection.onReload();
            }
        }
    }

    private void onWorldLoad(ServerLevel world) {
        if (!this.builtinDimensions.containsKey(world.dimension())) {
            BuiltinAuthority dimension = BuiltinAuthority.dimension(world.dimension());
            this.addBuiltinDimension(world.dimension(), dimension);
        }

        this.allAuthorities.addDimensionIndex(world.dimension());

        this.onAuthoritiesChanged();
    }

    private void onWorldUnload(ServerLevel world) {
        BuiltinAuthority dimension = this.builtinDimensions.get(world.dimension());
        if (dimension != null && dimension.isEmpty()) {
            this.builtinDimensions.remove(world.dimension());
            this.allAuthorities.remove(dimension);
        }

        this.allAuthorities.removeDimensionIndex(world.dimension());

        this.onAuthoritiesChanged();
    }

    private void onReload() {
        List<Authority> authoritiesWithBehavior = new ArrayList<>();
        for (Authority authority : this.allAuthorities) {
            if (authority.hasBehavior()) {
                authoritiesWithBehavior.add(authority);
            }
        }

        for (Authority authority : authoritiesWithBehavior) {
            AuthorityBehaviorMap behavior = authority.behavior().rebuild();
            this.replaceAuthority(authority, authority.withBehavior(behavior));
        }
    }

    public boolean addAuthority(UserAuthority authority) {
        if (this.userAuthorities.add(authority)) {
            this.allAuthorities.add(authority);
            this.onAuthoritiesChanged();
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAuthority(UserAuthority authority) {
        if (this.userAuthorities.remove(authority)) {
            this.allAuthorities.remove(authority);
            this.onAuthoritiesChanged();
            return true;
        } else {
            return false;
        }
    }

    public void replaceAuthority(Authority from, Authority to) {
        if (this.allAuthorities.replace(from, to)) {
            if (from instanceof UserAuthority && to instanceof UserAuthority) {
                this.replaceUserAuthority((UserAuthority) from, (UserAuthority) to);
            }

            if (from instanceof BuiltinAuthority && to instanceof BuiltinAuthority) {
                this.replaceBuiltinAuthority((BuiltinAuthority) from, (BuiltinAuthority) to);
            }

            this.onAuthoritiesChanged();
        }
    }

    private void replaceUserAuthority(UserAuthority from, UserAuthority to) {
        this.userAuthorities.replace(from, to);
    }

    private void replaceBuiltinAuthority(BuiltinAuthority from, BuiltinAuthority to) {
        if (from == this.builtinUniverse) {
            this.builtinUniverse = to;
            return;
        }

        for (Reference2ObjectMap.Entry<ResourceKey<Level>, BuiltinAuthority> entry : Reference2ObjectMaps.fastIterable(this.builtinDimensions)) {
            BuiltinAuthority authority = entry.getValue();
            if (authority == from) {
                entry.setValue(to);
                return;
            }
        }
    }

    private void onAuthoritiesChanged() {
        ProtectionPlayerTracker.INSTANCE.onAuthoritiesChanged();
    }

    @Nullable
    public Authority getAuthorityByKey(String key) {
        return this.allAuthorities.byKey(key);
    }

    @Nullable
    public UserAuthority getUserAuthorityByKey(String key) {
        return this.userAuthorities.byKey(key);
    }

    public Stream<Authority> allAuthorities() {
        return this.allAuthorities.stream();
    }

    public Stream<UserAuthority> userAuthorities() {
        return this.userAuthorities.stream();
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag authorityList = new ListTag();

        for (UserAuthority authority : this.userAuthorities) {
            DataResult<Tag> result = UserAuthority.CODEC.encodeStart(NbtOps.INSTANCE, authority);
            result.result().ifPresent(authorityList::add);
        }

        root.put("authorities", authorityList);

        root.put("builtin", this.writeBuiltin(new CompoundTag()));

        return root;
    }

    private CompoundTag writeBuiltin(CompoundTag root) {
        CompoundTag dimensionsTag = new CompoundTag();

        for (Map.Entry<ResourceKey<Level>, BuiltinAuthority> entry : this.builtinDimensions.entrySet()) {
            ResourceKey<Level> dimension = entry.getKey();
            BuiltinAuthority authority = entry.getValue();
            if (authority.isEmpty()) continue;

            Codec<BuiltinAuthority> codec = BuiltinAuthority.dimensionCodec(dimension);
            codec.encodeStart(NbtOps.INSTANCE, authority)
                    .result().ifPresent(nbt -> {
                        dimensionsTag.put(dimension.location().toString(), nbt);
                    });
        }

        root.put("dimensions", dimensionsTag);

        BuiltinAuthority.universeCodec().encodeStart(NbtOps.INSTANCE, this.builtinUniverse)
                .result().ifPresent(nbt -> {
                    root.put("universe", nbt);
                });

        return root;
    }

    @Override
    public void load(CompoundTag root) {
        ListTag authoritiesList = root.getList("authorities", Constants.NBT.TAG_COMPOUND);

        for (Tag authorityTag : authoritiesList) {
            UserAuthority.CODEC.decode(NbtOps.INSTANCE, authorityTag)
                    .map(Pair::getFirst)
                    .result()
                    .ifPresent(this::addAuthority);
        }

        this.readBuiltin(root.getCompound("builtin"));

        this.onAuthoritiesChanged();
    }

    private void readBuiltin(CompoundTag root) {
        CompoundTag dimensionsTag = root.getCompound("dimensions");
        for (String dimensionKey : dimensionsTag.getAllKeys()) {
            ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimensionKey));

            Codec<BuiltinAuthority> codec = BuiltinAuthority.dimensionCodec(dimension);
            codec.decode(NbtOps.INSTANCE, dimensionsTag.getCompound(dimensionKey))
                    .map(Pair::getFirst)
                    .result()
                    .ifPresent(authority -> this.addBuiltinDimension(dimension, authority));
        }

        Tag universeTag = root.get("universe");
        BuiltinAuthority.universeCodec().decode(NbtOps.INSTANCE, universeTag)
                .map(Pair::getFirst)
                .result()
                .ifPresent(this::addBuiltinUniverse);
    }

    private void addBuiltinDimension(ResourceKey<Level> dimension, BuiltinAuthority authority) {
        BuiltinAuthority lastAuthority = this.builtinDimensions.put(dimension, authority);
        if (lastAuthority == null) {
            this.allAuthorities.add(authority);
        } else {
            this.allAuthorities.replace(lastAuthority, authority);
        }
    }

    private void addBuiltinUniverse(BuiltinAuthority authority) {
        BuiltinAuthority lastAuthority = this.builtinUniverse;
        this.builtinUniverse = authority;
        this.allAuthorities.replace(lastAuthority, authority);
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}
