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
        while (true) {
            try {
                String jsonMessage = receiveSocketMessage();
                if (jsonMessage == null) {
                    break; // Exit the loop if we received null, indicating closure
                }
                if (messageListener != null) {
                    Message message = Parser.fromJson(jsonMessage, Message.class);
                    messageListener.onMessageReceived(message);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
            // Break the loop on EOFException

        }
    }


//    public void stopConnection(String username) {
//        try {
//            Message disconnectMessage = new Message("disconnect", "", username);
//            String json = Parser.toJson(disconnectMessage);
//            sendSocketMessage(json);
//
//            if (inputStream != null) inputStream.close();
//            if (out != null) out.close();
//            if (socket != null) socket.close();
//
//            System.out.println("Disconnected from server.");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public String receiveSocketMessage() {
        try {
            return inputStream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendSocketMessage(Message message) throws IOException {
        outputStream.writeUTF(Parser.toJson(message));
        outputStream.flush();
    }
}
