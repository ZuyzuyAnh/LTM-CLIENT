package org.example.view;

import org.example.client.Client;
import org.example.client.HTTPClient;
import org.example.dto.*;

import javax.swing.*;
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

    private int countdown = 10;
    private int playerScore = 0;
    private int opponentScore = 0;
    private int round = 0;
    private String answer = "";
    private Match match;
    private QuestionAnswer qa;
    private Timer timer;
    private User currentUser;

    private OnQuestionCompletedListener listener;

    public Game(QuestionAnswer questionAnswer, String opponent, int round, Match match, User currentUser, int playerScore) {
        setLayout(new BorderLayout());
        this.round = round;
        this.match = match;
        this.qa = questionAnswer;
        this.currentUser = currentUser;
        this.playerScore = playerScore;
        // Khởi tạo các label điểm và label tên người chơi
        playWithLabel = new JLabel("Playing with: " + opponent);
        playerScoreLabel = new JLabel("Your Score: " + playerScore);

        JPanel topPanel = new JPanel(new BorderLayout());
        leaveButton = new JButton("Leave game");

        JPanel scorePanel = new JPanel(new GridLayout(1, 2));
        scorePanel.add(playerScoreLabel);

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
            // Mở một thread mới để xử lý việc gửi yêu cầu âm thanh
            new Thread(() -> {
                try {
                    HTTPClient.sendAudioRequest(questionAnswer.getQuestion().getSoundUrl());
                } catch (Exception ex) {
                    ex.printStackTrace();  // In lỗi nếu có
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

        // Gán giá trị cho từng nút trả lời và chuyển tham chiếu `answer` vào listener
        for (int i = 0; i < 7; i++) {
            answerButtons[i] = new JButton(answerTexts[i]);
            answerButtons[i].setPreferredSize(new Dimension(100, 30));
            answerPanel.add(answerButtons[i]);
            answerButtons[i].addActionListener(new AnswerButtonListener(answerTexts[i])); // Truyền tham chiếu
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
                    countdown--;  // Giảm countdown mỗi giây
                    countdownLabel.setText("Time Left: " + countdown + "s");
                } else if (countdown == 0) {
                    timer.stop();  // Dừng timer khi countdown hết

                    if (answer.equals(qa.getCorrectAnswer())) {
                        JOptionPane.showMessageDialog(Game.this, "Time's up! Correct answer");  // Hiển thị thông báo
                        updateScore();
                    }else {
                        JOptionPane.showMessageDialog(Game.this, "Time's up! Incorrect, the answer is " + qa.getCorrectAnswer());  // Hiển thị thông báo
                    }

                    if (listener != null) {
                        try {
                            listener.onQuestionCompleted();  // Gọi listener khi câu hỏi hoàn thành
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        });
        timer.start();  // Bắt đầu timer
    }

    private void updateScore() {
        playerScore += 1;
        playerScoreLabel.setText("Your Score: " + playerScore);
    }

    private void leaveGame() {
        // Xử lý thoát khỏi trò chơi
        timer.stop();
        JOptionPane.showMessageDialog(this, "Bạn đã rời khỏi trò chơi!");
    }

    private class AnswerButtonListener implements ActionListener {
        private String answer;

        public AnswerButtonListener(String answer) {
            this.answer = answer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Game.this.answer = this.answer;
        }
    }

    // Setter cho listener
    public void setOnQuestionCompletedListener(OnQuestionCompletedListener listener) {
        this.listener = listener;
    }

    // Interface để gọi khi câu hỏi hoàn thành
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


