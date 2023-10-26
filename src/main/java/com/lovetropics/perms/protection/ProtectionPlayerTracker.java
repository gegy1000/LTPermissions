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
    private final Set<UUID> queuedClear = new ObjectOpenHashSet<>();

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        INSTANCE.trackers.remove(event.getEntity().getUUID());
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
        queuedClear.addAll(trackers.keySet());
    }

    private void onPlayerChangeDimension(ServerPlayer player) {
        clearTracker(player, player.getUUID());
    }

    private void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (queuedClear.remove(uuid)) {
            clearTracker(player, uuid);
            // Clearing and re-entering within the same tick can cause troubles, so wait to process
            return;
        }

        Tracker tracker = this.getOrInitializeTracker(player, uuid);

        long blockPos = player.blockPosition().asLong();
        if (blockPos != tracker.lastBlockPos) {
            this.onPlayerMoved(player, tracker);
            tracker.lastBlockPos = blockPos;
        }
    }

    private Tracker getOrInitializeTracker(ServerPlayer player, UUID uuid) {
        Tracker tracker = trackers.get(uuid);
        if (tracker == null) {
            tracker = initializeTracker(player);
            trackers.put(uuid, tracker);
        }
        return tracker;
    }

    private Tracker initializeTracker(ServerPlayer player) {
        ProtectionManager protection = ProtectionManager.get(player.server);

        Tracker tracker = new Tracker();
        tracker.lastBlockPos = player.blockPosition().asLong();

        AuthorityMap<Authority> authorities = protection.selectWithBehavior(player.level().dimension());
        if (authorities == null) {
            return tracker;
        }

        EventSource source = EventSource.forEntity(player);
        for (Authority authority : authorities) {
            if (authority.eventFilter().accepts(source)) {
                tracker.inside.add(authority);
                authority.behavior().getBehavior().onPlayerEnter(player);
            } else {
                tracker.outside.add(authority);
            }
        }

        return tracker;
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
