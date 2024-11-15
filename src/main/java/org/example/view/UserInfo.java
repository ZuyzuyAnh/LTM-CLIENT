package org.example.view;

import org.example.client.Client;
import org.example.dto.HistoryResponse;
import org.example.dto.Message;
import org.example.dto.Parser;
import org.example.dto.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class UserInfo extends JPanel {
    private JTextField usernameField, emailField, roleField, scoreField;
    private JButton saveButton, backButton;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;
    private MainFrame mainFrame;
    private User user;

    public UserInfo(User user, List<HistoryResponse> historyList, MainFrame mainFrame) {
        setLayout(new BorderLayout());
        this.mainFrame = mainFrame;
        this.user = user;
        // Panel để hiển thị và chỉnh sửa thông tin người dùng
        JPanel userInfoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        userInfoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin người chơi"));

        // Các trường nhập thông tin người dùng
        userInfoPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(user.getUsername());
        userInfoPanel.add(usernameField);

        userInfoPanel.add(new JLabel("Email:"));
        emailField = new JTextField(user.getEmail());
        userInfoPanel.add(emailField);

        userInfoPanel.add(new JLabel("Score:"));
        scoreField = new JTextField(String.valueOf(user.getScore()));
        userInfoPanel.add(scoreField);
        scoreField.setEditable(false);

        // Nút "Lưu" để lưu các thay đổi
        saveButton = new JButton("Lưu");
        saveButton.addActionListener(e -> {
            try {
                saveUserInfo();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        userInfoPanel.add(saveButton);

        // Nút "Quay lại màn hình chính"
        backButton = new JButton("Quay lại màn hình chính");
        backButton.addActionListener(e -> {
            try {
                goBackToMainScreen();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        userInfoPanel.add(backButton);

        add(userInfoPanel, BorderLayout.NORTH);

        // Tạo bảng lịch sử đấu
        String[] columnNames = {"Opponent", "Match ID", "Time", "Scores"};
        historyTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa các ô trong bảng
            }
        };

        historyTable = new JTable(historyTableModel);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        add(scrollPane, BorderLayout.CENTER);

        loadHistoryData(historyList);
    }

    private void loadHistoryData(List<HistoryResponse> historyList) {
        for (HistoryResponse history : historyList) {
            historyTableModel.addRow(new Object[]{
                    history.getOpponent(),
                    history.getMatchId(),
                    history.getTime(),
                    history.getScores()
            });
        }
    }

    private void saveUserInfo() throws IOException {
        // Lấy dữ liệu từ các trường nhập và cập nhật thông tin người dùng
        this.user.setUsername(usernameField.getText());
        this.user.setEmail(emailField.getText());

        Message message = new Message(
                user.getId(),
                "update info",
                Parser.toJson(user),
                null
        );

        Client.getInstance().sendSocketMessage(message);
        // Thực hiện logic lưu (ví dụ: gọi phương thức lưu vào cơ sở dữ liệu)
        JOptionPane.showMessageDialog(this, "Thông tin đã được lưu thành công!");
    }

    // Phương thức để quay lại màn hình chính
    private void goBackToMainScreen() throws IOException {
         mainFrame.goBackToMainScreen(user);
    }
}
