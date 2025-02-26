package me.emsockz.roserp.util;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

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

    // Из HEX-строки -> byte[]
    public static byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            result[i / 2] = (byte) ((hi << 4) + lo);
        }

        return result;
    }

    // Пример метода для конвертации List<byte[]> -> List<String> (HEX)
    public static List<String> convertByteArraysToHexList(List<byte[]> listOfByteArrays) {
        List<String> result = new ArrayList<>(listOfByteArrays.size());
        for (byte[] byteArray : listOfByteArrays) {
            result.add(bytesToHex(byteArray));
        }
        return result;
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
