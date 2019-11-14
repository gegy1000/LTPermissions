package com.lovetropics.perms.modifier;

import net.minecraft.entity.player.ServerPlayerEntity;

public interface RoleModifier {
    default void notifyChange(ServerPlayerEntity player) {
    }
}
