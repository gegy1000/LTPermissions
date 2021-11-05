package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.protection.authority.Authority;
import com.lovetropics.perms.protection.authority.map.AuthorityMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
    private final Set<UUID> queuedReinitialize = new ObjectOpenHashSet<>();

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();
        INSTANCE.trackers.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (event.phase == TickEvent.Phase.END && player instanceof ServerPlayerEntity) {
            INSTANCE.tickPlayer((ServerPlayerEntity) player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            INSTANCE.onPlayerChangeDimension((ServerPlayerEntity) player);
        }
    }

    public void onAuthoritiesChanged() {
        this.queuedReinitialize.addAll(this.trackers.keySet());
    }

    private void onPlayerChangeDimension(ServerPlayerEntity player) {
        this.queuedReinitialize.add(player.getUniqueID());
    }

    private void tickPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUniqueID();
        Tracker tracker = this.getOrInitializeTracker(player, uuid);

        long blockPos = player.getPosition().toLong();
        if (blockPos != tracker.lastBlockPos) {
            this.onPlayerMoved(player, tracker);
            tracker.lastBlockPos = blockPos;
        }
    }

    private Tracker getOrInitializeTracker(ServerPlayerEntity player, UUID uuid) {
        Object2ObjectMap<UUID, Tracker> trackers = this.trackers;
        Tracker tracker = trackers.get(uuid);

        if (tracker == null) {
            tracker = this.initializeTracker(player);
            trackers.put(uuid, tracker);
        } else if (this.queuedReinitialize.remove(uuid)) {
            Tracker lastTracker = tracker;
            tracker = this.initializeTracker(player);
            this.onTrackerReinitialized(player, lastTracker, tracker);

            trackers.put(uuid, tracker);
        }

        return tracker;
    }

    private Tracker initializeTracker(ServerPlayerEntity player) {
        ProtectionManager protection = ProtectionManager.get(player.server);

        Tracker tracker = new Tracker();
        tracker.lastBlockPos = player.getPosition().toLong();

        AuthorityMap<Authority> authorities = protection.selectWithBehavior(player.world.getDimensionKey());
        if (authorities == null) {
            return tracker;
        }

        EventSource source = EventSource.forEntity(player);
        for (Authority authority : authorities) {
            if (authority.eventFilter().accepts(source)) {
                tracker.inside.add(authority);
            } else {
                tracker.outside.add(authority);
            }
        }

        return tracker;
    }

    // TODO: consolidate with movement logic?
    private void onTrackerReinitialized(ServerPlayerEntity player, Tracker lastTracker, Tracker newTracker) {
        for (Authority authority : newTracker.inside) {
            if (!lastTracker.inside.contains(authority)) {
                this.onPlayerEnter(player, authority);
            }
        }

        for (Authority authority : lastTracker.inside) {
            if (!newTracker.inside.contains(authority)) {
                this.onPlayerExit(player, authority);
            }
        }
    }

    private void onPlayerMoved(ServerPlayerEntity player, Tracker tracker) {
        EventSource source = EventSource.forEntity(player);

        // TODO: how can we optimise this further?
        List<Authority> entering = this.collectEntering(tracker, source);
        List<Authority> exiting = this.collectExiting(tracker, source);

        if (entering != null) {
            for (Authority authority : entering) {
                tracker.inside.add(authority);
                tracker.outside.remove(authority);

                this.onPlayerEnter(player, authority);
            }
        }

        if (exiting != null) {
            for (Authority authority : exiting) {
                tracker.outside.add(authority);
                tracker.inside.remove(authority);

                this.onPlayerExit(player, authority);
            }
        }
    }

    private void onPlayerEnter(ServerPlayerEntity player, Authority authority) {
        authority.behavior().getBehavior().onPlayerEnter(player);
    }

    private void onPlayerExit(ServerPlayerEntity player, Authority authority) {
        authority.behavior().getBehavior().onPlayerExit(player);
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
