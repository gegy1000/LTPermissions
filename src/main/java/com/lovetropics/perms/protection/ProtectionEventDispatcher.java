package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LTPermissions.ID)
public final class ProtectionEventDispatcher {
    @SubscribeEvent
    public static void onTickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.player;
            FoodStats food = player.getFoodStats();
            if (food.needFood()) {
                ProtectionManager protect = protect(player.getServerWorld());
                EventSource source = EventSource.forEntity(player);
                if (protect.denies(source, ProtectionRule.HUNGER)) {
                    food.setFoodLevel(20);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.BREAK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onTrampleFarmland(BlockEvent.FarmlandTrampleEvent event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
            if (protect.denies(source, ProtectionRule.BREAK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_BLOCKS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT)) {
                event.setCanceled(true);
                return;
            }

            if (protect.denies(source, ProtectionRule.INTERACT_BLOCKS)) {
                event.setUseBlock(Event.Result.DENY);
            }
            if (protect.denies(source, ProtectionRule.INTERACT_ITEMS) || (isBlockItem(event) && protect.denies(source, ProtectionRule.PLACE))) {
                event.setUseItem(Event.Result.DENY);
            }
        }
    }

    private static boolean isBlockItem(PlayerInteractEvent.RightClickBlock event) {
        return event.getItemStack().getItem() instanceof BlockItem;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ProtectionManager protect = protect((ServerWorld) world);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_ITEMS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ProtectionManager protect = protect((ServerWorld) world);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_ENTITIES)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        PlayerEntity player = event.getPlayer();
        Entity target = event.getTarget();
        if (player instanceof ServerPlayerEntity && target.world instanceof ServerWorld) {
            ProtectionManager protect = protect((ServerWorld) target.world);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), target.getPosition());
            if (protect.denies(source, ProtectionRule.ATTACK)) {
                event.setCanceled(true);
            } else if (target instanceof PlayerEntity && protect.denies(source, ProtectionRule.PVP)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.world instanceof ServerWorld) {
            ProtectionManager protect = protect((ServerWorld) entity.world);

            EventSource source = EventSource.forEntity(entity);
            if (protect.denies(source, ProtectionRule.DAMAGE)) {
                event.setCanceled(true);
                return;
            }

            if (event.getSource() == DamageSource.FALL && protect.denies(source, ProtectionRule.FALL_DAMAGE)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onSpawnPortal(BlockEvent.PortalSpawnEvent event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.at(serverWorld, event.getPos());
            if (protect.denies(source, ProtectionRule.PORTALS)) {
                event.setCanceled(true);
            }
        }
    }

    private static ProtectionManager protect(ServerWorld world) {
        return ProtectionManager.get(world.getServer());
    }
}
