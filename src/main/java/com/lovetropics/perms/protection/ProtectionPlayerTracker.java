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

import java.util.*;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class ProtectionPlayerTracker {
    public static final ProtectionPlayerTracker INSTANCE = new ProtectionPlayerTracker();

    private final Object2ObjectMap<UUID, Tracker> trackers = new Object2ObjectOpenHashMap<>();
    private final Set<UUID> queuedReset = new ObjectOpenHashSet<>();

    private final List<Authority> enteringAuthorities = new ArrayList<>();
    private final List<AuthorityBehavior> enteringBehaviors = new ArrayList<>();
    private final Set<AuthorityBehavior> exitingBehaviors = new ReferenceArraySet<>();

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

        try {
            exitingBehaviors.addAll(tracker.behaviors);

            tracker.outside.clear();
            tracker.inside.clear();
            tracker.behaviors.clear();
            tracker.lastBlockPos = player.blockPosition().asLong();

            final AuthorityMap<Authority> authorities = protection.selectWithBehavior(player.level().dimension());
            if (authorities != null) {
                final EventSource source = EventSource.forEntity(player);
                for (final Authority authority : authorities) {
                    if (authority.eventFilter().accepts(source)) {
                        tracker.inside.add(authority);

                        final AuthorityBehavior behavior = authority.behavior().getBehavior();
                        if (tracker.behaviors.add(behavior)) {
                            exitingBehaviors.remove(behavior);
                            enteringBehaviors.add(behavior);
                        }
                    } else {
                        tracker.outside.add(authority);
                    }
                }
            }

            applyBehaviors(player, enteringBehaviors, exitingBehaviors);
        } finally {
            enteringAuthorities.clear();
            enteringBehaviors.clear();
            exitingBehaviors.clear();
        }
    }

    private void clearTracker(ServerPlayer player, UUID uuid) {
        Tracker tracker = trackers.remove(uuid);
        if (tracker != null) {
            for (AuthorityBehavior behavior : tracker.behaviors) {
                behavior.onPlayerExit(player);
            }
        }
    }

    private void onPlayerMoved(ServerPlayer player, Tracker tracker) {
        final EventSource source = EventSource.forEntity(player);
        try {
            if (collectEnteringAndExiting(tracker, source)) {
                applyBehaviors(player, enteringBehaviors, exitingBehaviors);
            }
        } finally {
            enteringAuthorities.clear();
            enteringBehaviors.clear();
            exitingBehaviors.clear();
        }
    }

    private boolean collectEnteringAndExiting(final Tracker tracker, final EventSource source) {
        final Iterator<Authority> outsideIterator = tracker.outside.iterator();
        while (outsideIterator.hasNext()) {
            final Authority outside = outsideIterator.next();
            if (!outside.eventFilter().accepts(source)) {
                continue;
            }

            // Defer adding these to the inside set, as we're about to iterate through the old ones
            enteringAuthorities.add(outside);
            outsideIterator.remove();

            final AuthorityBehavior behavior = outside.behavior().getBehavior();
            if (tracker.behaviors.add(behavior)) {
                enteringBehaviors.add(behavior);
            }
        }

        final Iterator<Authority> insideIterator = tracker.inside.iterator();
        while (insideIterator.hasNext()) {
            final Authority inside = insideIterator.next();
            if (inside.eventFilter().accepts(source)) {
                continue;
            }

            insideIterator.remove();
            tracker.outside.add(inside);
            exitingBehaviors.add(inside.behavior().getBehavior());
        }

        tracker.inside.addAll(enteringAuthorities);

        // If multiple authorities are applying a behavior to us, make sure that there's none left before removing it
        if (!exitingBehaviors.isEmpty()) {
            for (final Authority authority : tracker.inside) {
                exitingBehaviors.remove(authority.behavior().getBehavior());
            }
        }

        tracker.behaviors.removeAll(exitingBehaviors);

        return !enteringBehaviors.isEmpty() || !exitingBehaviors.isEmpty();
    }

    private void applyBehaviors(final ServerPlayer player, final Collection<AuthorityBehavior> entering, final Collection<AuthorityBehavior> exiting) {
        for (final AuthorityBehavior behavior : exiting) {
            behavior.onPlayerExit(player);
        }
        for (final AuthorityBehavior behavior : entering) {
            behavior.onPlayerEnter(player);
        }
    }

    private static final class Tracker {
        private final Set<Authority> inside = new ReferenceOpenHashSet<>();
        private final Set<Authority> outside = new ReferenceOpenHashSet<>();
        private final Set<AuthorityBehavior> behaviors = new ReferenceArraySet<>();
        private long lastBlockPos = -1;
    }
}
