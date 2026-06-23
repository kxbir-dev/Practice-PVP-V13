package ir.practice.pvp.managers;

import ir.practice.pvp.PracticePvP;
import ir.practice.pvp.models.Arena;
import ir.practice.pvp.models.BlockSnapshot;
import ir.practice.pvp.models.GameTeam;
import ir.practice.pvp.models.Match;
import ir.practice.pvp.utils.LuckPrefixHook;
import ir.practice.pvp.utils.MessageManager;
import ir.practice.pvp.utils.TitleUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

public class MatchManager {

    private final PracticePvP plugin;
    private final Map<UUID, Match>      playerMatches  = new HashMap<UUID, Match>();
    private final Map<UUID, Integer>    respawnTasks   = new HashMap<UUID, Integer>();
    private final Map<String, Match>    arenaMatches   = new HashMap<String, Match>();
    private final Map<UUID, Match>      spectatorMap   = new HashMap<UUID, Match>();

    // ── Per-player persistent scoreboards ────────────────────────────────────
    // We create ONE scoreboard per player and reuse it — no re-registration
    private final Map<UUID, Scoreboard> playerBoards   = new HashMap<UUID, Scoreboard>();

    public MatchManager(PracticePvP plugin) { this.plugin = plugin; }

    public Match getMatch(Player p)           { return playerMatches.get(p.getUniqueId()); }
    public boolean isInMatch(Player p)        { return playerMatches.containsKey(p.getUniqueId()); }
    public Match getMatchByArena(String name) { return arenaMatches.get(name.toLowerCase()); }
    public Match getSpectatedMatch(Player p)  { return spectatorMap.get(p.getUniqueId()); }

    // ── Start ─────────────────────────────────────────────────────────────────

    public void startMatch(final Arena arena, final Player p1, final Player p2) {
        int dur = plugin.getConfig().getInt("default-game-time", 1200);
        final Match match = new Match(arena, dur);
        match.addPlayer(p1, GameTeam.RED);
        match.addPlayer(p2, GameTeam.BLUE);
        match.setState(Match.MatchState.COUNTDOWN);

        playerMatches.put(p1.getUniqueId(), match);
        playerMatches.put(p2.getUniqueId(), match);
        arenaMatches.put(arena.getName().toLowerCase(), match);

        takeSnapshot(match, arena);
        teleportToSpawn(p1, GameTeam.RED,  arena);
        teleportToSpawn(p2, GameTeam.BLUE, arena);
        giveKit(p1, GameTeam.RED);
        giveKit(p2, GameTeam.BLUE);

        // Create persistent scoreboards ONCE per player
        initScoreboard(p1, match);
        initScoreboard(p2, match);

        startCountdown(match, p1, p2);
    }

    private void teleportToSpawn(Player p, GameTeam team, Arena arena) {
        Location s = (team == GameTeam.RED) ? arena.getRedSpawn() : arena.getBlueSpawn();
        if (s != null) p.teleport(s);
    }

