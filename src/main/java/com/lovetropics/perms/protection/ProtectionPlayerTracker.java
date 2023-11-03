package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.behavior.AuthorityBehavior;
import com.lovetropics.perms.protection.authority.map.AuthorityMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class ProtectionPlayerTracker {
    public static final ProtectionPlayerTracker INSTANCE = new ProtectionPlayerTracker();

    private final Object2ObjectMap<UUID, Tracker> trackers = new Object2ObjectOpenHashMap<>();
    private final Set<UUID> queuedReset = new ObjectOpenHashSet<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof final ServerPlayer player) {
            INSTANCE.clearTracker(player, player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            INSTANCE.tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.onPlayerChangeDimension(player);
        }
    }

    public void invalidate() {
        queuedReset.addAll(trackers.keySet());
    }

    private void onPlayerChangeDimension(ServerPlayer player) {
        final Tracker tracker = trackers.get(player);
        if (tracker != null) {
            resetTracker(player, tracker);
        }
    }

    private void tickPlayer(final ServerPlayer player) {
        final UUID playerId = player.getUUID();
        if (queuedReset.remove(playerId)) {
            final Tracker tracker = trackers.get(player);
            if (tracker != null) {
                resetTracker(player, tracker);
                // We already updated the tracker state for this tick, so we don't need to check it again
                return;
            }
        }

        final Tracker tracker = getOrInitializeTracker(player, playerId);

        final long blockPos = player.blockPosition().asLong();
        if (blockPos != tracker.lastBlockPos) {
            this.onPlayerMoved(player, tracker);
            tracker.lastBlockPos = blockPos;
        }
    }

    private Tracker getOrInitializeTracker(ServerPlayer player, UUID uuid) {
        Tracker tracker = trackers.get(uuid);
        if (tracker == null) {
            tracker = new Tracker();
            resetTracker(player, tracker);
            trackers.put(uuid, tracker);
        }
        return tracker;
    }

    private void resetTracker(final ServerPlayer player, final Tracker tracker) {
        final ProtectionManager protection = ProtectionManager.get(player.server);

        final Set<AuthorityBehavior> exitingBehaviors = new ReferenceArraySet<>(tracker.inside.size());
        for (final Authority authority : tracker.inside) {
            exitingBehaviors.add(authority.behavior().getBehavior());
        }

        tracker.outside.clear();
        tracker.inside.clear();
        tracker.lastBlockPos = player.blockPosition().asLong();

        final AuthorityMap<Authority> authorities = protection.selectWithBehavior(player.level().dimension());
        if (authorities != null) {
            final EventSource source = EventSource.forEntity(player);
            for (final Authority authority : authorities) {
                if (authority.eventFilter().accepts(source)) {
                    final AuthorityBehavior behavior = authority.behavior().getBehavior();
                    if (!exitingBehaviors.remove(behavior)) {
                        behavior.onPlayerEnter(player);
                    }
                    tracker.inside.add(authority);
                } else {
                    tracker.outside.add(authority);
                }
            }
        }

        for (final AuthorityBehavior behavior : exitingBehaviors) {
            behavior.onPlayerExit(player);
        }
    }

    private void clearTracker(ServerPlayer player, UUID uuid) {
        Tracker tracker = trackers.remove(uuid);
        if (tracker != null) {
            for (Authority authority : tracker.inside) {
                authority.behavior().getBehavior().onPlayerExit(player);
            }
        }
    }

    private void onPlayerMoved(ServerPlayer player, Tracker tracker) {
        EventSource source = EventSource.forEntity(player);

        // TODO: how can we optimise this further?
        List<Authority> entering = this.collectEntering(tracker, source);
        List<Authority> exiting = this.collectExiting(tracker, source);
        if (entering == null && exiting == null) {
            return;
        }

        Set<AuthorityBehavior> enteringBehaviors = new ReferenceArraySet<>();
        if (entering != null) {
            for (Authority authority : entering) {
                tracker.inside.add(authority);
                tracker.outside.remove(authority);
                enteringBehaviors.add(authority.behavior().getBehavior());
            }
        }

        Set<AuthorityBehavior> exitingBehaviors = new ReferenceArraySet<>();
        if (exiting != null) {
            for (Authority authority : exiting) {
                tracker.outside.add(authority);
                tracker.inside.remove(authority);
                exitingBehaviors.add(authority.behavior().getBehavior());
            }
        }

        for (AuthorityBehavior behavior : enteringBehaviors) {
            if (!exitingBehaviors.contains(behavior)) {
                behavior.onPlayerEnter(player);
            }
        }

        for (AuthorityBehavior behavior : exitingBehaviors) {
            if (!enteringBehaviors.contains(behavior)) {
                behavior.onPlayerExit(player);
            }
        }
    }

    @Nullable
    private List<Authority> collectEntering(Tracker tracker, EventSource source) {
        List<Authority> entering = null;
        for (Authority outside : tracker.outside) {
            if (outside.eventFilter().accepts(source)) {
                if (entering == null) {
                    entering = new ArrayList<>();
                }
                entering.add(outside);
            }
        }
        return entering;
    }

    @Nullable
    private List<Authority> collectExiting(Tracker tracker, EventSource source) {
        List<Authority> exiting = null;
        for (Authority inside : tracker.inside) {
            if (!inside.eventFilter().accepts(source)) {
                if (exiting == null) {
                    exiting = new ArrayList<>();
                }
                exiting.add(inside);
            }
        }
        return exiting;
    }

    private static final class Tracker {
        private final Set<Authority> inside = new ReferenceOpenHashSet<>();
        private final Set<Authority> outside = new ReferenceOpenHashSet<>();
        private long lastBlockPos = -1;
    }
}
