package com.lovetropics.perms.override;

public final class ProtectionBypassOverride implements RoleOverride {
    private final boolean bypass;

    public ProtectionBypassOverride(boolean bypass) {
        this.bypass = bypass;
    }

    public boolean isBypass() {
        return this.bypass;
    }
}
