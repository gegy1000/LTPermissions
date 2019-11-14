package com.lovetropics.perms.override.command;

import com.lovetropics.perms.LTPerms;
import com.lovetropics.perms.capability.PlayerRoles;
import com.lovetropics.perms.override.RoleOverrideType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class CommandPermEvaluator {
    public static PermissionResult canUseCommand(CommandSource source, CommandNode<CommandSource> node) {
        if (doesBypassPermissions(source)) return PermissionResult.PASS;

        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            PlayerRoles roles = entity.getCapability(LTPerms.playerRolesCap()).orElse(null);
            if (roles != null) {
                return roles.test(RoleOverrideType.COMMANDS, m -> m.test(node));
            }
        }

        return PermissionResult.PASS;
    }

    public static boolean doesBypassPermissions(CommandSource source) {
        return source.hasPermissionLevel(4) || !(source.getEntity() instanceof PlayerEntity);
    }
}
