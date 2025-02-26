package me.emsockz.roserp.file.config;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.util.ServerIPFetcher;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginCFG {

    public final String LANG;
    public final Boolean CHECK_UPDATE;
    public final String IP;
    public final Integer PORT;
    public final String HOST_URL;
    public final List<String> IGNORE_FILES;
    public final List<String> JOIN_PACKS;
    public final boolean resetPackOnLeave;

    public PluginCFG() {
        FileConfiguration cfg = RoseRP.getInstance().getConfig();
        LANG = cfg.getString("lang");
        CHECK_UPDATE = cfg.getBoolean("checkUpdate", true);
        PORT = cfg.getInt("port");

        if (cfg.contains("ip")) {
            IP = cfg.getString("ip");
        } else {
            IP = ServerIPFetcher.getPublicIP();
        }

        HOST_URL = "http://" + IP + ":" + PORT;
        IGNORE_FILES = cfg.getStringList("ignoreFiles");
        JOIN_PACKS = cfg.getStringList("joinPacks");
        resetPackOnLeave = cfg.getBoolean("resetPackOnLeave");
    }
}
