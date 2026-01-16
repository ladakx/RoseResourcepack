package me.emsockz.roserp.pack;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.RoseRPLogger;
import me.emsockz.roserp.file.config.MessagesCFG;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe applier. All Bukkit API access is executed on the main thread.
 */
public class Applier {

    public static void applyJoinPacks(Player player) {
        if (player == null) return;
        List<String> list = RoseRP.getPluginConfig().JOIN_PACKS;
        for (String pack : list) {
            if (pack == null) continue;
            if (RoseRP.hasPack(pack)) {
                apply(player, RoseRP.getPack(pack));
            } else {
                MessagesCFG.PACK_NOT_FOUND.sendMessage(player, "{pack}", pack);
            }
        }
    }

    public static void apply(Player player, Pack pack) {
        if (player == null || pack == null) return;

        // Always update internal state synchronously (thread-safe structures)
        CopyOnWriteArrayList<Pack> packList =
                RoseRP.getInstance().players.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArrayList<>());
        // remove then add to keep latest order - safe in COW list
        packList.remove(pack);
        packList.add(pack);

        // Call Bukkit API on main thread
        Runnable setRp = () -> {
            try {
                if (RoseRP.above1_20_3) {
                    player.setResourcePack(
                            pack.getUUID(),
                            pack.getRpURL(),
                            pack.getHash(),
                            pack.getPrompt(),
                            pack.isRequired()
                    );
                } else {
                    send(player, pack);
                }
            } catch (Exception e) {
                RoseRPLogger.error("Failed to set resource pack for player " + player.getName() + ": " + e.getMessage(), e);
                MessagesCFG.DOWNLOAD_FAILED.sendMessage(player);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            setRp.run();
        } else {
            Bukkit.getScheduler().runTask(RoseRP.getInstance(), setRp);
        }
    }

    public static void send(Player player, Pack pack) {
        if (player == null || pack == null) return;

        CopyOnWriteArrayList<Pack> packList =
                RoseRP.getInstance().players.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArrayList<>());
        packList.clear();
        packList.add(pack);

        Runnable op = () -> {
            try {
                if (RoseRP.above1_18_1) {
                    player.setResourcePack(
                            pack.getRpURL(),
                            pack.getHash(),
                            pack.getPrompt(),
                            pack.isRequired()
                    );
                } else {
                    player.setResourcePack(pack.getRpURL(), pack.getHash());
                }
            } catch (Exception e) {
                RoseRPLogger.error("Failed to send resource pack to " + player.getName() + ": " + e.getMessage(), e);
                MessagesCFG.DOWNLOAD_FAILED.sendMessage(player);
            }
        };

        if (Bukkit.isPrimaryThread()) op.run();
        else Bukkit.getScheduler().runTask(RoseRP.getInstance(), op);
    }

    public static void apply(Player player, List<Pack> packs) {
        if (player == null || packs == null) return;
        CopyOnWriteArrayList<Pack> packList = new CopyOnWriteArrayList<>(packs);
        RoseRP.getInstance().players.put(player.getUniqueId(), packList);

        // Apply sequentially on main thread (ensure order)
        Runnable op = () -> {
            for (Pack p : packs) {
                try {
                    if (RoseRP.above1_20_3) {
                        player.setResourcePack(
                                p.getUUID(),
                                p.getRpURL(),
                                p.getHash(),
                                p.getPrompt(),
                                p.isRequired()
                        );
                    } else {
                        send(player, p);
                    }
                } catch (Exception e) {
                    RoseRPLogger.warn("Failed to apply pack " + p.getName() + " to player " + player.getName() + ": " + e.getMessage());
                }
            }
        };

        if (Bukkit.isPrimaryThread()) op.run();
        else Bukkit.getScheduler().runTask(RoseRP.getInstance(), op);
    }

    public static void removePack(Player player, Pack pack) {
        if (player == null || pack == null) return;
        CopyOnWriteArrayList<Pack> packList = RoseRP.getInstance().players.get(player.getUniqueId());
        if (packList != null) {
            packList.remove(pack);
            if (packList.isEmpty()) {
                RoseRP.getInstance().players.remove(player.getUniqueId());
            } else {
                RoseRP.getInstance().players.put(player.getUniqueId(), packList);
            }
        }

        Runnable op = () -> {
            try {
                if (RoseRP.above1_20_4) {
                    player.removeResourcePack(pack.getUUID());
                } else {
                    // Older versions: clear all resourcepacks gracefully
                    if (RoseRP.above1_18_1) {
                        player.setResourcePack(pack.getRpURL(), pack.getHash(), pack.getPrompt(), false);
                    } else {
                        // best-effort fallback
                        player.setResourcePack(pack.getRpURL(), pack.getHash());
                    }
                }
            } catch (Exception e) {
                RoseRPLogger.warn("Failed to remove pack for player " + player.getName() + ": " + e.getMessage());
            }
        };

        if (Bukkit.isPrimaryThread()) op.run();
        else Bukkit.getScheduler().runTask(RoseRP.getInstance(), op);
    }

    public static void removePack(Player player, List<Pack> packs) {
        if (player == null || packs == null) return;
        CopyOnWriteArrayList<Pack> packList = RoseRP.getInstance().players.get(player.getUniqueId());
        if (packList != null) {
            packList.removeAll(packs);
            if (packList.isEmpty()) {
                RoseRP.getInstance().players.remove(player.getUniqueId());
            } else {
                RoseRP.getInstance().players.put(player.getUniqueId(), packList);
            }
        }

        Runnable op = () -> {
            try {
                if (RoseRP.above1_20_4) {
                    for (Pack p : packs) {
                        player.removeResourcePack(p.getUUID());
                    }
                } else {
                    // fallback: reset to none
                    if (RoseRP.above1_18_1) {
                        // set a small empty resource pack? plugin previously used google.com — avoid that.
                        player.setResourcePack("", new byte[0], "", false);
                    }
                }
            } catch (Exception e) {
                RoseRPLogger.warn("Failed to remove packs for player " + player.getName() + ": " + e.getMessage());
            }
        };

        if (Bukkit.isPrimaryThread()) op.run();
        else Bukkit.getScheduler().runTask(RoseRP.getInstance(), op);
    }

    public static void clearPacks(Player player) {
        if (player == null) return;
        RoseRP.getInstance().players.remove(player.getUniqueId());

        Runnable op = () -> {
            try {
                if (RoseRP.above1_20_4) {
                    player.removeResourcePacks();
                } else {
                    // earlier versions: set empty resource pack or reset
                    player.setResourcePack("", new byte[0], "", false);
                }
            } catch (Exception e) {
                RoseRPLogger.warn("Failed to clear packs for player " + player.getName() + ": " + e.getMessage());
            }
        };

        if (Bukkit.isPrimaryThread()) op.run();
        else Bukkit.getScheduler().runTask(RoseRP.getInstance(), op);
    }
}
