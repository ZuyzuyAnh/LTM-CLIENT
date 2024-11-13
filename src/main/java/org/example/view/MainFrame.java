package org.example.view;

import org.example.client.Client;
import org.example.client.MessageListener;
import org.example.dto.*;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;


public class MainFrame extends JFrame implements MessageListener {
    private static CardLayout cardLayout;
    private static JPanel panel;

    private static Login login;
    private static SignUp signUp;
    private static Home home;
    private static Admin admin;

    private User currentUser;

    public MainFrame() {
        super("Card Layout");
        setBounds(0, 0, 500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        login = new Login();
        signUp = new SignUp();

        login.setFrame(this);
        signUp.setFrame(this);

        panel = new JPanel();
        cardLayout = new CardLayout();
        panel.setLayout(cardLayout);

        panel.add(login, "login");
        panel.add(signUp, "signUp");

        cardLayout.show(panel, "login");

        add(panel);

        connect();

        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (currentUser != null) {
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

                    Client.getInstance().closeConnection();
                }
            }
        });
    }

    public void connect() {
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
                handleStartGame(message);
                break;
            case "invite declined":
                JOptionPane.showMessageDialog(this, "Người chơi từ chối ", "Error", JOptionPane.ERROR_MESSAGE);
                break;
            case "questions":
                java.util.List<Question> questionList = Parser.fromJsonArray(message.getData(), Question.class);
                admin.loadQuestions(questionList);
                break;
            case "answer":
                JOptionPane.showMessageDialog(this, message.getErrorMsg(), "Time's up", JOptionPane.ERROR_MESSAGE);
                break;
            case "end game":
                JOptionPane.showMessageDialog(this, message.getData());
                message = new Message(
                        currentUser.getId(),
                        "leaderboard",
                        null,
                        null
                );

                Client.getInstance().sendSocketMessage(message);
                showScreen("home");
                break;
            case "playing":
                home.handlePlaying(message);
                break;
            case "history":
                showUserInfo(message);
        }
    }

    private void handleStartGame(Message message) throws IOException {
        MatchStart matchStart = Parser.fromJson(message.getData(), MatchStart.class);
        java.util.List<QuestionAnswer> questionList = matchStart.getQuestionAnswers();
        int playerScore = 0;
        showQuestion(questionList.get(0), 0, matchStart.getMatch(), questionList, playerScore);
    }

    private void showQuestion(QuestionAnswer questionAnswer, int round, Match match, java.util.List<QuestionAnswer> questionList, int playerScore) throws IOException {
        String opponentName;
        if (match.getUser1().getUsername().equals(currentUser.getUsername())) {
            opponentName = match.getUser2().getUsername();
        }else {
            opponentName = match.getUser1().getUsername();
        }

        Game gameScreen = new Game(questionAnswer, opponentName, round, match, currentUser, playerScore);
        panel.add(gameScreen, "game");
        this.showScreen("game");

        gameScreen.setOnQuestionCompletedListener(() -> {
            if (round + 1 < 2) {
                showQuestion(questionList.get(round + 1), round + 1, match, questionList, gameScreen.getPlayerScore());
            } else {
                EndRequest request = new EndRequest(
                        gameScreen.getPlayerScore(),
                        match.getId()
                );

                Message message = new Message(
                        currentUser.getId(),
                        "end game",
                        Parser.toJson(request),
                        null
                );

                Client.getInstance().sendSocketMessage(message);
            }
        });
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


    private void showInviteAcceptDialog() {
        JOptionPane optionPane = new JOptionPane("Người chơi đã chấp nhận\nĐếm ngược: 3",
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        JDialog dialog = optionPane.createDialog(this, "Thông báo");

        final int[] countdown = {3};

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdown[0]--;

                if (countdown[0] > 0) {
                    optionPane.setMessage("Người chơi đã chấp nhận\nĐếm ngược: " + countdown[0]);
                    dialog.repaint();
                } else {
                    ((Timer)e.getSource()).stop();
                    dialog.dispose();
                }
            }
        });

        timer.start();
        dialog.setVisible(true);
    }

    public void showUserInfo(Message message) {
        java.util.List<HistoryResponse> historyResponses = Parser.fromJsonArray(message.getData(), HistoryResponse.class);

        UserInfo userInfo = new UserInfo(currentUser, historyResponses, this);
        panel.add(userInfo, "user");
        this.showScreen("user");
    }

    public void goBackToMainScreen(User user) {
        this.currentUser = user;

        home = new Home(currentUser);
        home.setFrame(this);

        showScreen("home");
    }
}
