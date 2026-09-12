package dev.einselbst.ghostdash;

import org.bukkit.Location;
import org.bukkit.World;

record PathPoint(double x, double y, double z, float yaw, float pitch) {
    static PathPoint from(Location location) {
        return new PathPoint(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    Location toLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
