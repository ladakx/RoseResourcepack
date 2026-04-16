package me.ladakx.roserp.util;

import me.ladakx.roserp.RoseRP;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {

    public static String colorize(String string) {
        if (string == null) return null;
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static List<String> getMessage(String string) {
        String prefix = RoseRP.getInstance().getMessages().getConfig().getString("Prefix", "");
        List<String> text = RoseRP.getInstance().getMessages().getConfig().getStringList(string);
        List<String> result = new ArrayList<>();

        for (String s : text) {
            result.add(colorize(s.replace("{prefix}", prefix)));
        }

        return result;
    }

    public static String replace(String text, String... replaces) {
        if (replaces == null || replaces.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid replaces");
        }

        String result = text;
        for (int i = 0; i < replaces.length; i += 2) {
            result = result.replace(replaces[i], replaces[i + 1]);
        }

        return result;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
