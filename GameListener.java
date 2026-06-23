package ir.practice.pvp.listeners;

import ir.practice.pvp.PracticePvP;
import ir.practice.pvp.models.Arena;
import ir.practice.pvp.models.GameTeam;
import ir.practice.pvp.models.LobbyItem;
import ir.practice.pvp.models.Match;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

public class GameListener implements Listener {

    private final PracticePvP plugin;

    public GameListener(PracticePvP plugin) { this.plugin = plugin; }

    // ── Join ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        plugin.getMatchManager().handlePlayerJoin(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                if (!plugin.getMatchManager().isInMatch(player)) {
                    plugin.getLobbyItemManager().giveItems(player);
                    plugin.getMatchManager().updatePlayerTabPrefix(player);
                }
            }
        }, 5L);
    }

    // ── Lobby item: hold effects ───────────────────────────────────────────────

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (plugin.getMatchManager().isInMatch(player)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() { plugin.getLobbyItemManager().showHeldEffects(player); }
        }, 1L);
    }

    // ── Lobby item: click action ──────────────────────────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMatchManager().isInMatch(player)) return;

        LobbyItem item = plugin.getLobbyItemManager().getHeld(player);
        if (item == null || item.getAction() == LobbyItem.ClickAction.NONE) return;

        boolean isLeft  = event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                       || event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
        boolean isRight = event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                       || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

        boolean triggered = false;
        switch (item.getAction()) {
            case LEFT:  triggered = isLeft;  break;
            case RIGHT: triggered = isRight; break;
            case ANY:   triggered = isLeft || isRight; break;
            default: break;
        }

        if (triggered && !item.getCommand().isEmpty()) {
            event.setCancelled(true);
            player.performCommand(item.getCommand());
        }
    }

    // ── Lobby item: prevent inventory move ────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (plugin.getMatchManager().isInMatch(player)) return;
        if (event.getCurrentItem() == null) return;
        LobbyItem li = plugin.getLobbyItemManager()
                .getBySlotAndMaterial(event.getSlot(), event.getCurrentItem().getType());
        if (li != null) event.setCancelled(true);
    }

    // ── Countdown: freeze horizontal movement ─────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatch(player);
        if (match == null) return;

        if (match.getState() == Match.MatchState.COUNTDOWN) {
            // Allow Y (jumping) but block X/Z movement
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                Location to = event.getFrom().clone();
                to.setY(event.getTo().getY());
                to.setYaw(event.getTo().getYaw());
                to.setPitch(event.getTo().getPitch());
                event.setTo(to);
            }
            return;
        }

        if (match.getState() != Match.MatchState.RUNNING) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        // Void → fake death
        if (player.getLocation().getY() <= match.getArena().getVoidLevel()) {
            final Player fp = player;
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() { plugin.getMatchManager().handleFakeDeath(fp, null); }
            }, 1L);
        }
    }

    // ── Damage: fake-death when health would reach 0 ──────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Match match = plugin.getMatchManager().getMatch(victim);
        if (match == null) return;

        // Block during countdown
        if (match.getState() == Match.MatchState.COUNTDOWN) { event.setCancelled(true); return; }

        // ── Spawn protection: only blocks incoming damage to protected player ──
        // If the VICTIM has protection AND the attacker is a player (not self-damage):
        if (match.hasSpawnProtection(victim.getUniqueId())
                && event.getDamager() instanceof Player) {
            // Protection blocks the hit — but if they fight back it goes away
            event.setCancelled(true);
            return;
        }

        if (!(event.getDamager() instanceof Player)) return;
        Player killer = (Player) event.getDamager();

        // If attacker has spawn protection, cancel it (they chose to fight)
        if (match.hasSpawnProtection(killer.getUniqueId())) {
            match.removeSpawnProtection(killer.getUniqueId());
        }

        // Would this kill the victim?
        double remaining = victim.getHealth() - event.getFinalDamage();
        if (remaining <= 0) {
            event.setCancelled(true);
            final Player fv = victim, fk = killer;
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() { plugin.getMatchManager().handleFakeDeath(fv, fk); }
            }, 1L);
        }
    }

    // ── Fall damage + void ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Global fall damage toggle
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !plugin.isFallDamageEnabled()) {
            event.setCancelled(true);
            return;
        }

        // Suppress void damage (we handle it in onPlayerMove)
        Match match = plugin.getMatchManager().getMatch(player);
        if (match != null && event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
        }
    }

    // ── Block Break ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatch(player);
        if (match == null) return;

        if (match.getState() == Match.MatchState.COUNTDOWN) { event.setCancelled(true); return; }

        Block block  = event.getBlock();
        Location loc = block.getLocation();
        Arena arena  = match.getArena();

        // Bed break
        if (block.getType() == Material.BED_BLOCK || block.getType() == Material.BED) {
            GameTeam bedOwner = null;
            if (arena.isRedBed(loc))        bedOwner = GameTeam.RED;
            else if (arena.isBlueBed(loc))  bedOwner = GameTeam.BLUE;

            if (bedOwner != null) {
                GameTeam bt = match.getTeam(player);
                if (bt == bedOwner) { event.setCancelled(true); player.sendMessage(ChatColor.RED + "You cannot break your own bed!"); return; }
                if (!match.isBedAlive(bedOwner)) { event.setCancelled(true); return; }
                event.setExpToDrop(0);
                plugin.getMatchManager().handleBedBreak(player, bedOwner, match);
                return;
            }
        }

        // Player-placed: allow, no drop
        if (arena.isInsideBounds(loc) && match.isPlayerPlaced(loc)) {
            match.untrackPlacedBlock(loc);
            event.setExpToDrop(0);
            event.setCancelled(true);
            block.setType(Material.AIR);
            return;
        }

        // Whitelisted: allow with drop
        if (arena.isInsideBoundsXZ(loc) && arena.isWhitelisted(loc)) {
            event.setExpToDrop(0);
            return;
        }

        event.setCancelled(true);
    }

    // ── Block Place ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatch(player);
        if (match == null) return;

        if (match.getState() == Match.MatchState.COUNTDOWN) { event.setCancelled(true); return; }

        Location loc = event.getBlock().getLocation();
        Arena arena  = match.getArena();

        if (!arena.isInsideBoundsXZ(loc)) { event.setCancelled(true); return; }
        if (loc.getBlockY() >= arena.getHeightLimit()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Cannot place above Y=" + arena.getHeightLimit() + "!");
            return;
        }
        match.trackPlacedBlock(loc);
    }

    // ── Item drop ─────────────────────────────────────────────────────────────

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMatchManager().isInMatch(player)) { event.setCancelled(true); return; }
        LobbyItem li = plugin.getLobbyItemManager().getHeld(player);
        if (li != null && !li.isDroppable()) event.setCancelled(true);
    }

    // ── Quit ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Match spectated = plugin.getMatchManager().getSpectatedMatch(player);
        if (spectated != null) spectated.removeSpectator(player.getUniqueId());

        Match match = plugin.getMatchManager().getMatch(player);
        if (match == null) return;

        Player opponent = plugin.getMatchManager().getOpponent(match, player);
        if (opponent != null) {
            GameTeam ot = match.getTeam(opponent);
            GameTeam lt = match.getTeam(player);
            String wn = "" + (ot != null ? ot.getChatColor() : "") + opponent.getName();
            String ln = "" + (lt != null ? lt.getChatColor() : "") + player.getName();
            plugin.getMatchManager().broadcastToMatch(match,
                    plugin.getMsgManager().get("disconnect-win",
                            ir.practice.pvp.utils.MessageManager.of("winner", wn, "loser", ln)));
        }
        plugin.getMatchManager().endMatch(match, opponent, false);
    }
}
