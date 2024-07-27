package com.lovetropics.perms.mixin;

import com.lovetropics.perms.CommandAliasConfiguration;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    @Inject(method = "lambda$loadResources$5", at = @At("HEAD"))
    private static void beforeLoadResources(FeatureFlagSet featureFlags, Commands.CommandSelection commandSelection, int functionCompilationLevel, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor, LayeredRegistryAccess<RegistryLayer> registryAccess, CallbackInfoReturnable<CompletionStage<?>> ci) {
        CommandAliasConfiguration.setResourceManager(resourceManager);
    }

    @Inject(method = "lambda$loadResources$5", at = @At("TAIL"))
    private static void afterLoadResources(FeatureFlagSet featureFlags, Commands.CommandSelection commandSelection, int functionCompilationLevel, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor, LayeredRegistryAccess<RegistryLayer> registryAccess, CallbackInfoReturnable<CompletionStage<?>> ci) {
        CommandAliasConfiguration.clearResourceManager();
    }
}
