package com.lovetropics.perms.override;

import net.minecraft.entity.player.ServerPlayerEntity;

public interface RoleOverride {
    default void notifyChange(ServerPlayerEntity player) {
    }
}
