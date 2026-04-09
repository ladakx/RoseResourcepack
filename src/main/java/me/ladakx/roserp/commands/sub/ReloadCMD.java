package me.ladakx.roserp.commands.sub;

import me.ladakx.roserp.RoseRP;
import me.ladakx.roserp.commands.SubCommandModel;
import me.ladakx.roserp.file.config.MessagesCFG;

public class ReloadCMD extends SubCommandModel {

    public ReloadCMD() {
        super();
        this.setPlayerCommand(false);
    }

    public boolean execute() {
        if (this.checkPermission("roserp.commands.reload", true)) {
            RoseRP.getInstance().reloadPlugin();
            this.sendMessage(MessagesCFG.RELOAD_PLUGIN);
        }
        return true;
    }
}
