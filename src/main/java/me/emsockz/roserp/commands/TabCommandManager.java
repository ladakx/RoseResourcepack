package me.emsockz.roserp.commands;

import me.emsockz.roserp.RoseRP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabCommandManager implements TabCompleter {

    public TabCommandManager() {
        super();
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("roserp.help.admin")) {
                return listSubCommandAdmin;
            } else {
                return sender.hasPermission("roserp.help") ? listSubCommandUser : null;
            }
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            List<String> r = new ArrayList<>(RoseRP.getInstance().packs.keySet());
            r.addAll(getPlayers());
            return r;
        }

        else if (args.length >= 3 && args[0].equalsIgnoreCase("reset")) {
            return new ArrayList<>(RoseRP.getInstance().packs.keySet());
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("texture")) {
            List<String> r = new ArrayList<>(RoseRP.getInstance().packs.keySet());
            r.addAll(getPlayers());
            return r;
        }

        else if (args.length >= 3 && args[0].equalsIgnoreCase("texture")) {
            return new ArrayList<>(RoseRP.getInstance().packs.keySet());
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("zip")) {
            List<String> arr = new ArrayList<>(RoseRP.getInstance().packs.keySet());
            arr.add("all");
            return arr;
        }

        else if (args.length == 2 && args[0].equalsIgnoreCase("host")) {
            return List.of("status");
        }

        return null;
    }

    private static List<String> getPlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private static final List<String> listSubCommandAdmin =
            List.of("help", "reload", "reset", "zip", "texture", "host");
    private static final List<String> listSubCommandUser = List.of("help", "texture");
}
