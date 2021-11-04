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
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
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
public final class ProtectionManager extends WorldSavedData {
    private static final String KEY = "protection";

    private final SortedAuthorityHashMap<UserAuthority> userAuthorities = new SortedAuthorityHashMap<>();
    private final IndexedAuthorityMap<Authority> allAuthorities = new IndexedAuthorityMap<>();

    private BuiltinAuthority builtinUniverse = BuiltinAuthority.universe();
    private final Map<RegistryKey<World>, BuiltinAuthority> builtinDimensions = new Reference2ObjectOpenHashMap<>();

    private ProtectionManager() {
        super(KEY);
        this.allAuthorities.add(this.builtinUniverse);
    }

    public static ProtectionManager get(MinecraftServer server) {
        return server.func_241755_D_().getSavedData().getOrCreate(ProtectionManager::new, KEY);
    }

    public PermissionResult test(EventSource source, ProtectionRule rule) {
        Iterable<Authority> authorities = this.allAuthorities.select(source.getDimension(), rule);
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
    public AuthorityMap<Authority> selectWithBehavior(RegistryKey<World> dimension) {
        return this.allAuthorities.selectWithBehavior(dimension);
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protection = get(serverWorld.getServer());
            protection.onWorldLoad(serverWorld);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protection = get(serverWorld.getServer());
            protection.onWorldUnload(serverWorld);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world instanceof ServerWorld) {
            if (AuthorityBehaviorConfigs.hasReloaded()) {
                ProtectionManager protection = get(event.world.getServer());
                protection.onReload();
            }
        }
    }

    private void onWorldLoad(ServerWorld world) {
        if (!this.builtinDimensions.containsKey(world.getDimensionKey())) {
            BuiltinAuthority dimension = BuiltinAuthority.dimension(world.getDimensionKey());
            this.addBuiltinDimension(world.getDimensionKey(), dimension);
        }

        this.allAuthorities.addDimensionIndex(world.getDimensionKey());

        this.onAuthoritiesChanged();
    }

    private void onWorldUnload(ServerWorld world) {
        BuiltinAuthority dimension = this.builtinDimensions.get(world.getDimensionKey());
        if (dimension != null && dimension.isEmpty()) {
            this.builtinDimensions.remove(world.getDimensionKey());
            this.allAuthorities.remove(dimension);
        }

        this.allAuthorities.removeDimensionIndex(world.getDimensionKey());

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
        if (from instanceof UserAuthority && to instanceof UserAuthority) {
            UserAuthority userFrom = (UserAuthority) from;
            UserAuthority userTo = (UserAuthority) to;
            this.userAuthorities.replace(userFrom, userTo);
        }

        if (this.allAuthorities.replace(from, to)) {
            this.onAuthoritiesChanged();
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
    public CompoundNBT write(CompoundNBT root) {
        ListNBT authorityList = new ListNBT();

        for (UserAuthority authority : this.userAuthorities) {
            DataResult<INBT> result = UserAuthority.CODEC.encodeStart(NBTDynamicOps.INSTANCE, authority);
            result.result().ifPresent(authorityList::add);
        }

        root.put("authorities", authorityList);

        root.put("builtin", this.writeBuiltin(new CompoundNBT()));

        return root;
    }

    private CompoundNBT writeBuiltin(CompoundNBT root) {
        CompoundNBT dimensionsTag = new CompoundNBT();

        for (Map.Entry<RegistryKey<World>, BuiltinAuthority> entry : this.builtinDimensions.entrySet()) {
            RegistryKey<World> dimension = entry.getKey();
            BuiltinAuthority authority = entry.getValue();
            if (authority.isEmpty()) continue;

            Codec<BuiltinAuthority> codec = BuiltinAuthority.dimensionCodec(dimension);
            codec.encodeStart(NBTDynamicOps.INSTANCE, authority)
                    .result().ifPresent(nbt -> {
                        dimensionsTag.put(dimension.getLocation().toString(), nbt);
                    });
        }

        root.put("dimensions", dimensionsTag);

        BuiltinAuthority.universeCodec().encodeStart(NBTDynamicOps.INSTANCE, this.builtinUniverse)
                .result().ifPresent(nbt -> {
                    root.put("universe", nbt);
                });

        return root;
    }

    @Override
    public void read(CompoundNBT root) {
        ListNBT authoritiesList = root.getList("authorities", Constants.NBT.TAG_COMPOUND);

        for (INBT authorityTag : authoritiesList) {
            UserAuthority.CODEC.decode(NBTDynamicOps.INSTANCE, authorityTag)
                    .map(Pair::getFirst)
                    .result()
                    .ifPresent(this::addAuthority);
        }

        this.readBuiltin(root.getCompound("builtin"));

        this.onAuthoritiesChanged();
    }

    private void readBuiltin(CompoundNBT root) {
        CompoundNBT dimensionsTag = root.getCompound("dimensions");
        for (String dimensionKey : dimensionsTag.keySet()) {
            RegistryKey<World> dimension = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(dimensionKey));

            Codec<BuiltinAuthority> codec = BuiltinAuthority.dimensionCodec(dimension);
            codec.decode(NBTDynamicOps.INSTANCE, dimensionsTag.getCompound(dimensionKey))
                    .map(Pair::getFirst)
                    .result()
                    .ifPresent(authority -> this.addBuiltinDimension(dimension, authority));
        }

        INBT universeTag = root.get("universe");
        BuiltinAuthority.universeCodec().decode(NBTDynamicOps.INSTANCE, universeTag)
                .map(Pair::getFirst)
                .result()
                .ifPresent(this::addBuiltinUniverse);
    }

    private void addBuiltinDimension(RegistryKey<World> dimension, BuiltinAuthority authority) {
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
