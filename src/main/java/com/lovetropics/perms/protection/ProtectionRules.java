package com.lovetropics.perms.protection;

import com.lovetropics.perms.PermissionResult;
import net.minecraft.nbt.CompoundNBT;

import java.util.EnumMap;
import java.util.Map;

public final class ProtectionRules {
    private final Map<ProtectionRule, PermissionResult> rules = new EnumMap<>(ProtectionRule.class);

    public void put(ProtectionRule rule, PermissionResult result) {
        this.rules.put(rule, result);
    }

    public PermissionResult test(ProtectionRule rule) {
        return this.rules.getOrDefault(rule, PermissionResult.PASS);
    }

    public CompoundNBT write(CompoundNBT root) {
        for (Map.Entry<ProtectionRule, PermissionResult> entry : this.rules.entrySet()) {
            root.putString(entry.getKey().getKey(), entry.getValue().getName());
        }
        return root;
    }

    public static ProtectionRules read(CompoundNBT root) {
        ProtectionRules rules = new ProtectionRules();
        for (String key : root.keySet()) {
            ProtectionRule rule = ProtectionRule.byKey(key);
            if (rule != null) {
                rules.put(rule, PermissionResult.byName(root.getString(key)));
            }
        }
        return rules;
    }
}
