package me.emsockz.roserp;

import me.emsockz.roserp.file.config.MessagesCFG;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by EncryptDev
 */
public class UpdateChecker implements Listener {

    private static final String url = "https://api.spigotmc.org/legacy/update.php?resource=";
    private static final String id = "107483";

    private boolean isAvailable;

    public UpdateChecker() {
        if (RoseRP.getPluginConfig().CHECK_UPDATE) {
            Bukkit.getScheduler().runTaskTimer(RoseRP.getInstance(), this::check,0, 72000);
        }
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        if (event.getPlayer().isOp()) {
            if (isAvailable) {
                MessagesCFG.UPDATE.sendMessage(event.getPlayer());
            }
        }
    }

    public void check() {
        isAvailable = checkUpdate();
    }

    private boolean checkUpdate() {
        RoseRP.logInfo("Check for updates...");
        try {
            String localVersion = RoseRP.getInstance().getDescription().getVersion();
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url + id).openConnection();
            connection.setRequestMethod("GET");
            String raw = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();

            String remoteVersion;
            if(raw.contains("-")) {
                remoteVersion = raw.split("-")[0].trim();
            } else {
                remoteVersion = raw;
            }

            if(!localVersion.equalsIgnoreCase(remoteVersion))
                return true;

        } catch (IOException e) {
            return false;
        }

        return false;
    }
}