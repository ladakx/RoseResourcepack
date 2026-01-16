package me.emsockz.roserp.commands;

import me.emsockz.roserp.commands.sub.*;
import me.emsockz.roserp.file.config.MessagesCFG;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SubCommandManager implements CommandExecutor {

    public static Map<String, SubCommandModel> subcommands = new HashMap<>();

    public SubCommandManager() {
        subcommands.put("reload", new ReloadCMD());
        subcommands.put("help", new HelpCMD());
        subcommands.put("zip", new ZipCMD());
        subcommands.put("reset", new ResetCMD());
        subcommands.put("host", new HostCMD());
        subcommands.put("texture", new TextureCMD());
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            subcommands.get("help").onExecute(sender, command, label, args);
        } else {
            String subcommand = args[0].toLowerCase();
            if (subcommands.get(subcommand) == null) {
                MessagesCFG.COMMAND_DOES_NOT_EXIST.sendMessage(sender);
            } else {
                subcommands.get(subcommand).onExecute(sender, command, label, args);
            }
        }
        return true;
    }
}
