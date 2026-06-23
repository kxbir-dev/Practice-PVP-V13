package ir.practice.pvp.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@SuppressWarnings("deprecation")
public class BlockSnapshot {

    private final int worldX, worldY, worldZ;
    private final String worldName;
    private final Material material;
    private final byte data;

    public BlockSnapshot(Block block) {
        Location loc  = block.getLocation();
        this.worldName = loc.getWorld().getName();
        this.worldX   = loc.getBlockX();
        this.worldY   = loc.getBlockY();
        this.worldZ   = loc.getBlockZ();
        this.material = block.getType();
        this.data     = block.getData();
    }

    public void restore() {
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) return;
        Block block = world.getBlockAt(worldX, worldY, worldZ);
        block.setTypeIdAndData(material.getId(), data, false);
    }

    public Material getMaterial() { return material; }
    public byte getData()         { return data; }
    public int getWorldY()        { return worldY; }
    public String getWorldName()  { return worldName; }
}
