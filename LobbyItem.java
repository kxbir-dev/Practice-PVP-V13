package ir.practice.pvp.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LobbyItem {

    public enum ClickAction { LEFT, RIGHT, ANY, NONE }

    private final String   name;
    private final Material material;
    private final int      slot;
    private String      subtitle   = "";
    private String      actionbar  = "";
    private ClickAction action     = ClickAction.NONE;
    private String      command    = "";
    private boolean     droppable  = false; // default: cannot drop

    public LobbyItem(String name, Material material, int slot) {
        this.name     = name;
        this.material = material;
        this.slot     = slot;
    }

    public String      getName()      { return name; }
    public Material    getMaterial()  { return material; }
    public int         getSlot()      { return slot; }
    public String      getSubtitle()  { return subtitle; }
    public String      getActionbar() { return actionbar; }
    public ClickAction getAction()    { return action; }
    public String      getCommand()   { return command; }
    public boolean     isDroppable()  { return droppable; }

    public void setSubtitle(String s)   { subtitle  = s; }
    public void setActionbar(String s)  { actionbar = s; }
    public void setAction(ClickAction a){ action    = a; }
    public void setCommand(String c)    { command   = c; }
    public void setDroppable(boolean d) { droppable = d; }

    public ItemStack toItemStack() { return new ItemStack(material, 1); }

    public String getKey() { return name + "_" + slot; }

    public static ClickAction parseAction(String s) {
        if (s == null) return ClickAction.NONE;
        switch (s.toUpperCase()) {
            case "LEFTCLICK": case "LEFT":  return ClickAction.LEFT;
            case "RIGHTCLICK": case "RIGHT": return ClickAction.RIGHT;
            case "ANYCLICK": case "ANY":    return ClickAction.ANY;
            default: return ClickAction.NONE;
        }
    }
}
