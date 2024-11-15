package me.emsockz.roserp.pack;

import me.emsockz.roserp.RoseRP;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public final class ConnectedPack {
    private final String name;
    private final String path;
    private final boolean absolutePath;
    private final String url;
    private final List<String> skipFiles;

    private final int timeoutConnect;
    private final int timeoutRead;

    public ConnectedPack(String path, String name, FileConfiguration cfg) {
        this.name = name;
        this.path =  cfg.getString(path+".path", "");
        this.absolutePath = cfg.getBoolean(path+".absolutePath", false);
        this.url = cfg.getString(path+".url", "");
        this.skipFiles = cfg.getStringList(path+".skipFiles");

        this.timeoutConnect = cfg.getInt(path+".timeout.connect", 10)*1000;
        this.timeoutRead = cfg.getInt(path+".timeout.read", 10)*1000;
    }

    public File getFile() {
        File file = null;
        if (url().isBlank()) {
            if (path().isBlank()) return null;
            file = getFileDisk();
        } else {
            file = download();
        }

        return file;
    }

    public File getFileDisk() {
        File connectedFile = null;
        if (absolutePath()) {
            String path = path();
            if (!path.startsWith("/"))
                path = "/" + path;

            connectedFile = new File(path);
        } else {
            connectedFile = new File("plugins/" + path());
        }

        return connectedFile;
    }

    public File download() {
        URL url = null;
        try {
            url = new URL(url()); // Метод url() должен возвращать строку с URL
        } catch (MalformedURLException e) {
            RoseRP.logSevere("Invalid URL: " + url());
            throw new RuntimeException("Invalid URL: " + url(), e);
        }

        RoseRP.logInfo("Download file " + url() + "...");

        // Получаем поток данных с URL
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutConnect); // Тайм-аут на подключение (10 секунд)
            connection.setReadTimeout(timeoutRead); // Тайм-аут на чтение (20 секунд)

            // Проверяем код ответа сервера (например, 200 OK)
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned non-OK status: " + responseCode);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                // Считываем данные в ByteArrayOutputStream
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096]; // Буфер для чтения данных
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                // Создаем временный файл и записываем в него данные
                File tempFile = File.createTempFile("downloaded_", ".zip");
                try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                    byteArrayOutputStream.writeTo(fileOutputStream);
                }

                return tempFile; // Возвращаем объект File, который теперь содержит данные
            }

        } catch (IOException e) {
            RoseRP.logSevere("Error downloading file (" + url() + "): " + e.getMessage());
            return null;
        }
    }

    public String name() {
        return name;
    }

    public String path() {
        return path;
    }

    public boolean absolutePath() {
        return absolutePath;
    }

    public String url() {
        return url;
    }

    public List<String> skipFiles() {
        return skipFiles;
    }

    public int timeoutConnect() {
        return timeoutConnect;
    }

    public int timeoutRead() {
        return timeoutRead;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConnectedPack) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.path, that.path) &&
                this.absolutePath == that.absolutePath &&
                Objects.equals(this.url, that.url) &&
                Objects.equals(this.skipFiles, that.skipFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, absolutePath, url, skipFiles);
    }

    @Override
    public String toString() {
        return "ConnectedPack{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", absolutePath=" + absolutePath +
                ", url='" + url + '\'' +
                ", skipFiles=" + skipFiles +
                ", timeoutConnect=" + timeoutConnect +
                ", timeoutRead=" + timeoutRead +
                '}';
    }
}