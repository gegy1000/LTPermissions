package com.lovetropics.perms.role;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nonnull;

public interface RoleLookup {
    RoleLookup EMPTY = new RoleLookup() {
        @Override
        @Nonnull
        public RoleReader byEntity(Entity entity) {
            return RoleReader.EMPTY;
        }

        @Override
        @Nonnull
        public RoleReader bySource(CommandSource source) {
            return RoleReader.EMPTY;
        }
    };

    @Nonnull
    default RoleReader byPlayer(PlayerEntity player) {
        return this.byEntity(player);
    }

    @Nonnull
    RoleReader byEntity(Entity entity);

    @Nonnull
    RoleReader bySource(CommandSource source);
}
