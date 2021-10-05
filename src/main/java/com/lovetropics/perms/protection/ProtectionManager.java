package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.BuiltinAuthority;
import com.lovetropics.perms.protection.authority.UserAuthority;
import com.lovetropics.perms.protection.authority.map.IndexedAuthorityMap;
import com.lovetropics.perms.protection.authority.map.SortedAuthorityHashMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class ProtectionManager extends WorldSavedData {
    private static final String KEY = "protection";

    private final SortedAuthorityHashMap<UserAuthority> userAuthorities = new SortedAuthorityHashMap<>();
    private final Map<RegistryKey<World>, BuiltinAuthority> builtinDimensions = new Reference2ObjectOpenHashMap<>();
    private final IndexedAuthorityMap<Authority> allAuthorities = new IndexedAuthorityMap<>();

    private ProtectionManager() {
        super(KEY);
        this.allAuthorities.add(BuiltinAuthority.universe());
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

    private void onWorldLoad(ServerWorld world) {
        BuiltinAuthority dimension = BuiltinAuthority.dimension(world.getDimensionKey());
        this.builtinDimensions.put(world.getDimensionKey(), dimension);
        this.allAuthorities.add(dimension);

        this.allAuthorities.addDimension(world.getDimensionKey());
    }

    private void onWorldUnload(ServerWorld world) {
        BuiltinAuthority dimension = this.builtinDimensions.remove(world.getDimensionKey());
        if (dimension != null) {
            this.allAuthorities.remove(dimension);
        }

        this.allAuthorities.removeDimension(world.getDimensionKey());
    }

    public boolean addAuthority(UserAuthority authority) {
        if (this.userAuthorities.add(authority)) {
            this.allAuthorities.add(authority);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAuthority(UserAuthority authority) {
        if (this.userAuthorities.remove(authority)) {
            this.allAuthorities.remove(authority);
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

        this.allAuthorities.replace(from, to);
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
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}
