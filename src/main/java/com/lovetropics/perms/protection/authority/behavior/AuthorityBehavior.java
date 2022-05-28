package com.lovetropics.perms.protection.authority.behavior;

import net.minecraft.server.level.ServerPlayer;

public interface AuthorityBehavior {
    AuthorityBehavior EMPTY = new AuthorityBehavior() {
    };

    static AuthorityBehavior compose(AuthorityBehavior... behaviors) {
        return new Composite(behaviors);
    }

    default void onPlayerEnter(ServerPlayer player) {
    }

    default void onPlayerExit(ServerPlayer player) {
    }

    final class Composite implements AuthorityBehavior {
        private final AuthorityBehavior[] behaviors;

        Composite(AuthorityBehavior[] behaviors) {
            this.behaviors = behaviors;
        }

        @Override
        public void onPlayerEnter(ServerPlayer player) {
            for (AuthorityBehavior behavior : this.behaviors) {
                behavior.onPlayerEnter(player);
            }
        }

        @Override
        public void onPlayerExit(ServerPlayer player) {
            for (AuthorityBehavior behavior : this.behaviors) {
                behavior.onPlayerExit(player);
            }
        }
    }
}
