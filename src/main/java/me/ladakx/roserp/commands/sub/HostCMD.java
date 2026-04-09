package me.ladakx.roserp.commands.sub;

import me.ladakx.roserp.RoseRP;
import me.ladakx.roserp.api.HostDiagnostics;
import me.ladakx.roserp.api.HostService;
import me.ladakx.roserp.commands.SubCommandModel;
import me.ladakx.roserp.file.config.MessagesCFG;

import java.time.Duration;
import java.time.Instant;

public class HostCMD extends SubCommandModel {

    public HostCMD() {
        this.setPlayerCommand(false);
    }

    @Override
    public boolean execute() {
        if (!checkPermission("roserp.commands.host", true)) {
            return true;
        }

        HostService host = RoseRP.getHosting();
        if (host == null) {
            sendMessage(MessagesCFG.HOST_NOT_INITIALIZED);
            return true;
        }

        if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
            HostDiagnostics d = host.getDiagnostics();

            Instant now = Instant.now();

            String uptime = d.getStartedAt() == null
                    ? "n/a"
                    : Duration.between(d.getStartedAt(), now).toSeconds() + "s";

            String lastAcceptAgo = d.getLastAcceptAt() == null
                    ? "never"
                    : Duration.between(d.getLastAcceptAt(), now).toSeconds() + "s ago";

            String lastServeAgo = d.getLastServeAt() == null
                    ? "never"
                    : Duration.between(d.getLastServeAt(), now).toSeconds() + "s ago";

            MessagesCFG.HOST_STATUS_LINE.sendMessage(aud, "{key}", "uptime", "{value}", uptime);
            MessagesCFG.HOST_STATUS_LINE.sendMessage(aud, "{key}", "lastAcceptAgo", "{value}", lastAcceptAgo);
            MessagesCFG.HOST_STATUS_LINE.sendMessage(aud, "{key}", "lastServeAgo", "{value}", lastServeAgo);

            return true;
        }

        sendHelp();
        return true;
    }
}
