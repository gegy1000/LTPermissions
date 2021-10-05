package com.lovetropics.perms.protection.authority.shape;

import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.perms.protection.EventFilter;
import com.lovetropics.perms.protection.EventSource;
import com.mojang.serialization.Codec;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.function.Function;

public interface AuthorityShape extends EventFilter {
    CodecRegistry<String, Codec<? extends AuthorityShape>> REGISTRY = CodecRegistry.stringKeys();

    Codec<AuthorityShape> CODEC = REGISTRY.dispatchStable(AuthorityShape::getCodec, Function.identity());

    static void register() {
        register("universe", UniverseShape.CODEC);
        register("dimension", DimensionShape.CODEC);
        register("box", BoxShape.CODEC);
        register("union", UnionShape.CODEC);
        register("transformed", TransformedShape.CODEC);
        register("cylinder", CylinderShape.CODEC);
        register("polygon", PolygonShape.CODEC);
    }

    static void register(String key, Codec<? extends AuthorityShape> codec) {
        REGISTRY.register(key, codec);
    }

    @Override
    boolean accepts(EventSource source);

    @Nullable
    default Region tryIntoRegion(MinecraftServer server) {
        return null;
    }

    Codec<? extends AuthorityShape> getCodec();
}
