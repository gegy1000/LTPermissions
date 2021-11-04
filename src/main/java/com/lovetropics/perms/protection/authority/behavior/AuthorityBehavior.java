package com.lovetropics.perms.protection.authority.behavior;

import net.minecraft.entity.player.ServerPlayerEntity;

public interface AuthorityBehavior {
    AuthorityBehavior EMPTY = new AuthorityBehavior() {
    };

    static AuthorityBehavior compose(AuthorityBehavior... behaviors) {
        return new Composite(behaviors);
    }

    default void onPlayerEnter(ServerPlayerEntity player) {
    }

    default void onPlayerExit(ServerPlayerEntity player) {
    }

    final class Composite implements AuthorityBehavior {
        private final AuthorityBehavior[] behaviors;

        Composite(AuthorityBehavior[] behaviors) {
            this.behaviors = behaviors;
        }

        @Override
        public void onPlayerEnter(ServerPlayerEntity player) {
            for (AuthorityBehavior behavior : this.behaviors) {
                behavior.onPlayerEnter(player);
            }
        }

        @Override
        public void onPlayerExit(ServerPlayerEntity player) {
            for (AuthorityBehavior behavior : this.behaviors) {
                behavior.onPlayerExit(player);
            }
        }
    }
}
