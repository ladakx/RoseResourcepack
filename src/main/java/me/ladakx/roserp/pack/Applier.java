package me.ladakx.roserp.pack;

import me.ladakx.roserp.RoseRP;
import me.ladakx.roserp.RoseRPLogger;
import me.ladakx.roserp.file.config.MessagesCFG;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.List;
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
                String packUrl = resolvePackUrl(player, pack);
                if (RoseRP.above1_20_3) {
                    player.setResourcePack(
                            pack.getUUID(),
                            packUrl,
                            pack.getHash(),
                            pack.getPrompt(),
                            pack.isRequired()
                        );
                } else {
                    send(player, pack, packUrl);
                }
            } catch (Exception e) {
                RoseRPLogger.error("Failed to set resource pack for player " + player.getName() + ": " + e.getMessage(), e);
                MessagesCFG.DOWNLOAD_FAILED.sendMessage(player);
            }
        };

        if (RoseRP.getSchedulerAdapter().isOwnedByCurrentRegion(player)) {
            setRp.run();
        } else {
            RoseRP.getSchedulerAdapter().runEntity(player, setRp);
        }
    }

    public static void send(Player player, Pack pack) {
        send(player, pack, resolvePackUrl(player, pack));
    }

    private static void send(Player player, Pack pack, String packUrl) {
        if (player == null || pack == null) return;

        CopyOnWriteArrayList<Pack> packList =
                RoseRP.getInstance().players.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArrayList<>());
        packList.remove(pack);
        packList.add(pack);

        Runnable op = () -> {
            try {
                sendLegacyResourcePack(player, pack, packUrl);
            } catch (Exception e) {
                RoseRPLogger.error("Failed to send resource pack to " + player.getName() + ": " + e.getMessage(), e);
                MessagesCFG.DOWNLOAD_FAILED.sendMessage(player);
            }
        };

        if (RoseRP.getSchedulerAdapter().isOwnedByCurrentRegion(player)) op.run();
        else RoseRP.getSchedulerAdapter().runEntity(player, op);
    }

    public static void apply(Player player, List<Pack> packs) {
        if (player == null || packs == null) return;
        CopyOnWriteArrayList<Pack> packList = new CopyOnWriteArrayList<>(packs);
        RoseRP.getInstance().players.put(player.getUniqueId(), packList);

        if (!RoseRP.above1_20_3) {
            if (!packs.isEmpty()) {
                Pack latestPack = packs.get(packs.size() - 1);
                send(player, latestPack, resolvePackUrl(player, latestPack));
            }
            return;
        }

        // Apply sequentially on main thread (ensure order)
        Runnable op = () -> {
            for (Pack p : packs) {
                try {
                    String packUrl = resolvePackUrl(player, p);
                    if (RoseRP.above1_20_3) {
                        player.setResourcePack(
                                p.getUUID(),
                                packUrl,
                                p.getHash(),
                                p.getPrompt(),
                                p.isRequired()
                        );
                    }
                } catch (Exception e) {
                    RoseRPLogger.warn("Failed to apply pack " + p.getName() + " to player " + player.getName() + ": " + e.getMessage());
                }
            }
        };

        if (RoseRP.getSchedulerAdapter().isOwnedByCurrentRegion(player)) op.run();
        else RoseRP.getSchedulerAdapter().runEntity(player, op);
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
                    restoreLegacyPacks(player);
                }
            } catch (Exception e) {
                RoseRPLogger.warn("Failed to remove pack for player " + player.getName() + ": " + e.getMessage());
            }
        };

        if (RoseRP.getSchedulerAdapter().isOwnedByCurrentRegion(player)) op.run();
        else RoseRP.getSchedulerAdapter().runEntity(player, op);
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
                    restoreLegacyPacks(player);
                }
            } catch (Exception e) {
                RoseRPLogger.warn("Failed to remove packs for player " + player.getName() + ": " + e.getMessage());
            }
        };

        if (RoseRP.getSchedulerAdapter().isOwnedByCurrentRegion(player)) op.run();
        else RoseRP.getSchedulerAdapter().runEntity(player, op);
    }

    public static void clearPacks(Player player) {
        if (player == null) return;
        RoseRP.getInstance().players.remove(player.getUniqueId());

        Runnable op = () -> {
            try {
                if (RoseRP.above1_20_4) {
                    player.removeResourcePacks();
                } else {
                    clearLegacyResourcePack(player);
                }
            } catch (Exception e) {
                RoseRPLogger.warn("Failed to clear packs for player " + player.getName() + ": " + e.getMessage());
            }
        };

        if (RoseRP.getSchedulerAdapter().isOwnedByCurrentRegion(player)) op.run();
        else RoseRP.getSchedulerAdapter().runEntity(player, op);
    }

    private static String resolvePackUrl(Player player, Pack pack) {
        if (!(RoseRP.getHosting() instanceof me.ladakx.roserp.host.Hosting)) {
            return pack.getRpURL();
        }
        me.ladakx.roserp.host.Hosting hosting = (me.ladakx.roserp.host.Hosting) RoseRP.getHosting();

        String clientIp = null;
        InetSocketAddress address = player.getAddress();
        if (address != null && address.getAddress() != null) {
            clientIp = address.getAddress().getHostAddress();
        }

        return hosting.getPackUrl(pack.getName(), clientIp).orElse(pack.getRpURL());
    }

    private static void restoreLegacyPacks(Player player) {
        CopyOnWriteArrayList<Pack> remainingPacks = RoseRP.getInstance().players.get(player.getUniqueId());
        if (remainingPacks == null || remainingPacks.isEmpty()) {
            clearLegacyResourcePack(player);
            return;
        }

        Pack latestPack = remainingPacks.get(remainingPacks.size() - 1);
        sendLegacyResourcePack(player, latestPack, resolvePackUrl(player, latestPack));
    }

    private static void sendLegacyResourcePack(Player player, Pack pack, String packUrl) {
        if (RoseRP.above1_18_1) {
            player.setResourcePack(
                    packUrl,
                    pack.getHash(),
                    pack.getPrompt(),
                    pack.isRequired()
            );
        } else {
            player.setResourcePack(packUrl, pack.getHash());
        }
    }

    private static void clearLegacyResourcePack(Player player) {
        if (RoseRP.above1_18_1) {
            player.setResourcePack("", new byte[0], "", false);
        } else if (RoseRP.above1_11_2) {
            player.setResourcePack("", new byte[0]);
        }
    }
}
