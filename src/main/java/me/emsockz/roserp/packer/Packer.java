package me.emsockz.roserp.packer;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.commands.sub.TextureCMD;
import me.emsockz.roserp.pack.ConnectedPack;
import me.emsockz.roserp.pack.Pack;

import java.io.*;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;

public class Packer {

    public static void packFiles(Pack obj) {
        TextureCMD.enable = false;

        try {
            File resourcePack = new File(RoseRP.getInstance().getDataFolder(), "resourcepacks/" + obj.getName());
            File pack = new File(resourcePack, obj.getName() + ".zip");

            // Создаём директории, если их нет
            if (!pack.getParentFile().exists()) {
                pack.getParentFile().mkdirs();
            }

            // Проверка на существование файла и удаление перед началом записи
            if (pack.exists()) {
                pack.delete();
            }

            try (FileOutputStream fos = new FileOutputStream(pack);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                Set<String> addedEntries = new HashSet<>();

                // Обработка подключенных пакетов
                for (ConnectedPack connectedPack : obj.getConnectedPacks()) {
                    File connectedFile = connectedPack.getFile();
                    if (connectedFile == null) {
                        RoseRP.logWarning("Connected Pack not found: "+ connectedPack.name());
                        continue;
                    }

                    if (connectedFile.isDirectory()) {
                        addFolderToZip(obj, connectedFile, "", zos, connectedPack.skipFiles(), addedEntries); // Пустая строка для корневого уровня
                    } else if (connectedFile.isFile() && connectedFile.getName().endsWith(".zip")) {
                        addZipToZip(obj, connectedFile, zos, connectedPack.skipFiles(), addedEntries);
                    }
                }

                // Обработка главной папки пакета
                addFolderToZip(obj, resourcePack, "", zos, List.of(), addedEntries); // Пустая строка для корневого уровня
            }

            TextureCMD.enable = true;
            obj.init(pack);
        } catch (IOException e) {
            if (e instanceof java.util.zip.ZipException) {
                // Ошибка дубликата — игнорируем
                return;
            }
            e.printStackTrace();
            TextureCMD.enable = true;
        }
    }

    private static void addFolderToZip(Pack pack, File folder, String baseName, ZipOutputStream zos, List<String> skipFiles, Set<String> addedEntries) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (shouldSkip(file.getPath(), skipFiles)) continue;

            try {
                String entryName = baseName.isEmpty() ? file.getName() : baseName + "/" + file.getName();

                if (file.isDirectory()) {
                    addFolderToZip(pack, file, entryName, zos, skipFiles, addedEntries);
                } else {
                    if (isDuplicateEntry(entryName, addedEntries)) {
                        if (!pack.isReplaceDuplicate()) {
                            RoseRP.logWarning("Duplicate entry found and skipped: " + entryName);
                            continue;
                        }
                    }

                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zos.putNextEntry(zipEntry);
                        addedEntries.add(entryName);  // Добавляем запись в набор

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();

                        // Установка защиты, если включена
                        if (pack.isProtect()) {
                            zipEntry.setCrc(buffer.length);
                            zipEntry.setSize(new BigInteger(buffer).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
                        }
                    }
                }
            } catch (IOException e) {
                if (!(e instanceof ZipException))
                    e.printStackTrace();
            }
        }
    }

    private static void addZipToZip(Pack pack, File zipFile, ZipOutputStream zos, List<String> skipFiles, Set<String> addedEntries) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            zip.stream().forEach(entry -> {
                if (shouldSkip(entry.getName(), skipFiles)) return;

                String entryName = entry.getName();
                if (isDuplicateEntry(entryName, addedEntries)) {
                    if (!pack.isReplaceDuplicate()) {
                        RoseRP.logWarning("Duplicate entry found and skipped: " + entryName);
                        return;
                    }
                }

                try (InputStream is = zip.getInputStream(entry)) {
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);
                    addedEntries.add(entryName);  // Добавляем запись в набор

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();

                    if (pack.isProtect()) {
                        zipEntry.setCrc(buffer.length);
                        zipEntry.setSize(new BigInteger(buffer).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
                    }
                } catch (IOException e) {
                    if (!(e instanceof ZipException))
                        e.printStackTrace();
                }
            });
        }
    }

    private static boolean isDuplicateEntry(String entryName, Set<String> addedEntries) {
        return !addedEntries.add(entryName);  // Добавляет запись и возвращает false, если она уже существует
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
