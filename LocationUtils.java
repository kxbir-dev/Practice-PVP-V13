package ir.practice.pvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class LocationUtils {

    public static void saveLocation(ConfigurationSection section, String key, Location loc) {
        if (loc == null) return;
        ConfigurationSection s = section.createSection(key);
        s.set("world", loc.getWorld().getName());
        s.set("x", loc.getX());
        s.set("y", loc.getY());
        s.set("z", loc.getZ());
        s.set("yaw", (double) loc.getYaw());
        s.set("pitch", (double) loc.getPitch());
    }

    public static Location loadLocation(ConfigurationSection section, String key) {
        if (section == null) return null;
        ConfigurationSection s = section.getConfigurationSection(key);
        if (s == null) return null;
        String worldName = s.getString("world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) return null;
        return new Location(
                Bukkit.getWorld(worldName),
                s.getDouble("x"),
                s.getDouble("y"),
                s.getDouble("z"),
                (float) s.getDouble("yaw"),
                (float) s.getDouble("pitch")
        );
    }
}
