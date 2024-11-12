import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class AudioClient extends Application {

    @Override
    public void start(Stage primaryStage) {
        String serverAddress = "127.0.0.1";
        int serverPort = 8082;

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            // Gửi đường dẫn tệp âm thanh tới server
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);
            writer.println("path_to_audio_file");  // Gửi đường dẫn tệp tới server

            // Nhận dữ liệu âm thanh từ server
            InputStream in = new BufferedInputStream(socket.getInputStream());

            // Lưu dữ liệu nhận được vào tệp tạm thời
            File tempFile = File.createTempFile("audio", ".mp3");
            try (FileOutputStream fileOut = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }

            // Tạo đối tượng Media từ tệp tạm thời
            Media media = new Media(tempFile.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            // Phát âm thanh
            mediaPlayer.play();

            // Thiết lập cửa sổ JavaFX
            primaryStage.setTitle("JavaFX Audio Player");
            primaryStage.setScene(new Scene(mediaPlayer.getMedia()));
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
