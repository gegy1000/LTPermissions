package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.role.Role;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public final class ProtectionExclusions implements EventFilter {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<ProtectionExclusions> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Codec.STRING.listOf().fieldOf("roles").forGetter(exclusions -> new ArrayList<>(exclusions.roles)),
                UUID_CODEC.listOf().fieldOf("players").forGetter(exclusions -> new ArrayList<>(exclusions.players))
        ).apply(instance, ProtectionExclusions::new);
    });

    private final Set<String> roles;
    private final Set<UUID> players;

    public ProtectionExclusions() {
        this.roles = new ObjectOpenHashSet<>();
        this.players = new ObjectOpenHashSet<>();
    }

    private ProtectionExclusions(Collection<String> roles, Collection<UUID> players) {
        this.roles = new ObjectOpenHashSet<>(roles);
        this.players = new ObjectOpenHashSet<>(players);
    }

    public boolean addRole(Role role) {
        return this.roles.add(role.id());
    }

    public boolean removeRole(Role role) {
        return this.roles.remove(role.id());
    }

    public boolean addPlayer(GameProfile profile) {
        return this.players.add(profile.getId());
    }

    public boolean removePlayer(GameProfile profile) {
        return this.players.remove(profile.getId());
    }

    public boolean isExcluded(PlayerEntity player) {
        if (this.players.contains(player.getUniqueID())) {
            return true;
        }

        if (player instanceof ServerPlayerEntity) {
            for (String excludeRoleId : this.roles) {
                Role excludeRole = LTPermissions.roles().get(excludeRoleId);
                if (excludeRole != null && LTPermissions.lookup().byPlayer(player).has(excludeRole)) {
                    return true;
                }
            }
        }

        return false;
    }

    public ProtectionExclusions copy() {
        return new ProtectionExclusions(this.roles, this.players);
    }

    @Override
    public boolean accepts(EventSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            return !this.isExcluded((PlayerEntity) entity);
        } else {
            return true;
        }
    }
}
