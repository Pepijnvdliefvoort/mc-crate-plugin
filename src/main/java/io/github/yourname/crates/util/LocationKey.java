package io.github.yourname.crates.util;

import org.bukkit.Location;

import java.util.Objects;

public final class LocationKey {
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public LocationKey(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static LocationKey from(Location loc) {
        return new LocationKey(
                Objects.requireNonNull(loc.getWorld()).getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    public String world() { return world; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationKey that)) return false;
        return x == that.x && y == that.y && z == that.z && world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public String toString() {
        return world + ":" + x + "," + y + "," + z;
    }
}
