package com.lovetropics.perms.storage;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.UUID;

public final class PlayerRoleStorage extends WorldSavedData {
    private static final String KEY = "player_roles";

    private final MinecraftServer server;
    private final Object2ObjectMap<UUID, PlayerRoles> rolesByPlayer = new Object2ObjectOpenHashMap<>();

    private PlayerRoleStorage(MinecraftServer server) {
        super(KEY);
        this.server = server;
    }

    public static PlayerRoleStorage forServer(MinecraftServer server) {
        return server.func_241755_D_().getSavedData().getOrCreate(() -> new PlayerRoleStorage(server), KEY);
    }

    public PlayerRoles getOrCreate(UUID player) {
        return this.rolesByPlayer.computeIfAbsent(player, p -> new PlayerRoles(this.server, p));
    }

    public PlayerRoles getOrCreate(Entity player) {
        return this.getOrCreate(player.getUniqueID());
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        for (Object2ObjectMap.Entry<UUID, PlayerRoles> entry : Object2ObjectMaps.fastIterable(this.rolesByPlayer)) {
            ListNBT rolesList = entry.getValue().serialize();
            root.put(entry.getKey().toString(), rolesList);
        }
        return root;
    }

    @Override
    public void read(CompoundNBT root) {
        this.rolesByPlayer.clear();

        for (String key : root.keySet()) {
            UUID id = UUID.fromString(key);

            ListNBT rolesList = root.getList(key, Constants.NBT.TAG_STRING);
            PlayerRoles roles = new PlayerRoles(this.server, id);
            roles.deserialize(rolesList);

            this.rolesByPlayer.put(id, roles);
        }
    }

    @Override
    public boolean isDirty() {
        return true;
    }
}
