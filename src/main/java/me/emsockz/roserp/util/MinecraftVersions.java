package me.emsockz.roserp.util;

import org.bukkit.Bukkit;

public class MinecraftVersions {

    /** Check minecraft server version */
    public static boolean isCoreVersionAboveOrEqual(String version) {
        String coreVersion = Bukkit.getBukkitVersion().split("-")[0];

        int comprison = compareVersions(coreVersion, version);
        return comprison >= 0;
    }

    /** Compare versions */
    private static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int part1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int part2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;

            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }

        return 0;
    }
}
