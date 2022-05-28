package com.lovetropics.perms.protection.authority.behavior.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.perms.LTPermissions;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class AuthorityBehaviorConfigs {
    private static final Logger LOGGER = LogManager.getLogger(AuthorityBehaviorConfigs.class);

    public static final CodecRegistry<ResourceLocation, AuthorityBehaviorConfig> REGISTRY = CodecRegistry.resourceLocationKeys();

    private static final JsonParser PARSER = new JsonParser();

    private static final AtomicBoolean RELOADED = new AtomicBoolean();

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                REGISTRY.clear();

                Collection<ResourceLocation> locations = resourceManager.listResources("authority_behaviors", file -> file.endsWith(".json"));
                for (ResourceLocation location : locations) {
                    ResourceLocation id = getIdFromLocation(location);

                    try (Resource resource = resourceManager.getResource(location)) {
                        DataResult<AuthorityBehaviorConfig> result = loadConfig(resource);
                        result.result().ifPresent(config -> REGISTRY.register(id, config));

                        result.error().ifPresent(error -> {
                            LOGGER.error("Failed to load game authority behavior at {}: {}", location, error);
                        });
                    } catch (Exception e) {
                        LOGGER.error("Failed to load authority behavior config at {}", location, e);
                    }
                }

                RELOADED.set(true);
            }, backgroundExecutor);

            return future.thenCompose(stage::wait);
        });
    }

    private static DataResult<AuthorityBehaviorConfig> loadConfig(Resource resource) throws IOException {
        try (InputStream input = resource.getInputStream()) {
            JsonElement json = PARSER.parse(new BufferedReader(new InputStreamReader(input)));
            return AuthorityBehaviorConfig.CODEC.parse(JsonOps.INSTANCE, json);
        }
    }

    private static ResourceLocation getIdFromLocation(ResourceLocation location) {
        String path = location.getPath();
        String name = path.substring("authority_behaviors/".length(), path.length() - ".json".length());
        return new ResourceLocation(location.getNamespace(), name);
    }

    public static boolean hasReloaded() {
        return RELOADED.compareAndSet(true, false);
    }
}
