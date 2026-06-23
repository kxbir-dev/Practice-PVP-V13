package ir.practice.pvp;

import ir.practice.pvp.commands.*;
import ir.practice.pvp.listeners.GameListener;
import ir.practice.pvp.managers.*;
import ir.practice.pvp.utils.LuckPrefixHook;
import ir.practice.pvp.utils.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PracticePvP extends JavaPlugin {

    private ArenaManager     arenaManager;
    private MatchManager     matchManager;
    private QueueManager     queueManager;
    private LobbyItemManager lobbyItemManager;
    private MessageManager   msgManager;

    private boolean fallDamageEnabled = false; // off by default

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();

        msgManager       = new MessageManager(this);
        arenaManager     = new ArenaManager(this);
        matchManager     = new MatchManager(this);
        queueManager     = new QueueManager(this);
        lobbyItemManager = new LobbyItemManager(this);

        LuckPrefixHook.init();

        // Load fall damage setting from config
        fallDamageEnabled = getConfig().getBoolean("fall-damage", false);

        PrcCommand prcCmd = new PrcCommand(this);
        getCommand("prc").setExecutor(prcCmd);
        getCommand("prc").setTabCompleter(prcCmd);

        BfCommand bfCmd = new BfCommand(this);
        getCommand("bf").setExecutor(bfCmd);
        getCommand("bf").setTabCompleter(bfCmd);

        SpectateCommand specCmd = new SpectateCommand(this);
        getCommand("spectate").setExecutor(specCmd);
        getCommand("spectate").setTabCompleter(specCmd);

        FallDamageCommand fdCmd = new FallDamageCommand(this);
        getCommand("falldamage").setExecutor(fdCmd);
        getCommand("falldamage").setTabCompleter(fdCmd);

        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getLogger().info("PracticePvP v2.4 enabled!");
    }

    @Override
    public void onDisable() {
        // Save fall damage setting
        getConfig().set("fall-damage", fallDamageEnabled);
        saveConfig();
        getLogger().info("PracticePvP disabled.");
    }

    public ArenaManager     getArenaManager()     { return arenaManager; }
    public MatchManager     getMatchManager()     { return matchManager; }
    public QueueManager     getQueueManager()     { return queueManager; }
    public LobbyItemManager getLobbyItemManager() { return lobbyItemManager; }
    public MessageManager   getMsgManager()       { return msgManager; }

    public boolean isFallDamageEnabled() { return fallDamageEnabled; }
    public void setFallDamageEnabled(boolean v) {
        fallDamageEnabled = v;
        getConfig().set("fall-damage", v);
        saveConfig();
    }
}
