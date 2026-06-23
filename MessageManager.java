package ir.practice.pvp.utils;

import ir.practice.pvp.PracticePvP;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final PracticePvP plugin;
    private FileConfiguration cfg;

    public MessageManager(PracticePvP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key, Map<String, String> ph) {
        String raw = cfg.getString(key, "&c[Missing: " + key + "]");
        if (ph != null) for (Map.Entry<String, String> e : ph.entrySet()) raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String get(String key) { return get(key, null); }

    public int getInt(String key, int def) { return cfg.getInt(key, def); }

    public static Map<String, String> of(String... pairs) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i + 1 < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
        return m;
    }
}
