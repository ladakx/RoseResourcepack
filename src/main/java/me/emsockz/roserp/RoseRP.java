package me.emsockz.roserp;

import me.emsockz.roserp.api.HostService;
import me.emsockz.roserp.bstats.Metrics;
import me.emsockz.roserp.commands.SubCommandManager;
import me.emsockz.roserp.commands.TabCommandManager;
import me.emsockz.roserp.file.MessagesFile;
import me.emsockz.roserp.file.config.MessagesCFG;
import me.emsockz.roserp.file.config.PluginCFG;
import me.emsockz.roserp.host.Hosting;
import me.emsockz.roserp.listeners.RPEventListener;
import me.emsockz.roserp.pack.Applier;
import me.emsockz.roserp.packer.Packer;
import me.emsockz.roserp.pack.Pack;
import me.emsockz.roserp.util.MinecraftVersions;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    @Override
    public void onEnable() {
        instance = this;
        RoseRPLogger.init(this);
        adventure = BukkitAudiences.create(this);

        Metrics metrics = new Metrics(this, 23796);
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));

        loadMessagesFiles();
        saveDefaultConfig();
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

        loadResourcepacks();

        host = new Hosting(this, pluginConfig.PORT, Runtime.getRuntime().availableProcessors());
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
        }, 1200L, 1200L); // каждую 1 минут
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
            CompletableFuture.runAsync(() -> Packer.packFiles(pack))
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            RoseRPLogger.error("Failed to pack resourcepack " + pack.getName(), ex);
                        } else {
                            MessagesCFG.RP_SUCCESSFULLY_PACKED.sendMessage(
                                    Bukkit.getConsoleSender(), "{pack}", pack.getName());
                        }
                    });
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        messages.reload();

        pluginConfig = new PluginCFG();
        MessagesCFG.refreshAll();

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
