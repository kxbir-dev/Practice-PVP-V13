package ir.practice.pvp.models;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public enum GameTeam {
    RED(ChatColor.RED,  DyeColor.RED,  "Red"),
    BLUE(ChatColor.BLUE, DyeColor.BLUE, "Blue");

    private final ChatColor chatColor;
    private final DyeColor  dyeColor;
    private final String    displayName;

    GameTeam(ChatColor c, DyeColor d, String n) { chatColor = c; dyeColor = d; displayName = n; }

    public ChatColor getChatColor()  { return chatColor; }
    public DyeColor  getDyeColor()   { return dyeColor; }
    public String    getDisplayName(){ return displayName; }

    public ItemStack[] getKit() {
        ItemStack sword   = makeUnbreakable(new ItemStack(Material.WOOD_SWORD));
        ItemStack pickaxe = makeUnbreakable(enchant(new ItemStack(Material.WOOD_PICKAXE), Enchantment.DIG_SPEED, 1));
        ItemStack axe     = makeUnbreakable(enchant(new ItemStack(Material.WOOD_AXE),     Enchantment.DIG_SPEED, 1));
        ItemStack shears  = makeUnbreakable(new ItemStack(Material.SHEARS));
        @SuppressWarnings("deprecation")
        ItemStack wool    = new ItemStack(Material.WOOL, 64, dyeColor.getWoolData());
        return new ItemStack[]{sword, pickaxe, axe, shears, wool};
    }

    public ItemStack[] getArmorKit() {
        return new ItemStack[]{
            makeUnbreakable(colorLeather(new ItemStack(Material.LEATHER_HELMET))),
            makeUnbreakable(colorLeather(new ItemStack(Material.LEATHER_CHESTPLATE))),
            makeUnbreakable(colorLeather(new ItemStack(Material.LEATHER_LEGGINGS))),
            makeUnbreakable(colorLeather(new ItemStack(Material.LEATHER_BOOTS)))
        };
    }

    private ItemStack enchant(ItemStack i, Enchantment e, int l) { i.addUnsafeEnchantment(e, l); return i; }

    private ItemStack colorLeather(ItemStack item) {
        LeatherArmorMeta m = (LeatherArmorMeta) item.getItemMeta();
        m.setColor(dyeColor.getColor());
        item.setItemMeta(m);
        return item;
    }

    /**
     * Sets Unbreakable:1 via NMS NBT (Spigot 1.8).
     */
    @SuppressWarnings({"deprecation","unchecked"})
    public static ItemStack makeUnbreakable(ItemStack item) {
        try {
            String pkg = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
            // CraftItemStack
            Class<?> craftClass = Class.forName(pkg + ".inventory.CraftItemStack");
            java.lang.reflect.Method asNMS   = craftClass.getMethod("asNMSCopy", ItemStack.class);
            Object nmsStack = asNMS.invoke(null, item);

            // Get NMS package
            String nmsPkg = nmsStack.getClass().getPackage().getName();
            Class<?> nbtClass = Class.forName(nmsPkg + ".NBTTagCompound");

            // Get or create tag
            java.lang.reflect.Method hasTag = nmsStack.getClass().getMethod("hasTag");
            java.lang.reflect.Method getTag = nmsStack.getClass().getMethod("getTag");
            java.lang.reflect.Method setTag = nmsStack.getClass().getMethod("setTag", nbtClass);

            Object tag;
            if ((Boolean) hasTag.invoke(nmsStack)) {
                tag = getTag.invoke(nmsStack);
            } else {
                tag = nbtClass.newInstance();
            }

            // tag.setInt("Unbreakable", 1)
            nbtClass.getMethod("setInt", String.class, int.class).invoke(tag, "Unbreakable", 1);
            setTag.invoke(nmsStack, tag);

            // Convert back
            java.lang.reflect.Method asBukkit = craftClass.getMethod("asBukkitCopy", nmsStack.getClass());
            return (ItemStack) asBukkit.invoke(null, nmsStack);
        } catch (Exception e) {
            return item; // fallback
        }
    }

    public static GameTeam fromString(String s) {
        if (s == null) return null;
        if (s.equalsIgnoreCase("red"))  return RED;
        if (s.equalsIgnoreCase("blue")) return BLUE;
        return null;
    }
}
