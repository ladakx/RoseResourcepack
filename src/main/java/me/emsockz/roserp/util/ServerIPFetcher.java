package me.emsockz.roserp.util;

import me.emsockz.roserp.RoseRP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class ServerIPFetcher {

    public static String getPublicIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader.readLine();
        } catch (Exception e) {
            RoseRP.logWarning("Failed to automatically fetch the public IP address of the server. Please type it manually in the  config.yml");
            return "127.0.0.1";
        }
    }
}
