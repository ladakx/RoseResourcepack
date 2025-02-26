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
    private final boolean enableHash;
    private final boolean protect;
    private final List<ConnectedPack> connectedPacks;
    private final String prompt;
    private final boolean required;
    private final boolean replaceDuplicate;
    private final List<String> jsonMerges;

    private UUID uuid;
    private File path = null;
    private byte[] hash = null;

    private List<byte[]> hashes = new ArrayList<byte[]>();

    public Pack(String name, FileConfiguration cfg) {
        this.name = name;
        this.enableHash = cfg.getBoolean("packs." + name + ".enableHash", true);
        this.protect = cfg.getBoolean("packs." + name + ".protect", true);
        this.rpURL = RoseRP.getPluginConfig().HOST_URL + "/" + name + ".zip";

        this.prompt = cfg.getString("packs."+name+".prompt", "");
        this.required = cfg.getBoolean("packs."+name+".required", false);
        this.replaceDuplicate = cfg.getBoolean("packs."+name+".replaceDuplicate", false);
        this.jsonMerges = cfg.getStringList("packs."+name+".jsonMerges");

        this.connectedPacks = new ArrayList<>();
        if (cfg.contains("packs." + name + ".connectedPacks")) {
            for (String pack : cfg.getConfigurationSection("packs." + name + ".connectedPacks").getKeys(false)) {
                this.connectedPacks.add(new ConnectedPack(
                        "packs."+name+".connectedPacks."+pack,
                        pack,
                        cfg
                ));
            }
        }
    }

    public void addHash(byte[] hash) {
        this.hashes.add(hash);
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

    public boolean isReplaceDuplicate() {
        return replaceDuplicate;
    }

    public List<String> getJsonMerges() {
        return jsonMerges;
    }

    public String getZip() {
        return RoseRP.getInstance().getDataFolder().getPath() + File.separator + "resourcepacks" + File.separator + this.name + File.separator + this.name + ".zip";
    }

    public String getRpURL() {
        return this.rpURL;
    }

    public boolean isEnableHash() {
        return this.enableHash;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void init(File path) {
        this.path = path;
        loadFromCache();

        if (isEnableHash()) {
            this.hash = Hash.getSHA1Checksum(path.getPath());
            this.addHash(hash);
        }
    }

    private void loadFromCache() {
        File cacheFile = new File(RoseRP.getInstance().getDataFolder(), ".cache/" + name + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);

        if (cacheFile.exists()) {
            String uuidString = config.getString("uuid");
            if (uuidString != null) {
                uuid = UUID.fromString(uuidString);
            }

            hashes = new ArrayList<byte[]>();
            List<String> hashes = config.getStringList("hashes");
            for (String hash : hashes) {
                this.hashes.add(Hash.hexToBytes(hash));
            }
        }

        else {
            uuid = UUID.randomUUID();
            hashes = new ArrayList<byte[]>();
            saveToCache(config, cacheFile);
        }
    }

    private void saveToCache(YamlConfiguration config, File cacheFile) {
        config.set("uuid", uuid.toString());
        config.set("hashes", Hash.convertByteArraysToHexList(hashes));
        try {
            config.save(cacheFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<byte[]> getHashes() {
        return hashes;
    }

    @Override
    public String toString() {
        return "Pack{" +
                "name='" + name + '\'' +
                ", rpURL='" + rpURL + '\'' +
                ", enableHash=" + enableHash +
                ", connectedPacks=" + connectedPacks +
                ", prompt='" + prompt + '\'' +
                ", required=" + required +
                ", path=" + path +
                ", hash=" + Arrays.toString(hash) +
                '}';
    }
}