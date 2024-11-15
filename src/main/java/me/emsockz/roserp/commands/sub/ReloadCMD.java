package me.emsockz.roserp.commands.sub;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.commands.SubCommandModel;
import me.emsockz.roserp.file.config.MessagesCFG;

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
