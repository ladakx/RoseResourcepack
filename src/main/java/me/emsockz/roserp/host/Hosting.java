package me.emsockz.roserp.host;

import me.emsockz.roserp.RoseRP; import me.emsockz.roserp.pack.Pack; import org.bukkit.Bukkit; import org.bukkit.scheduler.BukkitTask;

import java.io.*; import java.net.ServerSocket; import java.net.Socket; import java.net.SocketException; import java.nio.charset.StandardCharsets;

public class Hosting {

    private ServerSocket serverSocket;

    public boolean isRunning = true;

    public BukkitTask task;

    public Hosting() {
        try {
            serverSocket = new ServerSocket(RoseRP.getPluginConfig().PORT);
            serverSocket.setReceiveBufferSize(65536);  // 64KB buffer

        } catch (Exception e) {
            Bukkit.getLogger().info("Something went wrong hosting the server! " + e);
            return;
        }

        handleRequest();
    }

    public void handleRequest() {
        if (!RoseRP.getInstance().isEnabled() || serverSocket.isClosed()) {
            return;
        }

        this.task = Bukkit.getScheduler().runTaskAsynchronously(RoseRP.getInstance(), () -> {
            // Пока плагин жив, бежим в цикле
            while (isRunning && RoseRP.getInstance().isEnabled() && !serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    processSocket(socket);
                } catch (IOException e) {
                    // Вместо игнорирования — логируем
                    e.printStackTrace();
                }
            }
        });
    }

    private void processSocket(Socket socket) throws IOException {
        // Здесь сам код чтения запроса и отдачи файла
        String response = "HTTP/1.1 200 OK\r\nContent-Type: application/zip\r\n\r\n";
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String requestLine = reader.readLine(); // GET /filename.zip HTTP/1.1

        if (requestLine != null) {
            String[] split = requestLine.split(" ");
            if (split.length >= 2) {
                String url = split[1];
                String[] urlParts = url.split("/");

                if (urlParts.length >= 2) {
                    String name = urlParts[1].replace(".zip", "");
                    Pack pack = RoseRP.getPack(name);

                    if (pack != null) {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(response.getBytes(StandardCharsets.UTF_8));

                        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pack.getPath()));
                             BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {

                            byte[] buffer = new byte[65536];
                            int bytesRead;
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                bos.write(buffer, 0, bytesRead);
                            }
                        }
                    } else {
                        String notFoundResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                        socket.getOutputStream().write(notFoundResponse.getBytes());
                    }
                }
            }
        }
    }

    public BukkitTask getTask() {
        return task;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}