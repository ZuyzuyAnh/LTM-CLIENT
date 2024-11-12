package org.example.view;

import org.example.client.Client;
import org.example.client.MessageListener;
import org.example.dto.Message;
import org.example.dto.Parser;
import org.example.dto.Question;
import org.example.dto.User;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class MainFrame extends JFrame implements MessageListener {
    private static CardLayout cardLayout;
    private static JPanel panel;

    private static Login login;
    private static SignUp signUp;
    private static Home home;
    private static Admin admin;
    private static Game game;

    private User currentUser;

    public MainFrame() {
        super("Card Layout");
        setBounds(0, 0, 500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        login = new Login();
        signUp = new SignUp();

        login.setFrame(this);
        signUp.setFrame(this);

        connect();

        panel = new JPanel();
        cardLayout = new CardLayout();
        panel.setLayout(cardLayout);

        panel.add(login, "login");
        panel.add(signUp, "signUp");

        cardLayout.show(panel, "login");

        add(panel);

        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Message message = new Message(
                        currentUser.getId(),
                        "exit",
                        null,
                        null
                );

                try {
                    Client.getInstance().sendSocketMessage(message);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void connect() {
        Client client = Client.getInstance();
        client.setMessageListener(this);
        client.startConnection("localhost", 8080);

        new Thread(() -> {
            while (true) {
                client.receiveMessage();
            }
        }).start();
    }

    public void showScreen(String screen) {
        cardLayout.show(panel, screen);
    }


    @Override
    public void onMessageReceived(Message message) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        String action = message.getAction();

        switch (action) {
            case "login":
                User user = Parser.fromJson(message.getData(), User.class);
                if (user != null) {
                    if (user.getRole().equals("player")) {
                        login.submit(user);

                        home = new Home(user);
                        home.setFrame(this);
                        home.setCurrentUser(user);
                        panel.add(home, "home");
                        this.currentUser = user;

                        showScreen("home");
                    }else {
                        login.submit(user);
                        admin = new Admin(user, this);
                        panel.add(admin, "admin");
                        currentUser = user;

                        showScreen("admin");
                    }
                }else {
                    JOptionPane.showMessageDialog(panel, message.getErrorMsg());
                }

                break;
            case "signup":
                signUp.handle(message);
                break;
            case "online":
                home.handleOnline(message);
                break;
            case "leaderboard":
                home.handleLeaderBoard(message);
                break;
            case "error":
                JOptionPane.showMessageDialog(this, message.getErrorMsg(), "Error", JOptionPane.ERROR_MESSAGE);
                break;
            case "invite":
                showInvitationDialog(message);
                break;
            case "invite accepted":
                showInviteAcceptDialog();
                break;
            case "invite declined":
                JOptionPane.showMessageDialog(this, "Người chơi từ chối ", "Error", JOptionPane.ERROR_MESSAGE);
                break;
            case "questions":
                java.util.List<Question> questionList = Parser.fromJsonArray(message.getData(), Question.class);
                admin.loadQuestions(questionList);
                break;
            case "play audio":
                handleReceivedAudio(message);
                break;
        }
    }

    private void showInvitationDialog(Message message) throws IOException {
        String msg = "Bạn có muốn tham gia trận đấu không?";
        String title = "Lời mời từ người chơi " + message.getData();
        String senderStr = Parser.toJson(message.getSender());
        int option = JOptionPane.showOptionDialog(this,
                msg, title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, new Object[] {"Chấp nhận", "Từ chối"}, "Từ chối");

        if (option == JOptionPane.YES_OPTION) {
            Message acceptMsg = new Message(
                    currentUser.getId(),
                    "invite accepted",
                    senderStr,
                    null
            );

            Client.getInstance().sendSocketMessage(acceptMsg);
        } else if (option == JOptionPane.NO_OPTION) {
            Message declineMsg = new Message(
                    currentUser.getId(),
                    "invite declined",
                    senderStr,
                    null
            );

            Client.getInstance().sendSocketMessage(declineMsg);
        }
    }

    private void handleReceivedAudio(Message message) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        String base64Audio = message.getData();
        byte[] audioBytes = Base64.getDecoder().decode(base64Audio);

        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBytes))) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        }
    }

    private void showInviteAcceptDialog() {
        JOptionPane optionPane = new JOptionPane("Người chơi đã chấp nhận\nĐếm ngược: 3",
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        JDialog dialog = optionPane.createDialog(this, "Thông báo");

        final int[] countdown = {3};

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdown[0]--; // Giảm biến đếm ngược

                if (countdown[0] > 0) {
                    optionPane.setMessage("Người chơi đã chấp nhận\nĐếm ngược: " + countdown[0]);
                    dialog.repaint(); // Cập nhật lại dialog
                } else {
                    ((Timer)e.getSource()).stop(); // Dừng timer khi kết thúc đếm ngược
                    dialog.dispose(); // Đóng dialog
                }
            }
        });

        timer.start();
        dialog.setVisible(true);
    }
}
