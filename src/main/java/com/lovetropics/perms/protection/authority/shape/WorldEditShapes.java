package com.lovetropics.perms.protection.authority.shape;

import com.sk89q.worldedit.forge.ForgeWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.TransformRegion;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public final class WorldEditShapes {
    @Nullable
    public static RegionSelector tryIntoRegionSelector(MinecraftServer server, AuthorityShape shape) {
        Region region = shape.tryIntoRegion(server);
        if (region != null) {
            if (region instanceof CuboidRegion) {
                return new CuboidRegionSelector(region.getWorld(), region.getMinimumPoint(), region.getMaximumPoint());
            } else if (region instanceof CylinderRegion) {
                CylinderRegion cylinder = (CylinderRegion) region;
                return new CylinderRegionSelector(
                        region.getWorld(),
                        cylinder.getCenter().toVector2().toBlockPoint(), cylinder.getRadius(),
                        cylinder.getMinimumY(), cylinder.getMaximumY()
                );
            } else if (region instanceof Polygonal2DRegion) {
                Polygonal2DRegion polygon = (Polygonal2DRegion) region;
                return new Polygonal2DRegionSelector(
                        polygon.getWorld(), polygon.getPoints(),
                        polygon.getMinimumY(), polygon.getMaximumY()
                );
            }
        }

        return null;
    }

    @Nullable
    public static AuthorityShape tryFromRegion(Region region) {
        if (region instanceof CuboidRegion) {
            return BoxShape.fromRegion(region);
        } else if (region instanceof RegionIntersection) {
            return UnionShape.fromRegion((RegionIntersection) region);
        } else if (region instanceof TransformRegion) {
            return TransformedShape.fromRegion((TransformRegion) region);
        } else if (region instanceof CylinderRegion) {
            return CylinderShape.fromRegion((CylinderRegion) region);
        } else if (region instanceof Polygonal2DRegion) {
            return PolygonShape.fromRegion((Polygonal2DRegion) region);
        }

        List<BlockVector2> points = region.polygonize(-1);
        if (points.size() == 4) {
            return BoxShape.fromRegion(region);
        } else if (points.size() >= 3) {
            return PolygonShape.fromRegion(region, points);
        }

        return null;
    }

    public static RegistryKey<World> asDimension(com.sk89q.worldedit.world.World world) {
        ForgeWorld forgeWorld = (ForgeWorld) world;
        return forgeWorld.getWorld().getDimensionKey();
    }
}
