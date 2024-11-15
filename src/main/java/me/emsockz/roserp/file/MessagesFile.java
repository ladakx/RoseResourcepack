package me.emsockz.roserp.file;

import me.emsockz.roserp.RoseRP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessagesFile {
   private final String path;
   private File file;
   private FileConfiguration config;

   public MessagesFile() {
      super();
      this.path = "lang/messages_" + RoseRP.getInstance().getPluginConfig().LANG + ".yml";

      if (!(new File(RoseRP.getInstance().getDataFolder(), this.path)).exists()) {
         RoseRP.getInstance().saveResource(this.path, false);
         this.file = new File(RoseRP.getInstance().getDataFolder(), this.path);
         this.config = YamlConfiguration.loadConfiguration(this.file);
      } else {
         this.file = new File(RoseRP.getInstance().getDataFolder(), this.path);
         this.config = YamlConfiguration.loadConfiguration(this.file);
      }
   }

   public void save() {
      this.file = new File(RoseRP.getInstance().getDataFolder(), this.path);

      try {
         this.config.save(this.file);
      } catch (IOException exception) {
         throw new RuntimeException("Failed to save file " + this.path, exception);
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
   }

   public void reload() {
      this.file = new File(RoseRP.getInstance().getDataFolder(), this.path);
      this.config = YamlConfiguration.loadConfiguration(this.file);
   }

   public FileConfiguration getConfig() {
      return this.config;
   }
}
