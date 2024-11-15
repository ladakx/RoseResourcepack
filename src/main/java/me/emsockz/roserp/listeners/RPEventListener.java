package me.emsockz.roserp.listeners;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.file.config.MessagesCFG;
import me.emsockz.roserp.pack.Applier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class RPEventListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!RoseRP.getPluginConfig().JOIN_PACKS.isEmpty())
            Applier.applyJoinPacks(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (!RoseRP.getPluginConfig().resetPackOnLeave) {
            if (RoseRP.getInstance().players.containsKey(event.getPlayer().getUniqueId())) {
                Applier.clearPacks(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onResourcepackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            MessagesCFG.DOWNLOADING.sendMessage(player);
        }

        else if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            MessagesCFG.DOWNLOAD_COMPLETE.sendMessage(player);
        }

        else if (event.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            MessagesCFG.DOWNLOAD_FAILED.sendMessage(player);
        }

        else if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
            MessagesCFG.DOWNLOAD_FAILED.sendMessage(player);
        }
    }
}
