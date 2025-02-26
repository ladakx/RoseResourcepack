package me.emsockz.roserp.pack;

import me.emsockz.roserp.RoseRP;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Applier {

    public static void applyJoinPacks(Player player) {
        List<String> list = RoseRP.getPluginConfig().JOIN_PACKS;
        for (String pack : list) {
            if (RoseRP.hasPack(pack)) {
                apply(player, RoseRP.getPack(pack));
            }
        }
    }

    public static void apply(Player player, Pack pack) {
        if (RoseRP.above1_20_3) {
            if (RoseRP.getInstance().players.containsKey(player.getUniqueId())) {
                List<Pack> p = RoseRP.getInstance().players.get(player.getUniqueId());
                try {p.remove(pack);
                } catch (Exception ignored) {}

                p.add(pack);
                RoseRP.getInstance().players.put(player.getUniqueId(), p);
            } else {
                List<Pack> packs = new ArrayList<>();
                packs.add(pack);
                RoseRP.getInstance().players.put(player.getUniqueId(), packs);
            }

            player.setResourcePack(
                    pack.getUUID(),
                    pack.getRpURL(),
                    pack.getHash(),
                    pack.getPrompt(),
                    pack.isRequired()
            );
        } else send(player, pack);
    }

    public static void send(Player player, Pack pack) {
        if (RoseRP.above1_18_1) {
            List<Pack> packs = new ArrayList<>();
            packs.add(pack);
            RoseRP.getInstance().players.put(player.getUniqueId(), packs);
            player.setResourcePack(
                    pack.getRpURL(),
                    pack.getHash(),
                    pack.getPrompt(),
                    pack.isRequired()
            );
        } else {
            List<Pack> packs = new ArrayList<>();
            packs.add(pack);
            RoseRP.getInstance().players.put(player.getUniqueId(), packs);
            player.setResourcePack(
                    pack.getRpURL(),
                    pack.getHash()
            );
        }
    }

    public static void apply(Player player, List<Pack> packs) {
        RoseRP.getInstance().players.put(player.getUniqueId(), packs);
        for (Pack pack : packs) {
            apply(player, pack);
        }
    }

    public static void removePack(Player player, Pack pack) {
        if (RoseRP.getInstance().players.containsKey(player.getUniqueId())) {
            List<Pack> p = RoseRP.getInstance().players.get(player.getUniqueId());
            try {p.remove(pack);
            } catch (Exception ignored) {};


            if (p.isEmpty()) {
                RoseRP.getInstance().players.remove(player.getUniqueId());
            }
        }

        if (RoseRP.above1_20_4) {
            player.removeResourcePack(pack.getUUID());
        } else {
            player.setResourcePack("https://google.com");
        }
    }

    public static void removePack(Player player, List<Pack> packs) {
        if (RoseRP.getInstance().players.containsKey(player.getUniqueId())) {
            List<Pack> p = RoseRP.getInstance().players.get(player.getUniqueId());
            p.removeAll(packs);

            if (p.isEmpty()) {
                RoseRP.getInstance().players.remove(player.getUniqueId());
            }
        }

        if (RoseRP.above1_20_4) {
            List<UUID> uuids = packs.stream()
                    .map(Pack::getUUID)
                    .toList();

            for (UUID uuid : uuids) {
                player.removeResourcePack(uuid);
            }
        } else {
            player.setResourcePack("https://google.com");
        }
    }

    public static void clearPacks(Player player) {
        RoseRP.getInstance().players.remove(player.getUniqueId());
        if (RoseRP.above1_20_4) {
            player.removeResourcePacks();
        } else {
            player.setResourcePack("https://google.com");
        }
    }
}
