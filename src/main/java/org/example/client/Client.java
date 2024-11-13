package org.example.client;

import org.example.dto.Message;
import org.example.dto.Parser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static Client instance;
    private Socket socket;
    private PrintWriter out;
    private MessageListener messageListener;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private Client() {
    }

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void startConnection(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to server at " + ip + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessage() {
        while (!socket.isClosed()) { // Kiểm tra nếu socket vẫn mở
            try {
                String jsonMessage = receiveSocketMessage();
                if (jsonMessage == null) {
                    break; // Thoát khỏi vòng lặp nếu nhận được null (kết nối đã đóng)
                }
                if (messageListener != null) {
                    Message message = Parser.fromJson(jsonMessage, Message.class);
                    messageListener.onMessageReceived(message);
                }
            } catch (Exception e) {
            }
        }
    }

    public String receiveSocketMessage() {
        try {
            return inputStream.readUTF();
        } catch (IOException e) {
            return null; // Trả về null nếu kết nối bị đóng
        }
    }


    public void sendSocketMessage(Message message) throws IOException {
        outputStream.writeUTF(Parser.toJson(message));
        outputStream.flush();
    }

    public void closeConnection() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Connection closed successfully.");
        } catch (IOException e) {
            System.err.println("Error while closing the connection: " + e.getMessage());
        }
    }

}
