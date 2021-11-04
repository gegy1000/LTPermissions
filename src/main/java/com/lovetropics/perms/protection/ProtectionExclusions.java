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
    public static final ProtectionExclusions EMPTY = new ProtectionExclusions();

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<ProtectionExclusions> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Codec.STRING.listOf().fieldOf("roles").forGetter(exclusions -> new ArrayList<>(exclusions.roles)),
                UUID_CODEC.listOf().fieldOf("players").forGetter(exclusions -> new ArrayList<>(exclusions.players))
        ).apply(instance, ProtectionExclusions::new);
    });

    private final Set<String> roles;
    private final Set<UUID> players;

    private ProtectionExclusions() {
        this.roles = new ObjectOpenHashSet<>();
        this.players = new ObjectOpenHashSet<>();
    }

    private ProtectionExclusions(Collection<String> roles, Collection<UUID> players) {
        this.roles = new ObjectOpenHashSet<>(roles);
        this.players = new ObjectOpenHashSet<>(players);
    }

    public ProtectionExclusions addRole(Role role) {
        if (this.roles.contains(role.id())) return this;

        ProtectionExclusions result = new ProtectionExclusions(this.roles, this.players);
        result.roles.add(role.id());
        return result;
    }

    public ProtectionExclusions removeRole(Role role) {
        if (!this.roles.contains(role.id())) return this;

        ProtectionExclusions result = new ProtectionExclusions(this.roles, this.players);
        result.roles.remove(role.id());
        return result;
    }

    public ProtectionExclusions addPlayer(GameProfile profile) {
        if (this.players.contains(profile.getId())) return this;

        ProtectionExclusions result = new ProtectionExclusions(this.roles, this.players);
        result.players.add(profile.getId());
        return result;
    }

    public ProtectionExclusions removePlayer(GameProfile profile) {
        if (!this.players.contains(profile.getId())) return this;

        ProtectionExclusions result = new ProtectionExclusions(this.roles, this.players);
        result.players.remove(profile.getId());
        return result;
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

    @Override
    public boolean accepts(EventSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            return !this.isExcluded((PlayerEntity) entity);
        } else {
            return true;
        }
    }

    public boolean isEmpty() {
        return this.players.isEmpty() && this.roles.isEmpty();
    }
}
