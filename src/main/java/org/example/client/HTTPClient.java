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
            writer.writeBytes("Content-Type: audio/wav\r\n");
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
        }

        return response.toString();
    }


    public static void sendAudioRequest(String filePath) throws Exception {
        URL url = new URL("http://localhost:8081/audio");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

        // Gửi đường dẫn file đến server
        try (OutputStream os = connection.getOutputStream()) {
            os.write(filePath.getBytes("UTF-8"));
            os.flush();
        }

        // Kiểm tra phản hồi từ server
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Lỗi từ server: " + responseCode);
        }

        // Đọc dữ liệu âm thanh từ phản hồi và phát ngay lập tức
        try (InputStream inputStream = connection.getInputStream()) {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            try (SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info)) {
                audioLine.open(format);
                audioLine.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    audioLine.write(buffer, 0, bytesRead);
                }

                audioLine.drain();
            }
        }
    }
}

