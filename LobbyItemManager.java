package ir.practice.pvp.managers;

import ir.practice.pvp.PracticePvP;
import ir.practice.pvp.models.LobbyItem;
import ir.practice.pvp.utils.TitleUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class LobbyItemManager {

    private final PracticePvP plugin;
    private final Map<String, LobbyItem> items = new LinkedHashMap<String, LobbyItem>();
    private File dataFile;

    public LobbyItemManager(PracticePvP plugin) { this.plugin = plugin; load(); }

    public Collection<LobbyItem> getItems() { return items.values(); }

    public LobbyItem getBySlotAndMaterial(int slot, Material mat) {
        for (LobbyItem item : items.values())
            if (item.getSlot() == slot && item.getMaterial() == mat) return item;
        return null;
    }

    public LobbyItem getHeld(Player player) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack held = player.getInventory().getItemInHand();
        if (held == null || held.getType() == Material.AIR) return null;
        return getBySlotAndMaterial(slot, held.getType());
    }

    public boolean addItem(String name, Material material, int slot) {
        items.put(name + "_" + slot, new LobbyItem(name, material, slot));
        save(); return true;
    }

    public boolean removeItem(String name, int slot) {
        if (items.remove(name + "_" + slot) != null) { save(); return true; }
        return false;
    }

    public void giveItems(Player player) {
        for (LobbyItem item : items.values())
            player.getInventory().setItem(item.getSlot(), item.toItemStack());
        player.updateInventory();
    }

    public void showHeldEffects(Player player) {
        LobbyItem item = getHeld(player);
        if (item == null) return;
        if (!item.getSubtitle().isEmpty())
            TitleUtil.sendTitle(player, "", ChatColor.translateAlternateColorCodes('&', item.getSubtitle()), 0, 25, 5);
        if (!item.getActionbar().isEmpty())
            TitleUtil.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', item.getActionbar()));
    }

    public void save() {
        dataFile = new File(plugin.getDataFolder(), "sitem.yml");
        final FileConfiguration cfg = new YamlConfiguration();
        for (LobbyItem item : items.values()) {
            String p = "items." + item.getKey();
            cfg.set(p + ".name",      item.getName());
            cfg.set(p + ".material",  item.getMaterial().name());
            cfg.set(p + ".slot",      item.getSlot());
            cfg.set(p + ".subtitle",  item.getSubtitle());
            cfg.set(p + ".actionbar", item.getActionbar());
            cfg.set(p + ".action",    item.getAction().name());
            cfg.set(p + ".command",   item.getCommand());
            cfg.set(p + ".droppable", item.isDroppable());
        }
        final File file = dataFile;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override public void run() { try { cfg.save(file); } catch (Exception e) { e.printStackTrace(); } }
        });
    }

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "sitem.yml");
        if (!dataFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("items");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String p = "items." + key;
            Material mat;
            try { mat = Material.valueOf(cfg.getString(p + ".material", "STONE").toUpperCase()); }
            catch (Exception e) { mat = Material.STONE; }
            int slot      = cfg.getInt(p + ".slot", 0);
            String name   = cfg.getString(p + ".name", key);
            LobbyItem item = new LobbyItem(name, mat, slot);
            item.setSubtitle(cfg.getString(p + ".subtitle", ""));
            item.setActionbar(cfg.getString(p + ".actionbar", ""));
            item.setAction(LobbyItem.parseAction(cfg.getString(p + ".action", "NONE")));
            item.setCommand(cfg.getString(p + ".command", ""));
            item.setDroppable(cfg.getBoolean(p + ".droppable", false));
            items.put(item.getKey(), item);
        }
    }
}
