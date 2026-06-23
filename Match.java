package ir.practice.pvp.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class Match {

    public enum MatchState { WAITING, COUNTDOWN, RUNNING, ENDED }

    private final Arena arena;
    private final Map<UUID, GameTeam> playerTeams = new LinkedHashMap<UUID, GameTeam>();
    private boolean redBedAlive  = true;
    private boolean blueBedAlive = true;
    private MatchState state     = MatchState.WAITING;
    private int timeLeft;

    private final Set<String>         playerPlacedBlocks = new HashSet<String>();
    private final List<BlockSnapshot> arenaSnapshot      = new ArrayList<BlockSnapshot>();
    private final Set<UUID>           spawnProtected     = new HashSet<UUID>();
    private final Set<UUID>           spectators         = new HashSet<UUID>();

    public Match(Arena arena, int gameDuration) {
        this.arena    = arena;
        this.timeLeft = gameDuration;
    }

    public Arena getArena() { return arena; }

    public void addPlayer(Player player, GameTeam team) { playerTeams.put(player.getUniqueId(), team); }
    public GameTeam getTeam(Player player) { return playerTeams.get(player.getUniqueId()); }
    public Map<UUID, GameTeam> getPlayerTeams() { return playerTeams; }

    public boolean isRedBedAlive()  { return redBedAlive; }
    public boolean isBlueBedAlive() { return blueBedAlive; }
    public void setRedBedAlive(boolean v)  { redBedAlive  = v; }
    public void setBlueBedAlive(boolean v) { blueBedAlive = v; }
    public boolean isBedAlive(GameTeam t)  { return t == GameTeam.RED ? redBedAlive : blueBedAlive; }
    public void destroyBed(GameTeam t)     { if (t == GameTeam.RED) redBedAlive = false; else blueBedAlive = false; }

    public MatchState getState()       { return state; }
    public void setState(MatchState s) { state = s; }
    public int  getTimeLeft()          { return timeLeft; }
    public void setTimeLeft(int v)     { timeLeft = v; }
    public void tickTime()             { if (timeLeft > 0) timeLeft--; }

    public void trackPlacedBlock(Location loc)   { playerPlacedBlocks.add(key(loc)); }
    public void untrackPlacedBlock(Location loc) { playerPlacedBlocks.remove(key(loc)); }
    public boolean isPlayerPlaced(Location loc)  { return playerPlacedBlocks.contains(key(loc)); }

    public List<BlockSnapshot> getArenaSnapshot() { return arenaSnapshot; }
    public void addSnapshot(BlockSnapshot bs)     { arenaSnapshot.add(bs); }

    public void addSpawnProtection(UUID u)    { spawnProtected.add(u); }
    public void removeSpawnProtection(UUID u) { spawnProtected.remove(u); }
    public boolean hasSpawnProtection(UUID u) { return spawnProtected.contains(u); }

    public Set<UUID> getSpectators()       { return spectators; }
    public void addSpectator(UUID u)       { spectators.add(u); }
    public void removeSpectator(UUID u)    { spectators.remove(u); }
    public boolean isSpectator(UUID u)     { return spectators.contains(u); }

    private String key(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
