package com.lovetropics.perms.override;

import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public interface RoleOverride {
    default void notifyChange(MinecraftServer server, UUID player) {
    }
}
