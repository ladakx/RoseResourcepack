package me.emsockz.roserp.file.config;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.util.StringUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;

public enum MessagesCFG {
    UPDATE("Update"),
    RELOAD_PLUGIN("ReloadPlugin"),
    NO_PERMISSIONS("NoPerm"),
    NOT_FOR_CONSOLE("NotForConsole"),
    HELP_COMMAND("HelpCommand"),
    HELP_COMMAND_ADMIN("HelpCommandAdmin"),
    RP_SUCCESSFULLY_PACKED("RPSuccessfullyPacked"),
    PLAYER_NOT_FOUND("PlayerNotFound"),
    PACK_NOT_FOUND("PackNotFound"),
    DOWNLOAD_FAILED("DownloadFailed"),
    DOWNLOAD_COMPLETE("DownloadComplete"),
    DOWNLOADING("Downloading"),
    APPLY_OTHER_RESOURCEPACKS("ApplyOtherResourcepacks"),
    RESET_OTHER_RESOURCEPACKS("ResetOtherResourcepacks"),
    RESET_COMPLETE("ResetComplete"),
    COMMAND_DOES_NOT_EXIST("CommandNotFound"),
    HOST_STATUS_HEADER("HostStatusHeader"),
    HOST_STATUS_LINE("HostStatusLine"),
    HOST_NOT_INITIALIZED("HostNotInitialized"),
    HOST_STARTED("HostStarted"),
    HOST_STOPPED("HostStopped"),
    HOST_ERROR("HostError"),
    HOST_FILE_NOT_FOUND("HostFileNotFound"),
    HOST_INVALID_REQUEST("HostInvalidRequest");

    private final String path;
    private List<Component> text;

    MessagesCFG(String path) {
        this.path = path;
        this.text = StringUtil.getMessage(path);
    }

    public String getPath() {
        return this.path;
    }

    public List<Component> getText() {
        return this.text;
    }

    public void refresh() {
        text = StringUtil.getMessage(path);
    }

    public void sendMessage(Audience audience) {
        for (Component component : text) {
            audience.sendMessage(component);
        }
    }

    public void sendMessage(Audience audience, String... replaces) {
        for (Component component : text) {
            audience.sendMessage(StringUtil.replace(component, replaces));
        }
    }

    public void sendMessage(CommandSender sender) {
        sendMessage(RoseRP.getAdventure().sender(sender));
    }

    public void sendMessage(CommandSender sender, String... replaces) {
        sendMessage(RoseRP.getAdventure().sender(sender), replaces);
    }

    public static void refreshAll() {
        for (MessagesCFG e : values()) e.refresh();
    }
}