    private void giveKit(Player p, GameTeam team) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setHealth(20); p.setFoodLevel(20);
        p.setGameMode(GameMode.SURVIVAL);
        p.setLevel(0); p.setExp(0);
        for (ItemStack item : team.getKit()) p.getInventory().addItem(item);
        ItemStack[] armor = team.getArmorKit();
        p.getInventory().setHelmet(armor[0]);
        p.getInventory().setChestplate(armor[1]);
        p.getInventory().setLeggings(armor[2]);
        p.getInventory().setBoots(armor[3]);
        p.sendMessage(plugin.getMsgManager().get("match-start-team",
                MessageManager.of("team", "" + team.getChatColor() + team.getDisplayName())));
    }

    // ── Scoreboard: create once, update in place ──────────────────────────────

    /**
     * Creates ONE scoreboard per player with a fixed objective named "pp".
     * Subsequent calls to updateScoreboard() only change score values — never
     * re-registers objectives — which is what causes "Restart the Server" spam.
     */
    private void initScoreboard(Player p, Match match) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        // Register objective ONCE
        Objective obj = board.registerNewObjective("pp", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(plugin.getMsgManager().get("scoreboard-title"));

        // Apply LuckPrefix tab prefix to this board — does NOT touch display name
        applyLuckPrefixTeam(board, p);

        playerBoards.put(p.getUniqueId(), board);
        p.setScoreboard(board);

        // Fill initial scores
        refreshScoreboardLines(p, board, match);
    }

    public void updateScoreboard(Match match) {
        for (UUID uuid : match.getPlayerTeams().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;

            Scoreboard board = playerBoards.get(uuid);
            if (board == null) {
                // Safety: init if missing
                initScoreboard(p, match);
                board = playerBoards.get(uuid);
            }
            refreshScoreboardLines(p, board, match);
        }
    }

    /**
     * Updates score values in-place. No new objectives or scoreboards created.
     * We wipe all existing scores and re-set them — this is safe and does NOT
     * produce "Restart the Server".
     */
    private void refreshScoreboardLines(Player p, Scoreboard board, Match match) {
        Objective obj = board.getObjective("pp");
        if (obj == null) return;

        // Update title (safe to call every tick)
        obj.setDisplayName(plugin.getMsgManager().get("scoreboard-title"));

        String serverIp = plugin.getConfig().getString("server-ip", "play.yourserver.net");
        MessageManager m = plugin.getMsgManager();
        String alive = m.get("scoreboard-bed-alive");
        String dead  = m.get("scoreboard-bed-dead");

        // Build line strings
        String redBedLine  = m.get("scoreboard-red-bed",  MessageManager.of("status", match.isRedBedAlive()  ? alive : dead));
        String blueBedLine = m.get("scoreboard-blue-bed", MessageManager.of("status", match.isBlueBedAlive() ? alive : dead));
        String timeLine    = m.get("scoreboard-time", MessageManager.of("time", formatTime(match.getTimeLeft())));
        String ipLine      = ChatColor.AQUA + serverIp;
        String sep         = ChatColor.WHITE + "-------------";

        // Reset all entries then re-set (avoids duplicate-line issues)
        for (String entry : new ArrayList<String>(board.getEntries())) {
            board.resetScores(entry);
        }

        int line = 10;
        setScore(obj, sep,         line--);
        setScore(obj, redBedLine,  line--);
        setScore(obj, blueBedLine, line--);
        setScore(obj, sep + " ",   line--); // trailing space makes it unique
        setScore(obj, timeLine,    line--);
        setScore(obj, sep + "  ",  line--);
        setScore(obj, ipLine,      line--);
    }

    private void setScore(Objective obj, String entry, int score) {
        obj.getScore(entry).setScore(score);
    }

    private String formatTime(int s) { return String.format("%02d:%02d", s / 60, s % 60); }

    // ── LuckPrefix tab: apply once per board ──────────────────────────────────

    /**
     * Adds the player's LuckPrefix prefix to the scoreboard team.
     * This only affects the TAB list prefix — it does NOT change the
     * player's display name or chat name at all.
     * Called once when the board is created, and again after match ends.
     */
    private void applyLuckPrefixTeam(Scoreboard board, Player player) {
        String prefix = LuckPrefixHook.getTabPrefix(player);
        if (prefix.isEmpty()) return;

        // Use player-specific team name to avoid conflicts between players
        String teamName = "lp" + player.getName().substring(0, Math.min(8, player.getName().length()));
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        team.setPrefix(prefix);
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }

    /** Called when a player joins/leaves — ensure their tab prefix is applied to current board */
    public void updatePlayerTabPrefix(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) return;
        applyLuckPrefixTeam(board, player);
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void startCountdown(final Match match, final Player p1, final Player p2) {
        final int[] count = {5};
        new BukkitRunnable() {
            @Override public void run() {
                if (match.getState() == Match.MatchState.ENDED) { cancel(); return; }
                if (count[0] > 0) {
                    String cs = String.valueOf(count[0]);
                    for (Player p : new Player[]{p1, p2}) {
                        p.sendMessage(plugin.getMsgManager().get("countdown-chat", MessageManager.of("seconds", cs)));
                        TitleUtil.sendTitle(p,
                                plugin.getMsgManager().get("countdown-title", MessageManager.of("seconds", cs)),
                                plugin.getMsgManager().get("countdown-subtitle"),
                                0, 25, 5);
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    }
                    count[0]--;
                } else {
                    match.setState(Match.MatchState.RUNNING);
                    for (Player p : new Player[]{p1, p2}) {
                        p.sendMessage(plugin.getMsgManager().get("countdown-go"));
                        TitleUtil.sendTitle(p, plugin.getMsgManager().get("countdown-go-title"), "", 0, 30, 10);
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1f, 2f);
                    }
                    startGameTimer(match);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ── Snapshot / Restore ────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void takeSnapshot(Match match, Arena arena) {
        if (arena.getCorner1() == null || arena.getCorner2() == null) return;
        World w = arena.getCorner1().getWorld();
        int minX = Math.min(arena.getCorner1().getBlockX(), arena.getCorner2().getBlockX());
        int maxX = Math.max(arena.getCorner1().getBlockX(), arena.getCorner2().getBlockX());
        int minY = Math.min(arena.getCorner1().getBlockY(), arena.getCorner2().getBlockY());
        int maxY = Math.max(arena.getCorner1().getBlockY(), arena.getCorner2().getBlockY());
        int minZ = Math.min(arena.getCorner1().getBlockZ(), arena.getCorner2().getBlockZ());
        int maxZ = Math.max(arena.getCorner1().getBlockZ(), arena.getCorner2().getBlockZ());
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    match.addSnapshot(new BlockSnapshot(w.getBlockAt(x, y, z)));
    }

    private void restoreArena(Match match) {
        List<BlockSnapshot> snap = new ArrayList<BlockSnapshot>(match.getArenaSnapshot());
        // Pass 1: AIR
        for (BlockSnapshot bs : snap) if (bs.getMaterial() == Material.AIR) bs.restore();
        // Pass 2: non-bed blocks
        for (BlockSnapshot bs : snap)
            if (bs.getMaterial() != Material.AIR && bs.getMaterial() != Material.BED_BLOCK) bs.restore();
        // Pass 3: bed foot then head
        List<BlockSnapshot> feet = new ArrayList<BlockSnapshot>(), heads = new ArrayList<BlockSnapshot>();
        for (BlockSnapshot bs : snap) {
            if (bs.getMaterial() != Material.BED_BLOCK) continue;
            if (bs.getData() < 8) feet.add(bs); else heads.add(bs);
        }
        for (BlockSnapshot bs : feet)  bs.restore();
        for (BlockSnapshot bs : heads) bs.restore();
    }

    // ── Fake-death ────────────────────────────────────────────────────────────

    public void handleFakeDeath(Player player, Player killer) {
        Match match = getMatch(player);
        if (match == null || match.getState() != Match.MatchState.RUNNING) return;
        GameTeam team = match.getTeam(player);
        if (team == null) return;

        player.setHealth(20);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setGameMode(GameMode.SPECTATOR);

        String vName = "" + team.getChatColor() + player.getName();
        Player opponent = getOpponent(match, player);

        if (killer != null && !killer.equals(player)) {
            GameTeam kt  = match.getTeam(killer);
            String kName = "" + (kt != null ? kt.getChatColor() : "") + killer.getName();
            String msg   = plugin.getMsgManager().get("kill-message", MessageManager.of("killer", kName, "victim", vName));
            killer.sendMessage(msg);
            killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);
            player.sendMessage(msg);
            if (opponent != null && !opponent.equals(killer)) {
                opponent.sendMessage(msg);
                opponent.playSound(opponent.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);
            }
        } else {
            String msg = "" + team.getChatColor() + player.getName() + ChatColor.GRAY + " fell into the void.";
            broadcastToMatch(match, msg);
            if (opponent != null) opponent.playSound(opponent.getLocation(), Sound.ORB_PICKUP, 1f, 1.5f);
        }

        if (!match.isBedAlive(team)) { endMatch(match, opponent, false); return; }
        startRespawnCountdown(player, match, match.getArena().getRespawnTime());
    }

    // ── Respawn countdown ─────────────────────────────────────────────────────

    private void startRespawnCountdown(final Player player, final Match match, final int seconds) {
        if (respawnTasks.containsKey(player.getUniqueId()))
            Bukkit.getScheduler().cancelTask(respawnTasks.get(player.getUniqueId()));

        final int[] rem = {seconds};
        int taskId = new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline() || match.getState() != Match.MatchState.RUNNING) { cancel(); return; }
                if (rem[0] <= 0) { doRespawn(player, match); cancel(); return; }
                player.sendMessage(plugin.getMsgManager().get("respawn-countdown", MessageManager.of("seconds", String.valueOf(rem[0]))));
                player.setLevel(rem[0]);
                rem[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        respawnTasks.put(player.getUniqueId(), taskId);
    }

    private void doRespawn(Player player, Match match) {
        respawnTasks.remove(player.getUniqueId());
        GameTeam team = match.getTeam(player);
        if (team == null) return;

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20); player.setFoodLevel(20);
        giveKit(player, team);

        Location spawn = (team == GameTeam.RED) ? match.getArena().getRedSpawn() : match.getArena().getBlueSpawn();
        if (spawn != null) player.teleport(spawn);
        player.setLevel(0);
        player.sendMessage(plugin.getMsgManager().get("respawn-spawned"));

        Player opponent = getOpponent(match, player);
        if (opponent != null)
            opponent.sendMessage(plugin.getMsgManager().get("respawn-opponent",
                    MessageManager.of("team", "" + team.getChatColor(), "player", player.getName())));

        applySpawnProtection(player, match, plugin.getMsgManager().getInt("spawn-protection-seconds", 3));
        updateScoreboard(match);
    }

    private void applySpawnProtection(final Player player, final Match match, int secs) {
        match.addSpawnProtection(player.getUniqueId());
        new BukkitRunnable() {
            @Override public void run() { match.removeSpawnProtection(player.getUniqueId()); }
        }.runTaskLater(plugin, secs * 20L);
    }

    // ── Bed break ─────────────────────────────────────────────────────────────

    public void handleBedBreak(Player breaker, GameTeam bedTeam, Match match) {
        match.destroyBed(bedTeam);
        GameTeam bt  = match.getTeam(breaker);
        String bName = "" + (bt != null ? bt.getChatColor() : "") + breaker.getName();
        String msg   = plugin.getMsgManager().get("bed-break",
                MessageManager.of("breaker", bName, "team", "" + bedTeam.getChatColor() + bedTeam.getDisplayName()));
        broadcastToMatch(match, msg);
        for (UUID uuid : match.getPlayerTeams().keySet()) {
            Player p = Bukkit.getPlayer(uuid); if (p != null) p.playSound(p.getLocation(), Sound.WITHER_DEATH, 1f, 1f);
        }
        for (Map.Entry<UUID, GameTeam> e : match.getPlayerTeams().entrySet()) {
            if (e.getValue() == bedTeam) {
                Player victim = Bukkit.getPlayer(e.getKey());
                if (victim != null) TitleUtil.sendTitle(victim,
                        plugin.getMsgManager().get("bed-break-title"),
                        plugin.getMsgManager().get("bed-break-subtitle"), 10, 60, 10);
            }
        }
        updateScoreboard(match);
    }

    // ── End match ─────────────────────────────────────────────────────────────

    public void endMatch(final Match match, Player winner, boolean admin) {
        if (match.getState() == Match.MatchState.ENDED) return;
        match.setState(Match.MatchState.ENDED);

        for (UUID uuid : match.getPlayerTeams().keySet()) {
            if (respawnTasks.containsKey(uuid)) { Bukkit.getScheduler().cancelTask(respawnTasks.get(uuid)); respawnTasks.remove(uuid); }
        }

        for (Map.Entry<UUID, GameTeam> e : match.getPlayerTeams().entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey()); if (p == null || !p.isOnline()) continue;
            boolean won = (winner != null && p.getUniqueId().equals(winner.getUniqueId()));
            if (admin) {
                p.sendMessage(plugin.getMsgManager().get("match-stopped"));
            } else if (winner == null) {
                p.sendMessage(plugin.getMsgManager().get("match-draw"));
                TitleUtil.sendTitle(p, plugin.getMsgManager().get("match-draw"), "", 10, 60, 10);
            } else if (won) {
                p.sendMessage(plugin.getMsgManager().get("match-winner", MessageManager.of("winner", winner.getName())));
                TitleUtil.sendTitle(p, plugin.getMsgManager().get("victory-title"), plugin.getMsgManager().get("victory-subtitle"), 10, 80, 10);
            } else {
                p.sendMessage(plugin.getMsgManager().get("match-winner", MessageManager.of("winner", winner.getName())));
                TitleUtil.sendTitle(p, plugin.getMsgManager().get("lose-title"), plugin.getMsgManager().get("lose-subtitle"), 10, 80, 10);
            }
        }

        arenaMatches.remove(match.getArena().getName().toLowerCase());

        new BukkitRunnable() {
            @Override public void run() {
                restoreArena(match);
                new BukkitRunnable() {
                    @Override public void run() {
                        for (UUID uuid : match.getPlayerTeams().keySet()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null && p.isOnline()) sendToLobby(p);
                            playerMatches.remove(uuid);
                            playerBoards.remove(uuid); // clean up board reference
                        }
                        for (UUID su : new ArrayList<UUID>(match.getSpectators())) {
                            Player sp = Bukkit.getPlayer(su); if (sp != null) removeSpectator(sp);
                        }
                    }
                }.runTaskLater(plugin, 20L);
            }
        }.runTaskLater(plugin, 80L);
    }

    public void sendToLobby(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(20); player.setFoodLevel(20);
        player.setLevel(0); player.setExp(0);
        player.setGameMode(GameMode.SURVIVAL);

        String lobbyWorld = plugin.getConfig().getString("lobby-world", "world");
        World world = Bukkit.getWorld(lobbyWorld);
        if (world != null) {
            double x  = plugin.getConfig().getDouble("lobby-spawn.x", 0.5);
            double y  = plugin.getConfig().getDouble("lobby-spawn.y", 64);
            double z  = plugin.getConfig().getDouble("lobby-spawn.z", 0.5);
            float yaw = (float) plugin.getConfig().getDouble("lobby-spawn.yaw", 0);
            float pit = (float) plugin.getConfig().getDouble("lobby-spawn.pitch", 0);
            player.teleport(new Location(world, x, y, z, yaw, pit));
        }

        // Give lobby its own fresh scoreboard with LuckPrefix applied
        // This preserves the player's prefix in lobby tab
        Scoreboard lobbyBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        applyLuckPrefixTeam(lobbyBoard, player);
        player.setScoreboard(lobbyBoard);

        player.sendMessage(plugin.getMsgManager().get("match-lobby"));
        plugin.getLobbyItemManager().giveItems(player);
    }

    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        playerBoards.remove(uuid);
        if (playerMatches.containsKey(uuid)) {
            playerMatches.remove(uuid);
            final Player p = player;
            new BukkitRunnable() { @Override public void run() { sendToLobby(p); } }.runTaskLater(plugin, 5L);
        }
        if (spectatorMap.containsKey(uuid)) { Match m = spectatorMap.remove(uuid); if (m != null) m.removeSpectator(uuid); }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private void startGameTimer(final Match match) {
        new BukkitRunnable() {
            @Override public void run() {
                if (match.getState() == Match.MatchState.ENDED) { cancel(); return; }
                match.tickTime();
                updateScoreboard(match);
                if (match.getTimeLeft() <= 0) {
                    match.setRedBedAlive(false); match.setBlueBedAlive(false);
                    broadcastToMatch(match, plugin.getMsgManager().get("match-draw"));
                    endMatch(match, null, false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ── Spectate ──────────────────────────────────────────────────────────────

    public boolean addSpectator(Player sp, Player target) {
        Match match = getMatch(target);
        if (match == null) { sp.sendMessage(ChatColor.RED + target.getName() + " is not in a match."); return false; }
        if (isInMatch(sp)) { sp.sendMessage(ChatColor.RED + "You cannot spectate while in a match."); return false; }
        removeSpectator(sp);
        match.addSpectator(sp.getUniqueId());
        spectatorMap.put(sp.getUniqueId(), match);
        sp.setGameMode(GameMode.SPECTATOR);
        Location loc = match.getArena().getRedSpawn();
        if (loc != null) sp.teleport(loc);
        sp.sendMessage(ChatColor.GREEN + "Now spectating " + target.getName() + ".");
        return true;
    }

    public void removeSpectator(Player sp) {
        Match match = spectatorMap.remove(sp.getUniqueId());
        if (match != null) match.removeSpectator(sp.getUniqueId());
        sendToLobby(sp);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    public void broadcastToMatch(Match match, String message) {
        for (UUID uuid : match.getPlayerTeams().keySet()) {
            Player p = Bukkit.getPlayer(uuid); if (p != null && p.isOnline()) p.sendMessage(message);
        }
    }

    public Player getOpponent(Match match, Player player) {
        for (UUID uuid : match.getPlayerTeams().keySet())
            if (!uuid.equals(player.getUniqueId())) return Bukkit.getPlayer(uuid);
        return null;
    }
}
