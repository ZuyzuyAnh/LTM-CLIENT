package org.example.view;

import org.example.dto.Match;
import org.example.dto.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Game extends JPanel {

    private JButton[] answerButtons;
    private JButton listenButton;
    private JButton leaveButton;
    private JLabel questionLabel;
    private JLabel playWithLabel;
    private JTextField questionField;
    private JLabel countdownLabel;
    private JLabel playerScoreLabel;
    private JLabel opponentScoreLabel;

    private int countdown = 10;
    private int playerScore = 0;
    private int opponentScore = 0;

    private Timer timer;

    public Game(Match match, User currentUser) {

        User user1 = match.getUser1();
        User user2 = match.getUser2();

        if (match.getUser2().getUsername().equals(currentUser.getUsername())) {
            user1 = match.getUser2();
            user2 = match.getUser1();
        }

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        leaveButton = new JButton("Leave game");

        JPanel scorePanel = new JPanel(new GridLayout(1, 2));

        scorePanel.add(playerScoreLabel);
        scorePanel.add(opponentScoreLabel);

        topPanel.add(playWithLabel, BorderLayout.WEST);
        topPanel.add(leaveButton, BorderLayout.EAST);
        topPanel.add(scorePanel, BorderLayout.CENTER);

        JPanel questionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        questionLabel = new JLabel("Question");

        questionField = new JTextField(20);
        questionField.setPreferredSize(new Dimension(200, 25));
        questionField.setEditable(false);

        listenButton = new JButton("Click here to listen");
        listenButton.setPreferredSize(new Dimension(150, 25));

        countdownLabel = new JLabel("Time Left: " + countdown + "s");
        countdownLabel.setFont(new Font("Arial", Font.BOLD, 14));
        countdownLabel.setForeground(Color.RED);

        questionPanel.add(questionLabel);
        questionPanel.add(questionField);
        questionPanel.add(listenButton);
        questionPanel.add(countdownLabel);

        JPanel answerPanel = new JPanel(new GridLayout(5, 2, 20, 20));
        answerButtons = new JButton[10];
        String[] answerTexts = {
                "Con mèo", "Con chó", "Con bò", "Con lợn", "Con gà",
                "Con sư tử", "Con báo", "Con khủng long", "Con dê", "Con vịt"
        };

        for (int i = 0; i < 10; i++) {
            answerButtons[i] = new JButton(answerTexts[i]);
            answerButtons[i].setPreferredSize(new Dimension(100, 30)); // Đặt kích thước nhỏ cho các nút đáp án
            answerPanel.add(answerButtons[i]);
            answerButtons[i].addActionListener(new AnswerButtonListener(answerTexts[i]));
        }

        // Thêm các panel vào giao diện
        add(topPanel, BorderLayout.NORTH);
        add(questionPanel, BorderLayout.CENTER);
        add(answerPanel, BorderLayout.SOUTH);

        // Sự kiện cho nút "Leave game"
        leaveButton.addActionListener(e -> leaveGame());

        // Khởi tạo timer đếm ngược
        startCountdown();
    }

    private void startCountdown() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdown--;
                countdownLabel.setText("Time Left: " + countdown + "s");
                if (countdown <= 0) {
                    timer.stop();
                    JOptionPane.showMessageDialog(Game.this, "Time's up!");
                    // Xử lý khi hết thời gian, có thể chuyển sang câu hỏi tiếp theo
                }
            }
        });
        timer.start();
    }

    private void leaveGame() {
        // Xử lý thoát khỏi trò chơi
        timer.stop();
        JOptionPane.showMessageDialog(this, "Bạn đã rời khỏi trò chơi!");
    }

    // Xử lý sự kiện khi người chơi chọn đáp án
    private class AnswerButtonListener implements ActionListener {
        private String answer;

        public AnswerButtonListener(String answer) {
            this.answer = answer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            playerScore += 10;
            playerScoreLabel.setText("Your Score: " + playerScore);

            JOptionPane.showMessageDialog(Game.this, "Bạn đã chọn: " + answer);
            countdown = 10;
            countdownLabel.setText("Time Left: " + countdown + "s");
        }
    }
}

