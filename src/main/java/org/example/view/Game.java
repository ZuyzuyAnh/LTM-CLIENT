package org.example.view;

import org.example.client.Client;
import org.example.client.HTTPClient;
import org.example.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Game extends JPanel {
    private JButton[] answerButtons;
    private JButton listenButton;
    private JButton leaveButton;
    private JLabel questionLabel;
    private JLabel playWithLabel;
    private JTextField questionField;
    private JLabel countdownLabel;
    private JLabel playerScoreLabel;
    private JLabel roundLabel; // Hiển thị vòng chơi

    private int countdown = 10;
    private int playerScore = 0;
    private String answer = "";
    private QuestionAnswer qa;
    private Timer timer;
    private JButton selectedButton = null; // Nút được chọn gần đây

    private boolean isLeaveGame;
    private OnQuestionCompletedListener listener;

    public Game(QuestionAnswer questionAnswer, String opponent, int round, Match match, User currentUser, int playerScore) {
        setLayout(new BorderLayout());
        this.qa = questionAnswer;
        this.playerScore = playerScore;

        playWithLabel = new JLabel("Playing with: " + opponent);
        playWithLabel.setBorder(new EmptyBorder(0, 0, 0, 20));

        playerScoreLabel = new JLabel("Your Score: " + playerScore);
        roundLabel = new JLabel("Round: " + round);

        JPanel topPanel = new JPanel(new BorderLayout());
        leaveButton = new JButton("Leave game");

        leaveButton.addActionListener(e -> {
            isLeaveGame = true;
        });

        JPanel scorePanel = new JPanel(new GridLayout(1, 3));
        scorePanel.add(playerScoreLabel);
        scorePanel.add(roundLabel);

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
        listenButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    HTTPClient.sendAudioRequest(questionAnswer.getQuestion().getSoundUrl());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        countdownLabel = new JLabel("Time Left: " + countdown + "s");
        countdownLabel.setFont(new Font("Arial", Font.BOLD, 14));
        countdownLabel.setForeground(Color.RED);

        questionPanel.add(questionLabel);
        questionPanel.add(questionField);
        questionPanel.add(listenButton);
        questionPanel.add(countdownLabel);

        JPanel answerPanel = new JPanel(new GridLayout(5, 2, 20, 20));
        answerButtons = new JButton[10];

        String[] answerTexts = new String[questionAnswer.getAnswers().size()];
        questionAnswer.getAnswers().toArray(answerTexts);

        for (int i = 0; i < 7; i++) {
            answerButtons[i] = new JButton(answerTexts[i]);
            answerButtons[i].setPreferredSize(new Dimension(100, 30));
            answerPanel.add(answerButtons[i]);
            answerButtons[i].addActionListener(new AnswerButtonListener(answerButtons[i], answerTexts[i]));
        }

        add(topPanel, BorderLayout.NORTH);
        add(questionPanel, BorderLayout.CENTER);
        add(answerPanel, BorderLayout.SOUTH);

        leaveButton.addActionListener(e -> leaveGame());

        startCountdown();
    }

    private void startCountdown() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (countdown > 0) {
                    countdown--;
                    countdownLabel.setText("Time Left: " + countdown + "s");
                } else if (countdown == 0) {
                    timer.stop();

                    if (answer.equals(qa.getCorrectAnswer())) {
                        JOptionPane.showMessageDialog(Game.this, "Time's up! Correct answer");
                        updateScore();
                    } else {
                        JOptionPane.showMessageDialog(Game.this, "Time's up! Incorrect, the answer is " + qa.getCorrectAnswer());
                    }

                    if (listener != null) {
                        try {
                            listener.onQuestionCompleted();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        });
        timer.start();
    }

    private void updateScore() {
        playerScore += 1;
        playerScoreLabel.setText("Your Score: " + playerScore);
    }

    private void leaveGame() {
        timer.stop();
        JOptionPane.showMessageDialog(this, "Bạn đã rời khỏi trò chơi!");
    }

    private class AnswerButtonListener implements ActionListener {
        private JButton button;
        private String answer;

        public AnswerButtonListener(JButton button, String answer) {
            this.button = button;
            this.answer = answer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Game.this.answer = this.answer;

            // Làm nổi bật nút hiện tại và reset màu các nút khác
            if (selectedButton != null) {
                selectedButton.setBackground(null); // Trả lại màu mặc định cho nút trước đó
            }
            button.setBackground(Color.ORANGE); // Đặt màu đậm cho nút đã chọn
            selectedButton = button; // Cập nhật nút đã chọn gần đây
        }
    }

    public void setOnQuestionCompletedListener(OnQuestionCompletedListener listener) {
        this.listener = listener;
    }

    public interface OnQuestionCompletedListener {
        void onQuestionCompleted() throws IOException;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public void setPlayerScore(int playerScore) {
        this.playerScore = playerScore;
    }
}
