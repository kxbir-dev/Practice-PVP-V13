package ir.practice.pvp.commands;

import ir.practice.pvp.PracticePvP;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BfCommand implements CommandExecutor, TabCompleter {

    private final PracticePvP plugin;

    public BfCommand(PracticePvP plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players."); return true; }
        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("queue")) {
            plugin.getQueueManager().joinQueue(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) {
            plugin.getQueueManager().leaveQueue(player);
            return true;
        }
        player.sendMessage("" + ChatColor.YELLOW + "/bf queue " + ChatColor.WHITE + "- Join queue");
        player.sendMessage("" + ChatColor.YELLOW + "/bf leave " + ChatColor.WHITE + "- Leave queue");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("queue", "leave");
        return Collections.emptyList();
    }
}
