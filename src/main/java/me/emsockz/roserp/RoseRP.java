package me.emsockz.roserp;

import me.emsockz.roserp.bstats.Metrics;
import me.emsockz.roserp.commands.SubCommandManager;
import me.emsockz.roserp.commands.TabCommandManager;
import me.emsockz.roserp.file.MessagesFile;
import me.emsockz.roserp.file.config.MessagesCFG;
import me.emsockz.roserp.file.config.PluginCFG;
import me.emsockz.roserp.host.Hosting;
import me.emsockz.roserp.listeners.RPEventListener;
import me.emsockz.roserp.packer.Packer;
import me.emsockz.roserp.pack.Pack;
import me.emsockz.roserp.util.MinecraftVersions;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class RoseRP extends JavaPlugin {

    // *****************************************************************************************************************
    // Logger
    private static final Logger logger = Logger.getLogger("Minecraft");

    public static final boolean above1_20_3 = MinecraftVersions.isCoreVersionAboveOrEqual("1.20.3");
    public static final boolean above1_20_4 = MinecraftVersions.isCoreVersionAboveOrEqual("1.20.4");
    public static final boolean above1_18_1 = MinecraftVersions.isCoreVersionAboveOrEqual("1.18.1");

    // *****************************************************************************************************************
    // Fields

    private static RoseRP instance = null;
    private static Hosting host = null;

    public final HashMap<String, Pack> packs = new HashMap<>();

    private BukkitAudiences adventure;

    private PluginCFG pluginConfig;

    private MessagesFile messages = null;


    public void onEnable() {
        instance = this;
        adventure = BukkitAudiences.create(instance);

        //metrics
        Metrics metrics = new Metrics(this, 23796);
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value")); // свои показатели


        this.loadMessagesFiles();
        this.saveDefaultConfig();
        if (instance.getConfig().getBoolean("loadDefaultFiles", false)) {
            loadDefaultFiles();
        }

        // load files
        pluginConfig = new PluginCFG();
        messages = new MessagesFile();
        MessagesCFG.refreshAll();

        // register command
        PluginCommand pluginCommand = instance.getCommand("roserp");
        assert pluginCommand != null;
        pluginCommand.setExecutor(new SubCommandManager());
        pluginCommand.setTabCompleter(new TabCommandManager());

        // register listeners
        Bukkit.getPluginManager().registerEvents(new RPEventListener(), this);

        // load resourcepack
        this.loadResourcepacks();

        // create web host
        host = new Hosting();
    }

    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }

        host.stop();
    }

    public void loadMessagesFiles() {
        for (String lang : Set.of("en", "ru")) {
            if (!(new File(instance.getDataFolder(), "lang/messages_" + lang + ".yml")).exists()) {
                instance.saveResource("lang/messages_" + lang + ".yml", false);
            }
        }
    }

    public void loadDefaultFiles() {
        saveFileWithoutWarn("resourcepacks/low_quality/pack.mcmeta");
        saveFileWithoutWarn("resourcepacks/main/pack.mcmeta");
        instance.getConfig().set("loadDefaultFiles", false);
        instance.saveConfig();
        instance.reloadConfig();
    }

    private void saveFileWithoutWarn(String path) {
        File file = new File(instance.getDataFolder(), path);
        if (!file.exists()) {
            instance.saveResource(path, false);
        }
    }

    public void loadResourcepacks() {
        FileConfiguration cfg = instance.getConfig();
        if (cfg.contains("packs")) {
            cfg.getConfigurationSection("packs").getKeys(false).forEach((pack) -> packs.put(pack,
                    new Pack(
                            pack,
                            cfg
                    )
            )
            );
        }

        for (Map.Entry<String, Pack> entry : packs.entrySet()) {
            String name = entry.getKey();
            Pack pack = entry.getValue();
            File path = new File(instance.getDataFolder(), "resourcepacks/" + name + "/" + pack.getZipArchiveName() + ".zip");

            if (path.exists()) {
                pack.init(path);
            }

            else {
                CompletableFuture.runAsync(
                        () -> Packer.packFiles(pack)
                ).whenComplete(
                        (result, ex) ->
                                MessagesCFG.RP_SUCCESSFULLY_PACKED
                                        .sendMessage(Bukkit.getConsoleSender(), "{pack}", pack.getName())
                );
            }
        }
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

    public static RoseRP getInstance() {
        return instance;
    }

    public static Hosting getHosting() {
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
            throw new IllegalStateException("Tried to access Adventure when the plugin was disables!");
        } else {
            return instance.adventure;
        }
    }

    public static boolean hasPack(String name) {
        return instance.packs.containsKey(name);
    }

    public static Pack getPack(String name) {
        return instance.packs.getOrDefault(name, null);
    }

    public void reloadPlugin() {
        instance.reloadConfig();
        messages.reload();

        // reload config && messages.yml
        pluginConfig = new PluginCFG();
        MessagesCFG.refreshAll();

        // reload packs
        packs.clear();
        loadResourcepacks();
    }
}
