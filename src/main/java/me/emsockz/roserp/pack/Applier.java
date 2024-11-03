package me.emsockz.roserp.pack;

import me.emsockz.roserp.RoseRP;
import org.bukkit.entity.Player;

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
            player.setResourcePack(
                    pack.getUUID(),
                    pack.getRpURL(),
                    pack.getHash(),
                    pack.getPrompt(),
                    pack.isRequired()
            );
        } else if (RoseRP.above1_18_1) {
            player.setResourcePack(
                    pack.getRpURL(),
                    pack.getHash(),
                    pack.getPrompt(),
                    pack.isRequired()
            );
        } else {
            player.setResourcePack(
                    pack.getRpURL(),
                    pack.getHash()
            );
        }
    }

    public static void apply(Player player, List<Pack> packs) {
        for (Pack pack : packs) {
            apply(player, pack);
        }
    }

    public static void removePack(Player player, Pack pack) {
        if (RoseRP.above1_20_4) {
            player.removeResourcePack(pack.getUUID());
        } else {
            player.setResourcePack("https://google.com");
        }
    }

    public static void removePack(Player player, List<Pack> packs) {
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
        if (RoseRP.above1_20_4) {
            player.removeResourcePacks();
        } else {
            player.setResourcePack("https://google.com");
        }
    }
}
