package com.lovetropics.perms.protection;

import com.lovetropics.perms.protection.scope.ProtectionScope;
import net.minecraft.nbt.CompoundNBT;

public final class ProtectionRegion implements Comparable<ProtectionRegion> {
    public final String key;
    public final int level;
    public final ProtectionScope scope;
    public final ProtectionRules rules;

    public ProtectionRegion(String key, int level, ProtectionScope scope, ProtectionRules rules) {
        this.key = key;
        this.level = level;
        this.scope = scope;
        this.rules = rules;
    }

    public ProtectionRegion(String key, ProtectionScope scope, int level) {
        this(key, level, scope, new ProtectionRules());
    }

    public CompoundNBT write(CompoundNBT root) {
        root.putString("key", this.key);
        root.putInt("level", this.level);
        root.put("scope", this.scope.write(new CompoundNBT()));
        root.put("rules", this.rules.write(new CompoundNBT()));
        return root;
    }

    public static ProtectionRegion read(CompoundNBT root){
        String key = root.getString("key");
        int level = root.getInt("level");
        ProtectionScope scope = ProtectionScope.read(root.getCompound("scope"));
        ProtectionRules rules = ProtectionRules.read(root.getCompound("rules"));
        return new ProtectionRegion(key, level, scope, rules);
    }

    @Override
    public int compareTo(ProtectionRegion other) {
        return Integer.compare(other.level, this.level);
    }
}
