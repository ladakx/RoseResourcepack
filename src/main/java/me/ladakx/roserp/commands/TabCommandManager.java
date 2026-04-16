package me.ladakx.roserp.commands;

import me.ladakx.roserp.RoseRP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TabCommandManager implements TabCompleter {

    public TabCommandManager() {
        super();
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (sender.isOp() || sender.hasPermission("roserp.commands.help.admin")) {
                return listSubCommandAdmin;
            } else {
                return sender.hasPermission("roserp.commands.help") ? listSubCommandUser : null;
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
            return Collections.singletonList("status");
        }

        return null;
    }

    private static List<String> getPlayers() {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }

    private static final List<String> listSubCommandAdmin =
            Arrays.asList("help", "reload", "reset", "zip", "texture", "host");
    private static final List<String> listSubCommandUser = Arrays.asList("help", "texture");
}
