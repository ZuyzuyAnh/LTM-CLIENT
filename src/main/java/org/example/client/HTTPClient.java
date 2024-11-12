package org.example.client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public class HTTPClient {

    public static String uploadFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File không tồn tại: " + filePath);
        }

        URL url = new URL("http://localhost:8081/upload");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        String boundary = UUID.randomUUID().toString();
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = connection.getOutputStream(); DataOutputStream writer = new DataOutputStream(out)) {

            writer.writeBytes("--" + boundary + "\r\n");
            writer.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
            writer.writeBytes("Content-Type: " + Files.probeContentType(file.toPath()) + "\r\n");
            writer.writeBytes("\r\n");

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                }
            }

            writer.writeBytes("\r\n");

            writer.writeBytes("--" + boundary + "--\r\n");
            writer.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Lỗi khi tải file lên: " + responseCode);
        }

        StringBuilder response = new StringBuilder();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            System.out.println("Response body: " + response.toString());
        }

        return response.toString();
    }

    public static void sendAudioRequest(String filePath) throws Exception {
        try {
            // Tạo kết nối HTTP tới server
            URL url = new URL("http://localhost:8081/audio");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");

            // Gửi đường dẫn file trong body của request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = filePath.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            // Kiểm tra mã phản hồi của server
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Nhận và xử lý response từ server (file âm thanh)
                try (InputStream inputStream = connection.getInputStream()) {
                    // Đọc toàn bộ dữ liệu từ InputStream vào ByteArrayOutputStream
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096]; // Đọc theo khối
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }

                    // Tạo AudioInputStream từ ByteArrayInputStream
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(byteArrayInputStream);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInputStream);

                    // Phát âm thanh
                    clip.start();
                    System.out.println("Audio is playing...");

                    // Đợi cho đến khi âm thanh phát xong
                    while (clip.isRunning()) {
                        Thread.sleep(10);
                    }
                    System.out.println("Audio finished playing.");
                } catch (Exception e) {
                    System.out.println("Error while playing audio: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error: " + responseCode);
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

    }
}

