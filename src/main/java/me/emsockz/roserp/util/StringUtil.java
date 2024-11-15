package me.emsockz.roserp.util;

import me.emsockz.roserp.RoseRP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {

    /**
     * Converts a Component to a String.
     * @param component The Component to be converted.
     * @return The converted String.
     */
    public static String parseComponent(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    /**
     * Converts a String to a Component.
     * @param string The String to be converted.
     * @return The converted Component.
     */
    public static Component parseString(String string) {
        if (string == null) return null;
        return MiniMessage.miniMessage().deserialize(parseLegacyString(string));
    }

    /**
     * Retrieves a List<String> message from a config and converts it to a List<Component>.
     * @param string The key of the message in the config.
     * @return The converted List<Component>.
     */
    public static List<Component> getMessage(String string) {
        String prefix = RoseRP.getInstance().getMessages().getConfig().getString("Prefix", "");
        List<String> text = RoseRP.getInstance().getMessages().getConfig().getStringList(string);
        List<Component> result = new ArrayList<>();

        for (String s : text) {
            result.add(MiniMessage.miniMessage().deserialize(parseLegacyString(s.replace("{prefix}", prefix))));
        }

        return result;
    }

    /**
     * Replaces text in a MiniMessage component with other text.
     *
     * @param component The component to replace text in.
     * @param replaces A arrays containing text to replace and the text to replace it with.
     * @return The component with text replaced.
     */
    public static Component replace(Component component, String... replaces) {
        if (replaces == null || replaces.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid replaces");
        }

        String result = parseComponent(component);

        for (int i = 0; i < replaces.length; i += 2) {
            result = result.replace(replaces[i], replaces[i+1]);
        }

        return parseString(result);
    }

    /**
     * Converts "Legacy" color codes (&c,&6,&l) to new ones (MiniMessage).
     *
     * @param string The string to convert.
     * @return The converted string.
     */
    public static String parseLegacyString(String string) {
        return string.replace("&", "§").replace("§r", "<reset>").replace("§0", "<black>").replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>").replace("§3", "<dark_aqua>").replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>").replace("§6", "<gold>").replace("§7", "<gray>")
                .replace("§8", "<dark_gray>").replace("§9", "<blue>").replace("§a", "<green>")
                .replace("§c", "<red>").replace("§d", "<light_purple>").replace("§b", "<aqua>")
                .replace("§f", "<white>").replace("§l", "<bold>").replace("§n", "<underlined>").replace("§e", "<yellow>")
                .replace("§o", "<i>").replace("§m", "<strikethrough>").replace("§k", "<obfuscated>");
    }
}
