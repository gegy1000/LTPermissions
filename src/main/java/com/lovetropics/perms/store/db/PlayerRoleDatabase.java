package com.lovetropics.perms.store.db;

import com.lovetropics.perms.config.RolesConfig;
import com.lovetropics.perms.store.PlayerRoleSet;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraftforge.common.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;

public final class PlayerRoleDatabase implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger(PlayerRoleDatabase.class);

    private final Uuid2BinaryDatabase binary;

    private PlayerRoleDatabase(Uuid2BinaryDatabase binary) {
        this.binary = binary;
    }

    public static PlayerRoleDatabase open(Path path) throws IOException {
        Uuid2BinaryDatabase binary = Uuid2BinaryDatabase.open(path);
        return new PlayerRoleDatabase(binary);
    }

    public void tryLoadInto(UUID uuid, PlayerRoleSet roles) {
        try {
            ByteBuffer bytes = this.binary.get(uuid);
            if (bytes != null) {
                try {
                    deserializeRoles(roles, bytes);
                } catch (IOException e) {
                    LOGGER.error("Failed to deserialize roles for {}, dropping", uuid, e);
                    this.binary.remove(uuid);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load roles for {}", uuid, e);
        }
    }

    public void trySave(UUID uuid, PlayerRoleSet roles) {
        try {
            if (!roles.isEmpty()) {
                ByteBuffer bytes = serializeRoles(roles);
                this.binary.put(uuid, bytes);
            } else {
                this.binary.remove(uuid);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save roles for {}", uuid, e);
        }
    }

    private static ByteBuffer serializeRoles(PlayerRoleSet roles) throws IOException {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("roles", roles.serialize());

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CompressedStreamTools.writeCompressed(nbt, output);
            return ByteBuffer.wrap(output.toByteArray());
        }
    }

    private static void deserializeRoles(PlayerRoleSet roles, ByteBuffer bytes) throws IOException {
        RolesConfig config = RolesConfig.get();

        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes.array())) {
            CompoundNBT nbt = CompressedStreamTools.readCompressed(input);
            roles.deserialize(config, nbt.getList("roles", Constants.NBT.TAG_STRING));
            roles.rebuildOverridesAndInitialize();
        }
    }

    @Override
    public void close() throws IOException {
        this.binary.close();
    }
}
