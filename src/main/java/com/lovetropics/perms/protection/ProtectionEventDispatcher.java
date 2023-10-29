package com.lovetropics.perms.protection;

import com.lovetropics.perms.LTPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
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
                ProtectionManager protect = protect(player.serverLevel());
                EventSource source = EventSource.forEntity(player);
                if (protect.denies(source, ProtectionRule.HUNGER)) {
                    food.setFoodLevel(20);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(event.getPlayer(), event.getPos());
            if (protect.denies(source, ProtectionRule.BREAK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onTrampleFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
            if (protect.denies(source, ProtectionRule.BREAK)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_BLOCKS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
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
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_ITEMS)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(event.getEntity(), event.getPos());
            if (protect.denies(source, ProtectionRule.INTERACT) || protect.denies(source, ProtectionRule.INTERACT_ENTITIES)) {
                event.setCanceled(true);
                return;
            }

            if (event.getTarget() instanceof ItemFrame && protect.denies(source, ProtectionRule.MODIFY_ITEM_FRAMES, ProtectionRule.MODIFY)) {
                event.setCanceled(true);
            } else if (event.getTarget() instanceof ArmorStand && protect.denies(source, ProtectionRule.MODIFY_ARMOR_STANDS, ProtectionRule.MODIFY)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (event.getEntity() instanceof ServerPlayer player && target.level() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.forEntityAt(player, target.blockPosition());
            if (protect.denies(source, ProtectionRule.ATTACK)) {
                event.setCanceled(true);
                return;
            } else if (target instanceof Player && protect.denies(source, ProtectionRule.PVP)) {
                event.setCanceled(true);
                return;
            }

            if (event.getTarget() instanceof ItemFrame && protect.denies(source, ProtectionRule.MODIFY_ITEM_FRAMES, ProtectionRule.MODIFY)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);

            EventSource source = EventSource.forEntity(entity);
            if (protect.denies(source, ProtectionRule.DAMAGE)) {
                event.setCanceled(true);
                return;
            }

            if (entity instanceof Player && protect.denies(source, ProtectionRule.PLAYER_DAMAGE)) {
                event.setCanceled(true);
                return;
            }

            if (event.getSource().is(DamageTypeTags.IS_FALL) && protect.denies(source, ProtectionRule.FALL_DAMAGE)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onSpawnPortal(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ProtectionManager protect = protect(level);
            EventSource source = EventSource.at(level, event.getPos());
            if (protect.denies(source, ProtectionRule.PORTALS)) {
                event.setCanceled(true);
            }
        }
    }

    public static boolean onEditSign(final ServerPlayer player, final BlockPos pos) {
        final ServerLevel level = player.serverLevel();
        final ProtectionManager protect = protect(level);
        final EventSource source = EventSource.forEntityAt(player, pos);
        return protect.denies(source, ProtectionRule.MODIFY_SIGNS, ProtectionRule.MODIFY);
    }

    public static boolean onRemoveBookFromLectern(final ServerPlayer player) {
        final ProtectionManager protect = protect(player.serverLevel());
        // TODO: The position checked here isn't quite correct, as we don't have the context for where the Lectern is
        final EventSource source = EventSource.forEntity(player);
        return protect.denies(source, ProtectionRule.MODIFY_LECTERNS, ProtectionRule.MODIFY);
    }

    private static ProtectionManager protect(ServerLevel level) {
        return ProtectionManager.get(level.getServer());
    }
}
