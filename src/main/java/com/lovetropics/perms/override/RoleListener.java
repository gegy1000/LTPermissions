package com.lovetropics.perms.override;

import net.minecraft.server.level.ServerPlayer;

public interface RoleListener {
    void accept(ServerPlayer player);
}
