package ir.practice.pvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Reads prefix from LuckPrefix's config.yml directly.
 * Config structure:
 *   Groups:
 *     <groupname>:
 *       TabFormat: "&7[Member] "
 * Player's group is read from LuckPrefix's playerdata files.
 */
public class LuckPrefixHook {

    private static Plugin lpPlugin   = null;
    private static File   lpConfig   = null;
    private static File   lpDataFolder = null;

    public static void init() {
        Plugin lp = Bukkit.getPluginManager().getPlugin("LuckPrefix");
        if (lp == null) {
            Bukkit.getLogger().info("[PracticePvP] LuckPrefix not found.");
            return;
        }
        lpPlugin     = lp;
        lpConfig     = new File(lp.getDataFolder(), "config.yml");
        lpDataFolder = lp.getDataFolder();
        Bukkit.getLogger().info("[PracticePvP] LuckPrefix hooked. Config: " + lpConfig.getPath());
    }

    public static boolean isAvailable() {
        return lpPlugin != null && lpConfig != null && lpConfig.exists();
    }

    /**
     * Returns the TabFormat prefix for this player from LuckPrefix config.yml.
     * Falls back to "default" group if player's group is not found.
     */
    public static String getTabPrefix(Player player) {
        if (!isAvailable()) return "";
        try {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(lpConfig);
            String group = getPlayerGroup(player, cfg);
            // Try TabFormat first, then Prefix
            String prefix = cfg.getString("Groups." + group + ".TabFormat", null);
            if (prefix == null) prefix = cfg.getString("Groups." + group + ".Prefix", "");
            // Fallback to default group
            if (prefix.isEmpty() && !group.equals("default")) {
                prefix = cfg.getString("Groups.default.TabFormat",
                         cfg.getString("Groups.default.Prefix", ""));
            }
            return ChatColor.translateAlternateColorCodes('&', prefix);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Determines the player's group by:
     * 1. Calling LuckPrefix API via reflection
     * 2. Reading playerdata YAML files
     * 3. Falling back to "default"
     */
    private static String getPlayerGroup(Player player, FileConfiguration cfg) {
        // --- Try API first ---
        if (lpPlugin != null) {
            for (String methodName : new String[]{"getGroup","getPlayerGroup","getRank"}) {
                try {
                    java.lang.reflect.Method m;
                    try {
                        m = lpPlugin.getClass().getMethod(methodName, Player.class);
                        Object r = m.invoke(lpPlugin, player);
                        if (r != null && !r.toString().isEmpty()) return r.toString().toLowerCase();
                    } catch (NoSuchMethodException e2) {
                        m = lpPlugin.getClass().getMethod(methodName, String.class);
                        Object r = m.invoke(lpPlugin, player.getName());
                        if (r != null && !r.toString().isEmpty()) return r.toString().toLowerCase();
                    }
                } catch (Exception ignored) {}
            }
        }

        // --- Try playerdata files ---
        if (lpDataFolder != null) {
            // Common locations LuckPrefix stores player data
            String[] paths = {
                "playerdata/" + player.getUniqueId() + ".yml",
                "playerdata/" + player.getName() + ".yml",
                "players/" + player.getUniqueId() + ".yml",
                "players/" + player.getName() + ".yml",
                "data/" + player.getUniqueId() + ".yml",
                "data/" + player.getName() + ".yml",
            };
            for (String path : paths) {
                File f = new File(lpDataFolder, path);
                if (f.exists()) {
                    FileConfiguration pd = YamlConfiguration.loadConfiguration(f);
                    for (String key : new String[]{"group","Group","rank","Rank","permission","Permission"}) {
                        String val = pd.getString(key);
                        if (val != null && !val.isEmpty()) return val.toLowerCase();
                    }
                }
            }
        }

        // --- Fallback: check which groups exist and use first non-default ---
        return "default";
    }
}
