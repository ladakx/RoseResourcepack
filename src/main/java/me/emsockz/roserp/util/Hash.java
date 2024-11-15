package me.emsockz.roserp.util;

import java.io.FileInputStream;
import java.security.MessageDigest;

public class Hash {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0'); // добавляем ведущий ноль при необходимости
            hexString.append(hex);
        }
        return hexString.toString();
    }

    //get sha1 checksum
    public static byte[] getSHA1Checksum(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] data = new byte[1024];
            int read = 0;
            while ((read = fis.read(data)) != -1) {
                sha1.update(data, 0, read);
            }
            return sha1.digest();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
