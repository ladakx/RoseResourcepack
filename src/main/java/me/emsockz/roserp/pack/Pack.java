package me.emsockz.roserp.pack;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.util.Hash;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Pack {

    private final String name;
    private final String rpURL;
    private final String zipArchiveName;
    private final boolean enableHash;
    private final boolean protect;
    private final List<ConnectedPack> connectedPacks;
    private final String prompt;
    private final boolean required;

    private UUID uuid;
    private File path = null;
    private byte[] hash = null;

    public Pack(String name, FileConfiguration cfg) {
        this.name = name;
        this.enableHash = cfg.getBoolean("packs." + name + ".enableHash", true);
        this.protect = cfg.getBoolean("packs." + name + ".protect", true);
        this.zipArchiveName = cfg.getString("packs." + name + ".zipArchiveName", name);
        this.rpURL = RoseRP.getPluginConfig().HOST_URL + "/" + zipArchiveName + ".zip";

        this.prompt = cfg.getString("packs."+name+".prompt", "");
        this.required = cfg.getBoolean("packs."+name+".required", false);

        this.connectedPacks = new ArrayList<>();
        if (cfg.contains("packs." + name + ".connectedPacks")) {
            for (String pack : cfg.getConfigurationSection("packs." + name + ".connectedPacks").getKeys(false)) {
                this.connectedPacks.add(new ConnectedPack(
                        cfg.getString("packs." + name + ".connectedPacks."+pack+".path", ""),
                        cfg.getStringList("packs." + name + ".connectedPacks."+pack+".skipFiles")
                ));
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public byte[] getHash() {
        return hash;
    }

    public File getPath() {
        return path;
    }

    public List<ConnectedPack> getConnectedPacks() {
        return connectedPacks;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isProtect() {
        return protect;
    }

    public String getZip() {
        return RoseRP.getInstance().getDataFolder().getPath() + File.separator + "resourcepacks" + File.separator + this.name + File.separator + this.zipArchiveName + ".zip";
    }

    public String getRpURL() {
        return this.rpURL;
    }

    public boolean isEnableHash() {
        return this.enableHash;
    }

    public String getZipArchiveName() {
        return this.zipArchiveName;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void init(File path) {
        this.path = path;
        this.uuid = loadOrGenerateUUID();

        if (isEnableHash()) {
            this.hash = Hash.getSHA1Checksum(path.getPath());
        }
    }

    private UUID loadOrGenerateUUID() {
        File cacheFile = new File(RoseRP.getInstance().getDataFolder(), ".cache/" + name + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);

        if (cacheFile.exists()) {
            String uuidString = config.getString("uuid");
            if (uuidString != null) {
                return UUID.fromString(uuidString);
            }
        }

        UUID newUUID = UUID.randomUUID();
        saveUUIDToCache(newUUID, config, cacheFile);
        return newUUID;
    }

    private void saveUUIDToCache(UUID uuid, YamlConfiguration config, File cacheFile) {
        config.set("uuid", uuid.toString());
        try {
            config.save(cacheFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String toString() {
        return "Pack{" +
                "name='" + name + '\'' +
                ", rpURL='" + rpURL + '\'' +
                ", zipArchiveName='" + zipArchiveName + '\'' +
                ", enableHash=" + enableHash +
                ", connectedPacks=" + connectedPacks +
                ", prompt='" + prompt + '\'' +
                ", required=" + required +
                ", path=" + path +
                ", hash=" + Arrays.toString(hash) +
                '}';
    }
}
