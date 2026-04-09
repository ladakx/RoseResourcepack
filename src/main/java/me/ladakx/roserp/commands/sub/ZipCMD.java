package me.ladakx.roserp.commands.sub;

import me.ladakx.roserp.RoseRP;
import me.ladakx.roserp.commands.SubCommandModel;
import me.ladakx.roserp.file.config.MessagesCFG;
import me.ladakx.roserp.packer.Packer;
import me.ladakx.roserp.pack.Pack;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ZipCMD extends SubCommandModel {
    public ZipCMD() {
        super();
        this.setPlayerCommand(false);
    }

    public boolean execute() {
        if (this.checkPermission("roserp.commands.zip", true)) {
            if (this.args.length != 2) {
                this.sendMessage(MessagesCFG.HELP_COMMAND_ADMIN);
            }

            else if (this.args[1].equalsIgnoreCase("all")) {
                for (Map.Entry<String, Pack> entry : RoseRP.getInstance().packs.entrySet()) {
                    String id = entry.getKey();
                    Pack pack = entry.getValue();

                    CompletableFuture.runAsync(() -> {
                        Packer.packFiles(pack);
                    }).whenComplete((result, ex) -> {
                        MessagesCFG.RP_SUCCESSFULLY_PACKED.sendMessage(sender, "{pack}", id);
                    });
                }
            }

            else if (RoseRP.getPack(this.args[1]) == null) {
                this.sendMessage(MessagesCFG.PACK_NOT_FOUND);
            }

            else {
                CompletableFuture.runAsync(() -> {
                    Packer.packFiles(RoseRP.getPack(this.args[1]));
                }).whenComplete((result, ex) -> {
                    MessagesCFG.RP_SUCCESSFULLY_PACKED.sendMessage(sender, "{pack}", this.args[1]);
                });
            }
        }

        return true;
    }
}
