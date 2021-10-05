package com.lovetropics.perms.protection;

import com.lovetropics.perms.util.ObjectPool;
import com.lovetropics.perms.util.PooledObject;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.UnaryOperator;

public final class EventSource extends PooledObject<EventSource> {
    private static final ObjectPool<EventSource> POOL = ObjectPool.create(16, EventSource::new);

    private static final EventSource GLOBAL = new EventSource(null);

    private RegistryKey<World> dimension;
    private BlockPos pos;
    private Entity entity;

    private EventSource(ObjectPool<EventSource> pool) {
        super(pool);
    }

    void set(RegistryKey<World> dimension, BlockPos pos, Entity entity) {
        this.dimension = dimension;
        this.pos = pos;
        this.entity = entity;
    }

    public static EventSource global() {
        return GLOBAL;
    }

    public static EventSource at(World world, BlockPos pos) {
        return acquire(world.getDimensionKey(), pos, null);
    }

    public static EventSource at(RegistryKey<World> dimension, BlockPos pos) {
        return acquire(dimension, pos, null);
    }

    public static EventSource allOf(World world) {
        return acquire(world.getDimensionKey(), null, null);
    }

    public static EventSource allOf(RegistryKey<World> dimension) {
        return acquire(dimension, null, null);
    }

    public static EventSource forEntity(Entity entity) {
        return acquire(entity.world.getDimensionKey(), entity.getPosition(), entity);
    }

    public static EventSource forEntityAt(Entity entity, BlockPos pos) {
        return acquire(entity.world.getDimensionKey(), pos, entity);
    }

    public static EventSource transform(EventSource source, UnaryOperator<BlockPos> transform) {
        return acquire(source.dimension, source.pos != null ? transform.apply(source.pos) : null, source.entity);
    }

    static EventSource acquire(RegistryKey<World> dimension, BlockPos pos, @Nullable Entity entity) {
        EventSource source = POOL.acquire();
        source.set(dimension, pos, entity);
        return source;
    }

    @Nullable
    public RegistryKey<World> getDimension() {
        return this.dimension;
    }

    @Nullable
    public BlockPos getPos() {
        return this.pos;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    protected void release() {
        this.set(null, null, null);
    }
}
