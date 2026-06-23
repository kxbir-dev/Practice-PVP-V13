package ir.practice.pvp.commands;

import ir.practice.pvp.PracticePvP;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class FallDamageCommand implements CommandExecutor, TabCompleter {

    private final PracticePvP plugin;

    public FallDamageCommand(PracticePvP plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("practicepvp.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /falldamage <true|false>");
            return true;
        }
        if (args[0].equalsIgnoreCase("true")) {
            plugin.setFallDamageEnabled(true);
            sender.sendMessage(plugin.getMsgManager().get("falldamage-enabled"));
        } else if (args[0].equalsIgnoreCase("false")) {
            plugin.setFallDamageEnabled(false);
            sender.sendMessage(plugin.getMsgManager().get("falldamage-disabled"));
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /falldamage <true|false>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Arrays.asList("true", "false") : java.util.Collections.<String>emptyList();
    }
}
