package com.lovetropics.perms.protection.authority.behavior.config;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.perms.protection.authority.behavior.AuthorityBehavior;
import com.lovetropics.perms.protection.authority.behavior.CommandInvokingAuthorityBehavior;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class AuthorityBehaviorConfig {
    public static final Codec<AuthorityBehaviorConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).fieldOf("enter_commands").forGetter(c -> c.enterCommands),
                MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).fieldOf("exit_commands").forGetter(c -> c.exitCommands),
                Codec.BOOL.optionalFieldOf("command_feedback", false).forGetter(c -> c.commandFeedback)
        ).apply(instance, AuthorityBehaviorConfig::new);
    });

    private final String[] enterCommands;
    private final String[] exitCommands;
    private final boolean commandFeedback;

    public AuthorityBehaviorConfig(String[] enterCommands, String[] exitCommands, boolean commandFeedback) {
        this.enterCommands = enterCommands;
        this.exitCommands = exitCommands;
        this.commandFeedback = commandFeedback;
    }

    public AuthorityBehavior createBehavior() {
        if (this.enterCommands.length > 0 || this.exitCommands.length > 0) {
            return new CommandInvokingAuthorityBehavior(this.enterCommands, this.exitCommands, commandFeedback);
        }
        return AuthorityBehavior.EMPTY;
    }
}
