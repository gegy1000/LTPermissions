package com.lovetropics.perms;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class CommandAliasConfiguration {
    private static final CommandAliasConfiguration EMPTY = new CommandAliasConfiguration(ImmutableMap.of());
    private static final JsonParser JSON = new JsonParser();

    public static final Codec<CommandAliasConfiguration> CODEC = Codec.unboundedMap(Codec.STRING, MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new))
            .xmap(CommandAliasConfiguration::new, CommandAliasConfiguration::getAliases);

    private final Map<String, String[]> aliases;

    private CommandAliasConfiguration(Map<String, String[]> aliases) {
        this.aliases = aliases;
    }

    public static CommandAliasConfiguration load() {
        Path path = Paths.get("config/command_aliases.json");
        if (!Files.exists(path)) {
            if (!setupDefaultConfig(path)) {
                return EMPTY;
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject root = JSON.parse(reader).getAsJsonObject();
            DataResult<CommandAliasConfiguration> result = CODEC.parse(JsonOps.INSTANCE, root);
            result.error().ifPresent(error -> {
                LTPermissions.LOGGER.warn("Malformed command aliases configuration: {}", error);
            });

            return result.result().orElse(EMPTY);
        } catch (IOException e) {
            LTPermissions.LOGGER.warn("Failed to load command_aliases.json configuration", e);
            return EMPTY;
        }
    }

    private static boolean setupDefaultConfig(Path path) {
        try (InputStream input = LTPermissions.class.getResourceAsStream("/data/ltperms/default_command_aliases.json")) {
            Files.copy(input, path);
            return true;
        } catch (IOException e) {
            LTPermissions.LOGGER.warn("Failed to load default command_aliases.json configuration", e);
            return false;
        }
    }

    public Map<String, String[]> getAliases() {
        return this.aliases;
    }
}
