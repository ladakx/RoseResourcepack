package me.ladakx.roserp.file.config;

import me.ladakx.roserp.util.StringUtil;
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
    private List<String> text;

    MessagesCFG(String path) {
        this.path = path;
        this.text = StringUtil.getMessage(path);
    }

    public String getPath() {
        return this.path;
    }

    public List<String> getText() {
        return this.text;
    }

    public void refresh() {
        text = StringUtil.getMessage(path);
    }

    public void sendMessage(CommandSender sender) {
        for (String line : text) {
            sender.sendMessage(line);
        }
    }

    public void sendMessage(CommandSender sender, String... replaces) {
        for (String line : text) {
            sender.sendMessage(StringUtil.replace(line, replaces));
        }
    }

    public static void refreshAll() {
        for (MessagesCFG e : values()) e.refresh();
    }
}
