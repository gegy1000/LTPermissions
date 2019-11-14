package com.lovetropics.perms.modifier.command;

import com.lovetropics.perms.LTPerms;
import com.lovetropics.perms.RoleSet;
import com.lovetropics.perms.capability.PlayerRoleCapability;
import com.lovetropics.perms.modifier.RoleModifierType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class CommandPermEvaluator {
    public static PermissionResult canUseCommand(CommandSource source, CommandNode<CommandSource> node) {
        if (doesBypassPermissions(source)) return PermissionResult.PASS;

        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            RoleSet roleSet = entity.getCapability(LTPerms.playerRoleCap())
                    .map(PlayerRoleCapability::getRoles)
                    .orElse(RoleSet.EMPTY);

            return roleSet.test(RoleModifierType.COMMANDS, m -> m.test(node));
        }

        return PermissionResult.PASS;
    }

    public static boolean doesBypassPermissions(CommandSource source) {
        return source.hasPermissionLevel(4) || !(source.getEntity() instanceof PlayerEntity);
    }
}
