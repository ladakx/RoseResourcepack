package me.emsockz.roserp.commands;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.file.config.MessagesCFG;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class SubCommandModel {
    protected CommandSender sender;
    protected Player player;
    protected Command command;
    protected boolean isPlayer;
    protected String[] args;
    protected Audience aud;
    private boolean isPlayerCommand;

    public SubCommandModel() {
        super();
    }

    public boolean onExecute(CommandSender sender, Command command, String s, String[] args) {
        this.sender = sender;
        this.isPlayer = sender instanceof Player;
        this.player = this.isPlayer ? (Player)sender : null;
        this.args = args;
        this.command = command;
        this.aud = this.isPlayer ? RoseRP.getInstance().getAdventure().player(this.player) : RoseRP.getInstance().getAdventure().console();
        if (this.isPlayerCommand && !this.isPlayer) {
            this.sendMessage(MessagesCFG.NOT_FOR_CONSOLE);
            return true;
        } else {
            return this.execute();
        }
    }

    public abstract boolean execute();

    public void sendHelp() {
        if (this.checkPermission("roserp.commands.help.admin", false)) {
            this.sendMessage(MessagesCFG.HELP_COMMAND_ADMIN);
        } else if (this.checkPermission("roserp.commands.help", false)) {
            this.sendMessage(MessagesCFG.HELP_COMMAND);
        } else {
            this.sendMessage(MessagesCFG.NO_PERMISSIONS);
        }

    }

    public boolean checkPermission(String permission, boolean showMSG) {
        if (this.sender.hasPermission(permission)) {
            return true;
        } else {
            if (showMSG) {
                this.sendMessage(MessagesCFG.NO_PERMISSIONS);
            }

            return false;
        }
    }

    public void setPlayerCommand(boolean playerCommand) {
        this.isPlayerCommand = playerCommand;
    }

    public void sendMessage(MessagesCFG msg) {
        msg.sendMessage(aud);
    }

    public void sendMessage(MessagesCFG msg, String... replaces) {
        msg.sendMessage(aud, replaces);
    }
}
