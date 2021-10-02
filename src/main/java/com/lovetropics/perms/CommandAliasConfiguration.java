package com.lovetropics.perms;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class CommandAliasConfiguration {
    private static final JsonParser JSON = new JsonParser();

    private static CommandAliasConfiguration instance = new CommandAliasConfiguration(ImmutableMap.of());

    private final ImmutableMap<String, String[]> aliases;

    private CommandAliasConfiguration(ImmutableMap<String, String[]> aliases) {
        this.aliases = aliases;
    }

    public static CommandAliasConfiguration get() {
        return instance;
    }

    public static void setup() {
        Path path = Paths.get("command_aliases.json");
        if (!Files.exists(path)) {
            if (!setupDefaultConfig(path)) return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject root = JSON.parse(reader).getAsJsonObject();
            instance = parse(root);
        } catch (IOException e) {
            LTPermissions.LOGGER.warn("Failed to load command_aliases.json configuration", e);
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

    private static CommandAliasConfiguration parse(JsonObject root) {
        ImmutableMap.Builder<String, String[]> aliases = ImmutableMap.builder();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String alias = entry.getKey();

            JsonElement targetElement = entry.getValue();
            if (targetElement.isJsonArray()) {
                JsonArray targetArray = targetElement.getAsJsonArray();
                String[] target = new String[targetArray.size()];
                for (int i = 0; i < targetArray.size(); i++) {
                    target[i] = targetArray.get(i).getAsString();
                }
                aliases.put(alias, target);
            } else if (targetElement.isJsonPrimitive()) {
                String[] target = new String[] { targetElement.getAsString() };
                aliases.put(alias, target);
            }
        }

        return new CommandAliasConfiguration(aliases.build());
    }

    public Map<String, String[]> getAliases() {
        return this.aliases;
    }
}
