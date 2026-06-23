package ir.practice.pvp.managers;

import ir.practice.pvp.PracticePvP;
import ir.practice.pvp.models.Arena;
import ir.practice.pvp.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ArenaManager {

    private final PracticePvP plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<String, Arena>();
    private File arenaFile;

    public ArenaManager(PracticePvP plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    public Arena getArena(String name) { return name == null ? null : arenas.get(name.toLowerCase()); }
    public Collection<Arena> getArenas() { return arenas.values(); }
    public Set<String> getArenaNames()   { return arenas.keySet(); }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(String name) {
        if (arenas.remove(name.toLowerCase()) != null) { saveArenas(); return true; }
        return false;
    }

    public void saveArenas() {
        arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        final FileConfiguration cfg = new YamlConfiguration();

        for (Arena arena : arenas.values()) {
            String p = "arenas." + arena.getName();
            cfg.set(p + ".void-level",   arena.getVoidLevel());
            cfg.set(p + ".height-limit", arena.getHeightLimit());
            cfg.set(p + ".respawn-time", arena.getRespawnTime());
            LocationUtils.saveLocation(cfg, p + ".corner1",    arena.getCorner1());
            LocationUtils.saveLocation(cfg, p + ".corner2",    arena.getCorner2());
            LocationUtils.saveLocation(cfg, p + ".red-spawn",  arena.getRedSpawn());
            LocationUtils.saveLocation(cfg, p + ".blue-spawn", arena.getBlueSpawn());
            // Two-part beds
            LocationUtils.saveLocation(cfg, p + ".red-bed1",   arena.getRedBed1());
            LocationUtils.saveLocation(cfg, p + ".red-bed2",   arena.getRedBed2());
            LocationUtils.saveLocation(cfg, p + ".blue-bed1",  arena.getBlueBed1());
            LocationUtils.saveLocation(cfg, p + ".blue-bed2",  arena.getBlueBed2());
            for (Map.Entry<String, Location> e : arena.getBlockWhitelist().entrySet()) {
                LocationUtils.saveLocation(cfg, p + ".whitelist." + e.getKey(), e.getValue());
            }
        }

        final File file = arenaFile;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() {
                try { cfg.save(file); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void loadArenas() {
        arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenaFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(arenaFile);
        ConfigurationSection sec = cfg.getConfigurationSection("arenas");
        if (sec == null) return;

        for (String name : sec.getKeys(false)) {
            Arena arena = new Arena(name);
            String p = "arenas." + name;
            arena.setVoidLevel(cfg.getInt(p + ".void-level", 0));
            arena.setHeightLimit(cfg.getInt(p + ".height-limit", 256));
            arena.setRespawnTime(cfg.getInt(p + ".respawn-time", 5));
            arena.setCorner1(LocationUtils.loadLocation(cfg, p + ".corner1"));
            arena.setCorner2(LocationUtils.loadLocation(cfg, p + ".corner2"));
            arena.setRedSpawn(LocationUtils.loadLocation(cfg, p + ".red-spawn"));
            arena.setBlueSpawn(LocationUtils.loadLocation(cfg, p + ".blue-spawn"));
            arena.setRedBed1(LocationUtils.loadLocation(cfg, p + ".red-bed1"));
            arena.setRedBed2(LocationUtils.loadLocation(cfg, p + ".red-bed2"));
            arena.setBlueBed1(LocationUtils.loadLocation(cfg, p + ".blue-bed1"));
            arena.setBlueBed2(LocationUtils.loadLocation(cfg, p + ".blue-bed2"));
            ConfigurationSection wl = cfg.getConfigurationSection(p + ".whitelist");
            if (wl != null) {
                for (String id : wl.getKeys(false)) {
                    Location loc = LocationUtils.loadLocation(cfg, p + ".whitelist." + id);
                    if (loc != null) arena.addWhitelistBlock(id, loc);
                }
            }
            arenas.put(name.toLowerCase(), arena);
        }
    }
}
