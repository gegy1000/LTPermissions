package com.lovetropics.perms.override.command;

import com.lovetropics.perms.override.RoleOverrideType;
import com.lovetropics.perms.storage.PlayerRoleStorage;
import com.lovetropics.perms.storage.PlayerRoles;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class CommandPermEvaluator {
    public static PermissionResult canUseCommand(CommandSource source, MatchableCommand command) {
        if (doesBypassPermissions(source)) return PermissionResult.PASS;

        Entity entity = source.getEntity();
        if (entity instanceof PlayerEntity) {
            PlayerRoleStorage storage = PlayerRoleStorage.forServer(source.getServer());
            PlayerRoles roles = storage.getOrNull(entity);
            if (roles != null) {
                return roles.test(RoleOverrideType.COMMANDS, m -> m.test(command));
            }
        }

        return PermissionResult.PASS;
    }

    public static boolean doesBypassPermissions(CommandSource source) {
        return source.hasPermissionLevel(4) || !(source.getEntity() instanceof PlayerEntity);
    }
}
