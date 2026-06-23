package ir.practice.pvp.commands;

import ir.practice.pvp.PracticePvP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpectateCommand implements CommandExecutor, TabCompleter {

    private final PracticePvP plugin;

    public SpectateCommand(PracticePvP plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this."); return true; }
        Player player = (Player) sender;

        if (args.length == 0) {
            // Leave spectate
            if (plugin.getMatchManager().getSpectatedMatch(player) != null) {
                plugin.getMatchManager().removeSpectator(player);
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /spectate <playername>");
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot spectate yourself.");
            return true;
        }

        plugin.getMatchManager().addSpectator(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getMatchManager().isInMatch(p) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(p.getName());
                }
            }
        }
        return result;
    }
}
