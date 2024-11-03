package me.emsockz.roserp.packer;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.commands.sub.TextureCMD;
import me.emsockz.roserp.pack.ConnectedPack;
import me.emsockz.roserp.pack.Pack;

import java.io.*;
import java.math.BigInteger;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;

public class Packer {

    public static void packFiles(Pack obj) {
        TextureCMD.enable = false;

        try {
            File resourcePack = new File(RoseRP.getInstance().getDataFolder(), "resourcepacks/" + obj.getName());
            File pack = new File(resourcePack, obj.getZipArchiveName() + ".zip");

            // Проверка на существование файла и удаление перед началом записи
            if (pack.exists()) {
                pack.delete();
            }

            try (FileOutputStream fos = new FileOutputStream(pack);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // Обработка подключенных пакетов
                for (ConnectedPack connectedPack : obj.getConnectedPacks()) {
                    File connectedFile = new File("plugins/" + connectedPack.path());

                    if (connectedFile.isDirectory()) {
                        addFolderToZip(obj, connectedFile, "", zos, connectedPack.skipFiles()); // Пустая строка для корневого уровня
                    } else if (connectedFile.isFile() && connectedFile.getName().endsWith(".zip")) {
                        addZipToZip(obj, connectedFile, zos, connectedPack.skipFiles());
                    }
                }

                // Обработка главной папки пакета
                addFolderToZip(obj, resourcePack, "", zos, List.of()); // Пустая строка для корневого уровня
            }

            TextureCMD.enable = true;
            obj.init(pack);
        } catch (IOException exception) {
            exception.printStackTrace();
            TextureCMD.enable = true;
        }
    }

    private static void addFolderToZip(Pack obj, File folder, String baseName, ZipOutputStream zos, List<String> skipFiles) {
        File[] files = folder.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (shouldSkip(file.getPath(), skipFiles)) continue;

            try {
                String entryName = baseName.isEmpty() ? file.getName() : baseName + "/" + file.getName();

                if (file.isDirectory()) {
                    addFolderToZip(obj, file, entryName, zos, skipFiles); // Используем entryName вместо baseName
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zos.putNextEntry(zipEntry);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();

                        // Установка защиты, если включена
                        if (obj.isProtect()) {
                            zipEntry.setCrc(buffer.length); // Пример установки CRC
                            zipEntry.setSize(new BigInteger(buffer).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void addZipToZip(Pack obj, File zipFile, ZipOutputStream zos, List<String> skipFiles) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            zip.stream().forEach(entry -> {
                if (shouldSkip(entry.getName(), skipFiles)) return;

                try (InputStream is = zip.getInputStream(entry)) {
                    ZipEntry zipEntry = new ZipEntry(entry.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();

                    // Установка защиты, если включена
                    if (obj.isProtect()) {
                        zipEntry.setCrc(buffer.length); // Пример установки CRC
                        zipEntry.setSize(new BigInteger(buffer).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static boolean shouldSkip(String path, List<String> skipFiles) {
        for (String skipPath : skipFiles) {
            if (path.contains(skipPath)) {
                return true;
            }
        }

        // Проверка на игнорируемые расширения
        String extension = com.google.common.io.Files.getFileExtension(path);
        return RoseRP.getPluginConfig().IGNORE_FILES.contains(extension);
    }
}
