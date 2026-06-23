package ir.practice.pvp.models;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class Arena {

    private final String name;
    private Location corner1, corner2;
    private Location redSpawn, blueSpawn;
    // Each bed has TWO block locations (head + foot)
    private Location redBed1, redBed2;
    private Location blueBed1, blueBed2;
    private int voidLevel   = 0;
    private int heightLimit = 256;
    private int respawnTime = 5;

    private final Map<String, Location> blockWhitelist = new HashMap<String, Location>();

    public Arena(String name) { this.name = name; }

    public String getName()     { return name; }
    public Location getCorner1()   { return corner1; }   public void setCorner1(Location l)   { corner1 = l; }
    public Location getCorner2()   { return corner2; }   public void setCorner2(Location l)   { corner2 = l; }
    public Location getRedSpawn()  { return redSpawn; }  public void setRedSpawn(Location l)  { redSpawn = l; }
    public Location getBlueSpawn() { return blueSpawn; } public void setBlueSpawn(Location l) { blueSpawn = l; }

    // Red bed
    public Location getRedBed1()  { return redBed1; }  public void setRedBed1(Location l)  { redBed1 = l; }
    public Location getRedBed2()  { return redBed2; }  public void setRedBed2(Location l)  { redBed2 = l; }
    // Blue bed
    public Location getBlueBed1() { return blueBed1; } public void setBlueBed1(Location l) { blueBed1 = l; }
    public Location getBlueBed2() { return blueBed2; } public void setBlueBed2(Location l) { blueBed2 = l; }

    /** Returns true if loc matches either part of the red bed */
    public boolean isRedBed(Location loc) {
        return matchesLoc(loc, redBed1) || matchesLoc(loc, redBed2);
    }
    /** Returns true if loc matches either part of the blue bed */
    public boolean isBlueBed(Location loc) {
        return matchesLoc(loc, blueBed1) || matchesLoc(loc, blueBed2);
    }

    private boolean matchesLoc(Location loc, Location target) {
        if (target == null || loc == null) return false;
        return loc.getWorld().equals(target.getWorld())
                && loc.getBlockX() == target.getBlockX()
                && loc.getBlockY() == target.getBlockY()
                && loc.getBlockZ() == target.getBlockZ();
    }

    public int getVoidLevel()   { return voidLevel; }   public void setVoidLevel(int v)   { voidLevel = v; }
    public int getHeightLimit() { return heightLimit; } public void setHeightLimit(int v) { heightLimit = v; }
    public int getRespawnTime() { return respawnTime; } public void setRespawnTime(int v) { respawnTime = v; }

    public Map<String, Location> getBlockWhitelist() { return blockWhitelist; }
    public void addWhitelistBlock(String id, Location loc) { blockWhitelist.put(id, loc); }
    public boolean removeWhitelistBlock(String id) { return blockWhitelist.remove(id) != null; }
    public boolean isWhitelisted(Location loc) {
        for (Location wl : blockWhitelist.values()) {
            if (matchesLoc(loc, wl)) return true;
        }
        return false;
    }

    public boolean isInsideBoundsXZ(Location loc) {
        if (corner1 == null || corner2 == null) return false;
        if (!loc.getWorld().equals(corner1.getWorld())) return false;
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }

    public boolean isInsideBounds(Location loc) {
        if (!isInsideBoundsXZ(loc)) return false;
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        return loc.getBlockY() >= minY && loc.getBlockY() <= maxY;
    }

    public boolean isFullyConfigured() {
        return corner1 != null && corner2 != null
                && redSpawn != null && blueSpawn != null
                && redBed1 != null && redBed2 != null
                && blueBed1 != null && blueBed2 != null;
    }
}
