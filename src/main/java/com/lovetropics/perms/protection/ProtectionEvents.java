package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import com.lovetropics.perms.PermissionResult;
import com.lovetropics.perms.role.RoleReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.FoodStats;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class ProtectionEvents {
    @SubscribeEvent
    public static void onTickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            FoodStats food = player.getFoodStats();
            if (food.needFood()) {
                ProtectionManager regions = getRegions(player.getServerWorld());
                if (regions.test(player.world, player.getPosition(), ProtectionRule.HUNGER) == PermissionResult.DENY) {
                    food.setFoodLevel(20);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld && !doesBypassProtection(event.getPlayer())) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager regions = getRegions(serverWorld);
            PermissionResult result = regions.test(serverWorld, event.getPos(), ProtectionRule.BREAK);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onTrampleFarmland(BlockEvent.FarmlandTrampleEvent event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld && !doesBypassProtection(event.getEntity())) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager regions = getRegions(serverWorld);
            PermissionResult result = regions.test(serverWorld, event.getPos(), ProtectionRule.BREAK);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld && !doesBypassProtection(event.getPlayer())) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager regions = getRegions(serverWorld);
            PermissionResult result = regions.test(serverWorld, event.getPos(), ProtectionRule.INTERACT);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld && !doesBypassProtection(event.getPlayer())) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager regions = getRegions(serverWorld);
            PermissionResult result = regions.test(serverWorld, event.getPos(), ProtectionRule.INTERACT);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld && !doesBypassProtection(event.getPlayer())) {
            ProtectionManager regions = getRegions((ServerWorld) world);
            PermissionResult result = regions.test(event.getWorld(), event.getPos(), ProtectionRule.INTERACT);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld && !doesBypassProtection(event.getPlayer())) {
            ProtectionManager regions = getRegions((ServerWorld) world);
            PermissionResult result = regions.test(event.getWorld(), event.getPos(), ProtectionRule.INTERACT_ENTITIES);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        PlayerEntity player = event.getPlayer();
        Entity target = event.getTarget();
        if (player instanceof ServerPlayerEntity && target.world instanceof ServerWorld && !doesBypassProtection(player)) {
            ProtectionManager regions = getRegions((ServerWorld) target.world);
            PermissionResult result = regions.test(target.world, target.getPosition(), ProtectionRule.ATTACK);
            if (result == PermissionResult.DENY) {
                event.setCanceled(true);
            }
        }
    }

    private static ProtectionManager getRegions(ServerWorld world) {
        return ProtectionManager.get(world.getServer());
    }

    private static boolean doesBypassProtection(Entity player) {
        if (!(player instanceof ServerPlayerEntity)) return false;
        if (player.hasPermissionLevel(4)) return true;

        RoleReader roles = LTPermissions.lookup().byEntity(player);
        return roles.overrides().test(LTPermissions.BYPASS_PROTECTION);
    }
}
