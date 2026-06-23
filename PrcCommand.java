package ir.practice.pvp.commands;

import ir.practice.pvp.PracticePvP;
import ir.practice.pvp.models.Arena;
import ir.practice.pvp.models.GameTeam;
import ir.practice.pvp.models.LobbyItem;
import ir.practice.pvp.models.Match;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PrcCommand implements CommandExecutor, TabCompleter {

    private final PracticePvP plugin;

    public PrcCommand(PracticePvP plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players."); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission("practicepvp.admin")) { player.sendMessage(ChatColor.RED + "No permission."); return true; }
        if (args.length == 0) { sendHelp(player); return true; }

        String sub = args[0].toLowerCase();

        // ── /prc createarena <name> ──────────────────────────────────────────
        if (sub.equals("createarena") && args.length == 2) {
            if (plugin.getArenaManager().getArena(args[1]) != null) { player.sendMessage(ChatColor.RED + "Already exists."); return true; }
            plugin.getArenaManager().createArena(args[1]);
            player.sendMessage(ChatColor.GREEN + "Arena '" + args[1] + "' created.");
            return true;
        }

        // ── /prc arena list ──────────────────────────────────────────────────
        if (sub.equals("arena") && args.length == 2 && args[1].equalsIgnoreCase("list")) {
            player.sendMessage(ChatColor.GOLD + "══ Arenas ══");
            for (Arena a : plugin.getArenaManager().getArenas()) {
                String status = a.isFullyConfigured() ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Incomplete";
                Match active  = plugin.getMatchManager().getMatchByArena(a.getName());
                player.sendMessage(ChatColor.WHITE + "- " + ChatColor.AQUA + a.getName() + " " + status
                        + (active != null ? ChatColor.YELLOW + " [Active]" : ""));
            }
            return true;
        }

        // ── /prc arena <name> delete ─────────────────────────────────────────
        if (sub.equals("arena") && args.length == 3 && args[2].equalsIgnoreCase("delete")) {
            if (plugin.getMatchManager().getMatchByArena(args[1]) != null) { player.sendMessage(ChatColor.RED + "Match is running!"); return true; }
            player.sendMessage(plugin.getArenaManager().deleteArena(args[1]) ? ChatColor.GREEN + "Deleted." : ChatColor.RED + "Not found.");
            return true;
        }

        // ── /prc setarena <name> <1|2> ───────────────────────────────────────
        if (sub.equals("setarena") && args.length == 3) {
            Arena arena = req(player, args[1]); if (arena == null) return true;
            try {
                int c = Integer.parseInt(args[2]);
                if (c == 1)      { arena.setCorner1(player.getLocation()); player.sendMessage(ChatColor.GREEN + "Corner 1 set."); }
                else if (c == 2) { arena.setCorner2(player.getLocation()); player.sendMessage(ChatColor.GREEN + "Corner 2 set."); }
                else { player.sendMessage(ChatColor.RED + "Use 1 or 2."); return true; }
                plugin.getArenaManager().saveArenas();
            } catch (NumberFormatException e) { player.sendMessage(ChatColor.RED + "Use 1 or 2."); }
            return true;
        }

        // ── /prc arena <name> setvoid|setspawntime|sethightlimit <val> ───────
        if (sub.equals("arena") && args.length == 4) {
            Arena arena = req(player, args[1]); if (arena == null) return true;
            String action = args[2].toLowerCase();
            try {
                int val = Integer.parseInt(args[3]);
                if      (action.equals("setvoid"))        { arena.setVoidLevel(val);   player.sendMessage(ChatColor.GREEN + "Void Y=" + val); }
                else if (action.equals("setspawntime"))   { arena.setRespawnTime(val); player.sendMessage(ChatColor.GREEN + "Respawn=" + val + "s"); }
                else if (action.equals("sethightlimit") || action.equals("setheightlimit")) { arena.setHeightLimit(val); player.sendMessage(ChatColor.GREEN + "Height limit Y=" + val); }
                else { player.sendMessage(ChatColor.RED + "Unknown: setvoid/setspawntime/sethightlimit"); return true; }
                plugin.getArenaManager().saveArenas();
            } catch (NumberFormatException e) { player.sendMessage(ChatColor.RED + "Invalid number."); }
            return true;
        }

        // ── /prc bedset <red|blue> <arena> <part1|part2> ────────────────────
        if (sub.equals("bedset") && args.length == 4) {
            GameTeam team = reqTeam(player, args[1]); if (team == null) return true;
            Arena arena   = req(player, args[2]); if (arena == null) return true;
            Block target  = getTargetBlock(player);
            if (target == null || (target.getType() != Material.BED_BLOCK && target.getType() != Material.BED)) {
                player.sendMessage(ChatColor.RED + "Look at a bed block."); return true;
            }
            String part = args[3].toLowerCase();
            if (part.equals("part1") || part.equals("1")) {
                if (team == GameTeam.RED) arena.setRedBed1(target.getLocation()); else arena.setBlueBed1(target.getLocation());
                player.sendMessage("" + ChatColor.GREEN + team.getChatColor() + team.getDisplayName() + ChatColor.GREEN + " bed part1 set.");
            } else if (part.equals("part2") || part.equals("2")) {
                if (team == GameTeam.RED) arena.setRedBed2(target.getLocation()); else arena.setBlueBed2(target.getLocation());
                player.sendMessage("" + ChatColor.GREEN + team.getChatColor() + team.getDisplayName() + ChatColor.GREEN + " bed part2 set.");
            } else { player.sendMessage(ChatColor.RED + "Use part1 or part2."); return true; }
            plugin.getArenaManager().saveArenas();
            return true;
        }

        // ── /prc spawnset <red|blue> [arena] ─────────────────────────────────
        if (sub.equals("spawnset") && (args.length == 2 || args.length == 3)) {
            GameTeam team = reqTeam(player, args[1]); if (team == null) return true;
            Arena arena   = args.length == 3 ? req(player, args[2]) : getArenaAt(player);
            if (arena == null) { if (args.length == 2) player.sendMessage(ChatColor.RED + "Not in arena. Use /prc spawnset " + args[1] + " <arena>"); return true; }
            if (team == GameTeam.RED) arena.setRedSpawn(player.getLocation()); else arena.setBlueSpawn(player.getLocation());
            plugin.getArenaManager().saveArenas();
            player.sendMessage("" + ChatColor.GREEN + team.getChatColor() + team.getDisplayName() + ChatColor.GREEN + " spawn set.");
            return true;
        }

        // ── /prc <arena> blockwl add|remove <id> ─────────────────────────────
        if (args.length == 4 && args[1].equalsIgnoreCase("blockwl")) {
            Arena arena = req(player, args[0]); if (arena == null) return true;
            String id = args[3];
            if (args[2].equalsIgnoreCase("add")) {
                Block target = getTargetBlock(player);
                if (target == null) { player.sendMessage(ChatColor.RED + "Look at a block."); return true; }
                arena.addWhitelistBlock(id, target.getLocation());
                plugin.getArenaManager().saveArenas();
                player.sendMessage(ChatColor.GREEN + "Whitelisted as '" + id + "'.");
            } else if (args[2].equalsIgnoreCase("remove")) {
                player.sendMessage(arena.removeWhitelistBlock(id) ? ChatColor.GREEN + "Removed '" + id + "'." : ChatColor.RED + "Not found.");
                plugin.getArenaManager().saveArenas();
            }
            return true;
        }

        // ── /prc <arena> stop ─────────────────────────────────────────────────
        if (args.length == 2 && args[1].equalsIgnoreCase("stop")) {
            Arena arena = req(player, args[0]); if (arena == null) return true;
            Match match = plugin.getMatchManager().getMatchByArena(arena.getName());
            if (match == null) { player.sendMessage(ChatColor.RED + "No active match."); return true; }
            plugin.getMatchManager().endMatch(match, null, true);
            player.sendMessage(ChatColor.GREEN + "Match stopped.");
            return true;
        }

        // ── /prc start <arena> <red> <blue> ──────────────────────────────────
        if (sub.equals("start") && args.length == 4) {
            Arena arena = req(player, args[1]); if (arena == null) return true;
            if (!arena.isFullyConfigured()) { player.sendMessage(ChatColor.RED + "Arena not fully configured!"); return true; }
            Player red  = plugin.getServer().getPlayer(args[2]);
            Player blue = plugin.getServer().getPlayer(args[3]);
            if (red  == null) { player.sendMessage(ChatColor.RED + "'" + args[2] + "' not found."); return true; }
            if (blue == null) { player.sendMessage(ChatColor.RED + "'" + args[3] + "' not found."); return true; }
            if (plugin.getMatchManager().isInMatch(red))  { player.sendMessage(ChatColor.RED + red.getName()  + " already in match."); return true; }
            if (plugin.getMatchManager().isInMatch(blue)) { player.sendMessage(ChatColor.RED + blue.getName() + " already in match."); return true; }
            plugin.getMatchManager().startMatch(arena, red, blue);
            player.sendMessage(ChatColor.GREEN + "Match started!");
            return true;
        }

        // ── /prc setlobby ─────────────────────────────────────────────────────
        if (sub.equals("setlobby")) {
            Location loc = player.getLocation();
            plugin.getConfig().set("lobby-world", loc.getWorld().getName());
            plugin.getConfig().set("lobby-spawn.x", loc.getX()); plugin.getConfig().set("lobby-spawn.y", loc.getY());
            plugin.getConfig().set("lobby-spawn.z", loc.getZ()); plugin.getConfig().set("lobby-spawn.yaw", (double) loc.getYaw());
            plugin.getConfig().set("lobby-spawn.pitch", (double) loc.getPitch());
            plugin.saveConfig(); player.sendMessage(ChatColor.GREEN + "Lobby saved."); return true;
        }

        // ── /prc setip <ip> ───────────────────────────────────────────────────
        if (sub.equals("setip") && args.length == 2) {
            plugin.getConfig().set("server-ip", args[1]); plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "IP set to: " + args[1]); return true;
        }

        // ── /prc reload ───────────────────────────────────────────────────────
        if (sub.equals("reload")) { plugin.getMsgManager().reload(); player.sendMessage(ChatColor.GREEN + "Reloaded."); return true; }

        // ── /prc sitem add <material> <slot> ─────────────────────────────────
        if (sub.equals("sitem") && args.length >= 2) {
            return handleSitem(player, args);
        }

        sendHelp(player);
        return true;
    }

    private boolean handleSitem(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /prc sitem <add|delete|action|set|reset> ..."); return true; }
        String action = args[1].toLowerCase();

        // /prc sitem add <material> <slot>
        if (action.equals("add") && args.length == 4) {
            Material mat;
            try { mat = Material.valueOf(args[2].toUpperCase()); }
            catch (Exception e) { player.sendMessage(ChatColor.RED + "Unknown material: " + args[2]); return true; }
            int slot;
            try { slot = Integer.parseInt(args[3]) - 1; }
            catch (Exception e) { player.sendMessage(ChatColor.RED + "Slot must be 1-9."); return true; }
            if (slot < 0 || slot > 8) { player.sendMessage(ChatColor.RED + "Slot must be 1-9."); return true; }
            plugin.getLobbyItemManager().addItem(args[2].toLowerCase(), mat, slot);
            player.sendMessage(plugin.getMsgManager().get("sitem-added",
                    ir.practice.pvp.utils.MessageManager.of("item", args[2], "slot", String.valueOf(slot+1))));
            // Give to all online players
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!plugin.getMatchManager().isInMatch(p)) plugin.getLobbyItemManager().giveItems(p);
            }
            return true;
        }

        // /prc sitem delete <material> <slot>
        if (action.equals("delete") && args.length == 4) {
            int slot;
            try { slot = Integer.parseInt(args[3]) - 1; }
            catch (Exception e) { player.sendMessage(ChatColor.RED + "Slot must be 1-9."); return true; }
            if (plugin.getLobbyItemManager().removeItem(args[2].toLowerCase(), slot)) {
                player.sendMessage(plugin.getMsgManager().get("sitem-removed"));
            } else {
                player.sendMessage(plugin.getMsgManager().get("sitem-not-found"));
            }
            return true;
        }

        // /prc sitem action add <LeftClick|RightClick|AnyClick>
        if (action.equals("action") && args.length == 4 && args[2].equalsIgnoreCase("add")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            LobbyItem.ClickAction ca = LobbyItem.parseAction(args[3]);
            item.setAction(ca);
            plugin.getLobbyItemManager().save();
            player.sendMessage(plugin.getMsgManager().get("sitem-action-set",
                    ir.practice.pvp.utils.MessageManager.of("action", args[3])));
            return true;
        }

        // /prc sitem action reset
        if (action.equals("action") && args.length == 3 && args[2].equalsIgnoreCase("reset")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            item.setAction(LobbyItem.ClickAction.NONE);
            plugin.getLobbyItemManager().save();
            player.sendMessage(plugin.getMsgManager().get("sitem-action-reset"));
            return true;
        }

        // /prc sitem set subtitle <text>
        if (action.equals("set") && args.length >= 4 && args[2].equalsIgnoreCase("subtitle")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            String text = join(args, 3);
            item.setSubtitle(text);
            plugin.getLobbyItemManager().save();
            player.sendMessage(plugin.getMsgManager().get("sitem-subtitle-set"));
            return true;
        }

        // /prc sitem reset subtitle
        if (action.equals("reset") && args.length == 3 && args[2].equalsIgnoreCase("subtitle")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            item.setSubtitle("");
            plugin.getLobbyItemManager().save();
            player.sendMessage(plugin.getMsgManager().get("sitem-subtitle-reset"));
            return true;
        }

        // /prc sitem set actionbar <text>
        if (action.equals("set") && args.length >= 4 && args[2].equalsIgnoreCase("actionbar")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            String text = join(args, 3);
            item.setActionbar(text);
            plugin.getLobbyItemManager().save();
            player.sendMessage(plugin.getMsgManager().get("sitem-actionbar-set"));
            return true;
        }

        // /prc sitem reset actionbar
        if (action.equals("reset") && args.length == 3 && args[2].equalsIgnoreCase("actionbar")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            item.setActionbar("");
            plugin.getLobbyItemManager().save();
            player.sendMessage(plugin.getMsgManager().get("sitem-actionbar-reset"));
            return true;
        }

        // /prc sitem cmd add <command>
        if (action.equals("cmd") && args.length >= 4 && args[2].equalsIgnoreCase("add")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            String cmd = join(args, 3);
            item.setCommand(cmd);
            plugin.getLobbyItemManager().save();
            player.sendMessage(ChatColor.GREEN + "Command set to: /" + cmd);
            return true;
        }

        // /prc sitem cmd reset
        if (action.equals("cmd") && args.length == 3 && args[2].equalsIgnoreCase("reset")) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            item.setCommand("");
            plugin.getLobbyItemManager().save();
            player.sendMessage(ChatColor.GREEN + "Command reset.");
            return true;
        }

        // /prc sitem drop <true|false>
        if (action.equals("drop") && args.length == 3) {
            LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
            if (item == null) { player.sendMessage(plugin.getMsgManager().get("sitem-no-held")); return true; }
            boolean drop = args[2].equalsIgnoreCase("true");
            item.setDroppable(drop);
            plugin.getLobbyItemManager().save();
            player.sendMessage(ChatColor.GREEN + "Drop for this item set to: " + drop);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown sitem action.");
        return true;
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (!(sender instanceof Player)) return out;
        List<String> arenas = new ArrayList<String>(plugin.getArenaManager().getArenaNames());

        if (args.length == 1) {
            for (String s : Arrays.asList("createarena","arena","setarena","spawnset","bedset","start","setlobby","setip","reload","sitem"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            for (String n : arenas) if (n.startsWith(args[0].toLowerCase())) out.add(n);
        } else if (args.length == 2) {
            String s = args[0].toLowerCase();
            if (s.equals("arena"))   { out.add("list"); for (String n : arenas) if (n.startsWith(args[1].toLowerCase())) out.add(n); }
            else if (s.equals("setarena") || s.equals("start")) { for (String n : arenas) if (n.startsWith(args[1].toLowerCase())) out.add(n); }
            else if (s.equals("spawnset") || s.equals("bedset")) { for (String t : Arrays.asList("red","blue")) if (t.startsWith(args[1].toLowerCase())) out.add(t); }
            else if (s.equals("sitem")) { for (String x : Arrays.asList("add","delete","action","set","reset")) if (x.startsWith(args[1].toLowerCase())) out.add(x); }
            else { for (String x : Arrays.asList("stop","blockwl")) if (x.startsWith(args[1].toLowerCase())) out.add(x); }
        } else if (args.length == 3) {
            String s = args[0].toLowerCase();
            if (s.equals("arena"))      { for (String x : Arrays.asList("setvoid","setspawntime","sethightlimit","delete")) if (x.startsWith(args[2].toLowerCase())) out.add(x); }
            else if (s.equals("setarena"))  { out.add("1"); out.add("2"); }
            else if (s.equals("spawnset"))  { for (String n : arenas) if (n.startsWith(args[2].toLowerCase())) out.add(n); }
            else if (s.equals("bedset"))    { for (String n : arenas) if (n.startsWith(args[2].toLowerCase())) out.add(n); }
            else if (s.equals("start"))     { for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) out.add(p.getName()); }
            else if (args[1].equalsIgnoreCase("blockwl")) { out.add("add"); out.add("remove"); }
            else if (s.equals("sitem")) {
                String sa = args[1].toLowerCase();
                if (sa.equals("action")) { out.add("add"); out.add("reset"); }
                else if (sa.equals("set")) { out.add("subtitle"); out.add("actionbar"); }
                else if (sa.equals("reset")) { out.add("subtitle"); out.add("actionbar"); }
            }
        } else if (args.length == 4) {
            String s = args[0].toLowerCase();
            if (s.equals("bedset")) { out.add("part1"); out.add("part2"); }
            else if (s.equals("start")) { for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[3].toLowerCase())) out.add(p.getName()); }
            else if (s.equals("sitem") && args[1].equalsIgnoreCase("action") && args[2].equalsIgnoreCase("add")) {
                for (String x : Arrays.asList("LeftClick","RightClick","AnyClick")) if (x.toLowerCase().startsWith(args[3].toLowerCase())) out.add(x);
            }
        }
        return out;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Arena req(Player p, String name) {
        Arena a = plugin.getArenaManager().getArena(name);
        if (a == null) p.sendMessage(ChatColor.RED + "Arena '" + name + "' not found.");
        return a;
    }
    private GameTeam reqTeam(Player p, String s) {
        GameTeam t = GameTeam.fromString(s);
        if (t == null) p.sendMessage(ChatColor.RED + "Invalid team. Use red or blue.");
        return t;
    }
    private Arena getArenaAt(Player player) {
        for (Arena a : plugin.getArenaManager().getArenas())
            if (a.isInsideBoundsXZ(player.getLocation())) return a;
        return null;
    }
    private Block getTargetBlock(Player player) {
        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().normalize();
        Location loc = player.getEyeLocation().clone();
        for (int i = 0; i < 50; i++) {
            loc.add(dir.clone().multiply(0.1));
            Block b = loc.getBlock();
            if (b.getType() != Material.AIR) return b;
        }
        return null;
    }
    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) { if (i > start) sb.append(" "); sb.append(args[i]); }
        return sb.toString();
    }
    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.GOLD + "══ PracticePvP v2.4 ══");
        p.sendMessage(ChatColor.YELLOW + "/prc createarena <name>" + ChatColor.WHITE + " - Create arena");
        p.sendMessage(ChatColor.YELLOW + "/prc arena list" + ChatColor.WHITE + " - List arenas");
        p.sendMessage(ChatColor.YELLOW + "/prc arena <name> delete" + ChatColor.WHITE + " - Delete arena");
        p.sendMessage(ChatColor.YELLOW + "/prc setarena <name> <1|2>" + ChatColor.WHITE + " - Set corners");
        p.sendMessage(ChatColor.YELLOW + "/prc arena <name> setvoid <Y>" + ChatColor.WHITE + " - Void Y");
        p.sendMessage(ChatColor.YELLOW + "/prc arena <name> sethightlimit <Y>" + ChatColor.WHITE + " - Height limit");
        p.sendMessage(ChatColor.YELLOW + "/prc arena <name> setspawntime <sec>" + ChatColor.WHITE + " - Respawn delay");
        p.sendMessage(ChatColor.YELLOW + "/prc bedset <red|blue> <arena> <part1|part2>" + ChatColor.WHITE + " - Set bed parts");
        p.sendMessage(ChatColor.YELLOW + "/prc spawnset <red|blue> [arena]" + ChatColor.WHITE + " - Set spawn");
        p.sendMessage(ChatColor.YELLOW + "/prc start <arena> <red> <blue>" + ChatColor.WHITE + " - Start match");
        p.sendMessage(ChatColor.YELLOW + "/prc <arena> stop" + ChatColor.WHITE + " - Stop match");
        p.sendMessage(ChatColor.YELLOW + "/prc <arena> blockwl add|remove <id>" + ChatColor.WHITE + " - Whitelist blocks");
        p.sendMessage(ChatColor.YELLOW + "/prc sitem add <material> <slot>" + ChatColor.WHITE + " - Add lobby item");
        p.sendMessage(ChatColor.YELLOW + "/prc sitem delete <material> <slot>" + ChatColor.WHITE + " - Remove lobby item");
        p.sendMessage(ChatColor.YELLOW + "/prc sitem action add <click>" + ChatColor.WHITE + " - Set click action");
        p.sendMessage(ChatColor.YELLOW + "/prc sitem set subtitle <text>" + ChatColor.WHITE + " - Set subtitle");
        p.sendMessage(ChatColor.YELLOW + "/prc sitem set actionbar <text>" + ChatColor.WHITE + " - Set actionbar");
        p.sendMessage(ChatColor.YELLOW + "/prc setlobby / setip <ip> / reload");
        p.sendMessage(ChatColor.YELLOW + "/falldamage <true|false>" + ChatColor.WHITE + " - Toggle fall damage");
        p.sendMessage(ChatColor.YELLOW + "/bf queue / /bf leave" + ChatColor.WHITE + " - Queue");
        p.sendMessage(ChatColor.YELLOW + "/spectate <player>" + ChatColor.WHITE + " - Spectate");
    }
}
