package ir.practice.pvp.managers;

import ir.practice.pvp.PracticePvP;
import ir.practice.pvp.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class QueueManager {

    private final PracticePvP plugin;
    private final List<UUID>  waiting = new ArrayList<UUID>();
    private final Random      random  = new Random();

    public QueueManager(PracticePvP plugin) { this.plugin = plugin; }

    public boolean isQueued(Player player) { return waiting.contains(player.getUniqueId()); }

    public boolean joinQueue(Player player) {
        if (plugin.getMatchManager().isInMatch(player)) {
            player.sendMessage(plugin.getMsgManager().get("queue-in-match")); return false;
        }
        if (isQueued(player)) {
            player.sendMessage(plugin.getMsgManager().get("queue-already-in")); return false;
        }
        if (getAvailableArena() == null && waiting.isEmpty()) {
            player.sendMessage(plugin.getMsgManager().get("queue-no-arena")); return false;
        }
        waiting.add(player.getUniqueId());
        player.sendMessage(plugin.getMsgManager().get("queue-joined"));
        player.sendMessage(plugin.getMsgManager().get("queue-leave-hint"));
        tryStartMatch();
        return true;
    }

    public boolean leaveQueue(Player player) {
        if (waiting.remove(player.getUniqueId())) {
            player.sendMessage(plugin.getMsgManager().get("queue-left")); return true;
        }
        player.sendMessage(plugin.getMsgManager().get("queue-not-in")); return false;
    }

    public void removePlayer(UUID uuid) { waiting.remove(uuid); }

    private void tryStartMatch() {
        // Clean offline
        Iterator<UUID> it = waiting.iterator();
        while (it.hasNext()) { Player p = Bukkit.getPlayer(it.next()); if (p == null || !p.isOnline()) it.remove(); }

        if (waiting.size() < 2) return;

        Arena arena = getAvailableArena();
        if (arena == null) {
            for (UUID uuid : waiting) { Player p = Bukkit.getPlayer(uuid); if (p != null) p.sendMessage(plugin.getMsgManager().get("queue-busy")); }
            return;
        }

        UUID u1 = waiting.remove(0);
        UUID u2 = waiting.remove(0);
        Player p1 = Bukkit.getPlayer(u1);
        Player p2 = Bukkit.getPlayer(u2);

        if (p1 == null || !p1.isOnline()) { if (p2 != null) waiting.add(0, u2); tryStartMatch(); return; }
        if (p2 == null || !p2.isOnline()) { waiting.add(0, u1); tryStartMatch(); return; }

        plugin.getMatchManager().startMatch(arena, p1, p2);
    }

    private Arena getAvailableArena() {
        List<Arena> avail = new ArrayList<Arena>();
        for (Arena a : plugin.getArenaManager().getArenas()) {
            if (a.isFullyConfigured() && plugin.getMatchManager().getMatchByArena(a.getName()) == null)
                avail.add(a);
        }
        if (avail.isEmpty()) return null;
        return avail.get(random.nextInt(avail.size()));
    }
}
