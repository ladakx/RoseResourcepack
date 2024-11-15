package me.emsockz.roserp.commands.sub;

import me.emsockz.roserp.commands.SubCommandModel;
import me.emsockz.roserp.file.config.MessagesCFG;

public class HelpCMD extends SubCommandModel {
   public HelpCMD() {
      this.setPlayerCommand(false);
   }

   public boolean execute() {
      if (this.checkPermission("roserp.commands.help.admin", false)) {
         this.sendMessage(MessagesCFG.HELP_COMMAND_ADMIN);
      } else if (this.checkPermission("roserp.commands.help", false)) {
         this.sendMessage(MessagesCFG.HELP_COMMAND);
      } else {
         this.sendMessage(MessagesCFG.NO_PERMISSIONS);
      }

      return true;
   }
}
