package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
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
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) {
            FoodData food = player.getFoodData();
            if (food.needsFood()) {
                ProtectionManager protect = protect(player.getLevel());
                EventSource source = EventSource.forEntity(player);
                if (protect.denies(source, ProtectionRule.HUNGER)) {
                    food.setFoodLevel(20);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel serverWorld) {
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.BREAK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onTrampleFarmland(BlockEvent.FarmlandTrampleEvent event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel serverWorld) {
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
            if (protect.denies(source, ProtectionRule.BREAK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel serverWorld) {
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_BLOCKS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel serverWorld) {
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
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel) {
            ProtectionManager protect = protect((ServerLevel) world);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_ITEMS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel) {
            ProtectionManager protect = protect((ServerLevel) world);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_ENTITIES)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getTarget();
        if (player instanceof ServerPlayer && target.level instanceof ServerLevel) {
            ProtectionManager protect = protect((ServerLevel) target.level);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), target.blockPosition());
            if (protect.denies(source, ProtectionRule.ATTACK)) {
                event.setCanceled(true);
            } else if (target instanceof Player && protect.denies(source, ProtectionRule.PVP)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent event) {
        if (event.getSource().isBypassInvul()) {
            return;
        }

        LivingEntity entity = event.getEntityLiving();
        if (entity.level instanceof ServerLevel) {
            ProtectionManager protect = protect((ServerLevel) entity.level);

            EventSource source = EventSource.forEntity(entity);
            if (protect.denies(source, ProtectionRule.DAMAGE)) {
                event.setCanceled(true);
                return;
            }

            if (entity instanceof Player && protect.denies(source, ProtectionRule.PLAYER_DAMAGE)) {
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
        LevelAccessor world = event.getWorld();
        if (world instanceof ServerLevel serverWorld) {
            ProtectionManager protect = protect(serverWorld);
            EventSource source = EventSource.at(serverWorld, event.getPos());
            if (protect.denies(source, ProtectionRule.PORTALS)) {
                event.setCanceled(true);
            }
        }
    }

    private static ProtectionManager protect(ServerLevel world) {
        return ProtectionManager.get(world.getServer());
    }
}
