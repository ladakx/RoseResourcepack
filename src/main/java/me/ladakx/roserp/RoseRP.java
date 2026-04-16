package me.ladakx.roserp;

import me.ladakx.roserp.api.HostService;
import me.ladakx.roserp.bstats.Metrics;
import me.ladakx.roserp.commands.SubCommandManager;
import me.ladakx.roserp.commands.TabCommandManager;
import me.ladakx.roserp.file.MessagesFile;
import me.ladakx.roserp.file.config.MessagesCFG;
import me.ladakx.roserp.file.config.PluginCFG;
import me.ladakx.roserp.host.Hosting;
import me.ladakx.roserp.listeners.RPEventListener;
import me.ladakx.roserp.pack.Applier;
import me.ladakx.roserp.packer.Packer;
import me.ladakx.roserp.pack.Pack;
import me.ladakx.roserp.util.MinecraftVersions;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class RoseRP extends JavaPlugin {

    private static final Logger logger = Logger.getLogger("Minecraft");

    public static final boolean above1_20_3 = MinecraftVersions.isCoreVersionAboveOrEqual("1.20.3");
    public static final boolean above1_20_4 = MinecraftVersions.isCoreVersionAboveOrEqual("1.20.4");
    public static final boolean above1_18_1 = MinecraftVersions.isCoreVersionAboveOrEqual("1.18.1");

    private static RoseRP instance;
    private static HostService host;

    public final ConcurrentHashMap<String, Pack> packs = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, CopyOnWriteArrayList<Pack>> players = new ConcurrentHashMap<>();

    private BukkitAudiences adventure;
    private PluginCFG pluginConfig;
    private MessagesFile messages;
    private ExecutorService packerExecutor;

    @Override
    public void onEnable() {
        instance = this;
        RoseRPLogger.init(this);
        adventure = BukkitAudiences.create(this);

        Metrics metrics = new Metrics(this, 23796);
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));

        loadMessagesFiles();
        saveDefaultConfig();
        syncConfigDefaults();
        if (getConfig().getBoolean("loadDefaultFiles", false)) {
            loadDefaultFiles();
        }

        pluginConfig = new PluginCFG();
        messages = new MessagesFile();
        MessagesCFG.refreshAll();

        PluginCommand cmd = getCommand("roserp");
        if (cmd == null) {
            RoseRPLogger.error("Command /roserp is not registered in plugin.yml");
            return;
        }
        cmd.setExecutor(new SubCommandManager());
        cmd.setTabCompleter(new TabCommandManager());

        Bukkit.getPluginManager().registerEvents(new RPEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new UpdateChecker(), this);

        packerExecutor = Executors.newFixedThreadPool(
                Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 2)),
                r -> {
                    Thread t = new Thread(r, "roserp-packer");
                    t.setDaemon(true);
                    return t;
                });

        loadResourcepacks();

        host = new Hosting(this, Runtime.getRuntime().availableProcessors());
        try {
            host.start();
        } catch (Exception e) {
            RoseRPLogger.error("Failed to start resource pack host", e);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            HostService h = RoseRP.getHosting();
            if (!(h instanceof Hosting hosting)) return;

            if (!hosting.isReallyAlive()) {
                RoseRPLogger.error("Host watchdog detected dead host. Restarting...");
                try {
                    hosting.stop();
                    hosting.start();
                } catch (Exception e) {
                    RoseRPLogger.error("Failed to restart host from watchdog", e);
                }
            }
        }, 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        if (host != null) {
            host.stop();
            host = null;
        }

        if (adventure != null) {
            adventure.close();
            adventure = null;
        }

        if (packerExecutor != null) {
            packerExecutor.shutdownNow();
            packerExecutor = null;
        }
    }

    private void loadMessagesFiles() {
        for (String lang : Set.of("en", "ru")) {
            File f = new File(getDataFolder(), "lang/messages_" + lang + ".yml");
            if (!f.exists()) {
                saveResource("lang/messages_" + lang + ".yml", false);
            }
        }
    }

    private void loadDefaultFiles() {
        saveFileWithoutWarn("resourcepacks/low_quality/pack.mcmeta");
        saveFileWithoutWarn("resourcepacks/main/pack.mcmeta");
        getConfig().set("loadDefaultFiles", false);
        saveConfig();
        reloadConfig();
        syncConfigDefaults();
    }

    private void saveFileWithoutWarn(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    public void loadResourcepacks() {
        FileConfiguration cfg = getConfig();
        packs.clear();

        if (cfg.contains("packs")) {
            cfg.getConfigurationSection("packs").getKeys(false).forEach(name -> {
                try {
                    Pack pack = new Pack(name, cfg);
                    packs.put(pack.getName(), pack);
                } catch (Exception e) {
                    RoseRPLogger.error("Failed to load pack config: " + name, e);
                }
            });
        }

        for (Pack pack : packs.values()) {
            ExecutorService executor = packerExecutor;
            if (executor == null || executor.isShutdown()) {
                RoseRPLogger.warn("Packer executor is not available, skipping pack build for " + pack.getName());
                continue;
            }

            executor.submit(() -> {
                try {
                    Packer.packFiles(pack);
                    MessagesCFG.RP_SUCCESSFULLY_PACKED.sendMessage(
                            Bukkit.getConsoleSender(), "{pack}", pack.getName());
                } catch (Exception ex) {
                    RoseRPLogger.error("Failed to pack resourcepack " + pack.getName(), ex);
                }
            });
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        syncConfigDefaults();
        messages.reload();

        pluginConfig = new PluginCFG();
        MessagesCFG.refreshAll();

        if (host != null) {
            try {
                host.stop();
            } catch (Exception e) {
                RoseRPLogger.error("Failed to stop resource pack host during reload", e);
            }
        }

        players.forEach((uuid, list) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (above1_20_4) p.removeResourcePacks();
                else p.setResourcePack("", new byte[0], "", false);
            }
        });

        loadResourcepacks();

        players.forEach((uuid, list) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) return;
            for (Pack pack : list) {
                Applier.apply(p, pack);
            }
        });

        host = new Hosting(this, Runtime.getRuntime().availableProcessors());
        try {
            host.start();
        } catch (Exception e) {
            RoseRPLogger.error("Failed to restart resource pack host after reload", e);
        }
    }

    private void syncConfigDefaults() {
        FileConfiguration config = getConfig();
        boolean changed = false;

        changed |= applyDefault(config, "host.readTimeoutMs", 15000);
        changed |= applyDefault(config, "host.maxRequestLineLength", 2048);
        changed |= applyDefault(config, "host.maxActiveConnections", 128);
        changed |= applyDefault(config, "host.logNotFoundRequests", false);
        changed |= applyDefault(config, "host.logClientDisconnects", false);
        changed |= applyDefault(config, "host.bind.address", "");
        changed |= applyDefault(config, "host.bind.port", config.getInt("port", 8085));
        changed |= applyDefault(config, "host.public.scheme", "http");
        changed |= applyDefault(config, "host.public.host",
                config.contains("ip") ? config.getString("ip") : "127.0.0.1");
        changed |= applyDefault(config, "host.public.port", config.getInt("host.bind.port", config.getInt("port", 8085)));
        changed |= applyDefault(config, "host.allowOnlyOnlinePlayerIps", false);
        changed |= applyDefault(config, "host.requireTokens", false);
        changed |= applyDefault(config, "host.tokenTtlSeconds", 300);
        changed |= applyDefault(config, "host.api.enabled", false);
        changed |= applyDefault(config, "host.api.path", "/api/pack-link");
        changed |= applyDefault(config, "host.api.allowedIps", java.util.List.of());

        if (changed) {
            saveConfig();
        }
    }

    private boolean applyDefault(FileConfiguration config, String path, Object value) {
        if (config.contains(path)) {
            return false;
        }

        config.set(path, value);
        return true;
    }

    public static RoseRP getInstance() {
        return instance;
    }

    public static HostService getHosting() {
        return host;
    }

    public static PluginCFG getPluginConfig() {
        return instance.pluginConfig;
    }

    public static MessagesFile getMessages() {
        return instance.messages;
    }

    public static BukkitAudiences getAdventure() {
        if (instance.adventure == null) {
            throw new IllegalStateException("Adventure is not available (plugin disabled)");
        }
        return instance.adventure;
    }

    public static boolean hasPack(String name) {
        return instance.packs.containsKey(name);
    }

    public static Pack getPack(String name) {
        return instance.packs.get(name);
    }

    public static void logInfo(String text) {
        logger.info(text);
    }

    public static void logWarning(String text) {
        logger.warning(text);
    }

    public static void logSevere(String text) {
        logger.severe(text);
    }
}
