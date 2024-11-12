package org.example.view;

import org.example.client.AudioClient;
import org.example.client.Client;
import org.example.client.HTTPClient;
import org.example.dto.Parser;
import org.example.dto.Question;
import org.example.dto.Message;
import org.example.dto.User;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;

import static java.lang.System.in;

public class Admin extends JPanel {

    private JTable questionTable;
    private DefaultTableModel tableModel;
    private JTextField answerField;
    private JTextField audioFileField;
    private JButton addQuestionButton, editQuestionButton, deleteQuestionButton, browseAudioButton;
    private File selectedAudioFile;
    private byte[] audioData;

    private User currentUser;
    private MainFrame frame;

    public Admin(User currentUser, MainFrame frame) {
        setLayout(new BorderLayout());
        this.currentUser = currentUser;
        this.frame = frame;

        String[] columnNames = {"ID", "Đáp án", "Path","Play Audio"};
        tableModel = new DefaultTableModel(columnNames, 0);
        questionTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(questionTable);
        add(scrollPane, BorderLayout.CENTER);

        // Panel cho các thao tác quản lý câu hỏi
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 1));

        JPanel addPanel = new JPanel();
        addPanel.setLayout(new FlowLayout());

        addQuestionButton = new JButton("Thêm câu hỏi");
        addPanel.add(addQuestionButton);

        audioFileField = new JTextField(20);
        audioFileField.setEditable(false);
        addPanel.add(new JLabel("File âm thanh:"));
        addPanel.add(audioFileField);

        browseAudioButton = new JButton("Chọn file âm thanh");
        addPanel.add(browseAudioButton);

        controlPanel.add(addPanel);  // Thêm panel vào controlPanel
        add(controlPanel, BorderLayout.SOUTH);  // Thêm controlPanel vào phía dưới của giao diện


        // Khu vực nhập đáp án đúng và các thao tác sửa/xóa
        JPanel editPanel = new JPanel();
        editPanel.setLayout(new FlowLayout());

        editQuestionButton = new JButton("Sửa câu hỏi");
        deleteQuestionButton = new JButton("Xóa câu hỏi");

        answerField = new JTextField(10);
        editPanel.add(new JLabel("Đáp án:"));
        editPanel.add(answerField);
        editPanel.add(editQuestionButton);
        editPanel.add(deleteQuestionButton);

        controlPanel.add(editPanel);

        // Sự kiện browse file âm thanh
        browseAudioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Chọn file âm thanh");
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Audio Files", "wav", "mp3", "aac"));
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedAudioFile = fileChooser.getSelectedFile();
                    audioFileField.setText(selectedAudioFile.getAbsolutePath());  // Hiển thị đường dẫn file vào JTextField
                    audioData = encodeAudioFile(selectedAudioFile);  // Lưu dữ liệu âm thanh vào byte array
                }
            }
        });

        // Sự kiện thêm câu hỏi
        addQuestionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addQuestion();
            }
        });

        // Sự kiện sửa câu hỏi
        editQuestionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editQuestion();
            }
        });

        // Sự kiện xóa câu hỏi
        deleteQuestionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteQuestion();
            }
        });

        // Áp dụng renderer và editor cho cột "Play Audio"
        questionTable.getColumn("Play Audio").setCellRenderer(new PlayAudioButtonRenderer());
        questionTable.getColumn("Play Audio").setCellEditor(new PlayAudioButtonEditor(new JCheckBox(), this.currentUser));
    }

    // Hàm tải câu hỏi từ cơ sở dữ liệu vào bảng
    public void loadQuestions(List<Question> questions) {
        tableModel.setRowCount(0); // Xóa tất cả các hàng cũ
        for (Question question : questions) {
            tableModel.addRow(new Object[]{question.getId(), question.getAnswer(), question.getSoundUrl(),"Play"});
        }
    }

    // Hàm thêm câu hỏi mới vào cơ sở dữ liệu và bảng
    private void addQuestion() {
        String answer = answerField.getText().trim();
        if (audioData == null || answer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ đáp án và chọn file âm thanh.");
            return;
        }

        try {
            // Cập nhật bảng câu hỏi
            int nextId = tableModel.getRowCount() + 1;  // Tạo ID tự động
            tableModel.addRow(new Object[]{nextId, answer, "Play"});

            String soundPath = HTTPClient.uploadFile(selectedAudioFile.getAbsolutePath());
            Question question = new Question(0, soundPath, answer);

            Message message = new Message(
                    -1,
                    "upload audio",
                    Parser.toJson(question),
                    null
            );

            Client.getInstance().sendSocketMessage(message);

            JOptionPane.showMessageDialog(this, "Câu hỏi đã được thêm.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm câu hỏi.");
        }
    }

    private byte[] encodeAudioFile(File file) {
        if (file != null) {
            try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Có lỗi khi mã hóa âm thanh.");
            }
        }
        return null;
    }

    private void editQuestion() {
        int selectedRow = questionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn câu hỏi để sửa.");
            return;
        }

        String newAnswer = answerField.getText().trim();
        if (audioData == null || newAnswer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ đáp án và chọn file âm thanh.");
            return;
        }

        try {
            tableModel.setValueAt(newAnswer, selectedRow, 1);
            JOptionPane.showMessageDialog(this, "Câu hỏi đã được cập nhật.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật câu hỏi.");
        }
    }

    private void deleteQuestion() {
        int selectedRow = questionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn câu hỏi để xóa.");
            return;
        }

        try {
            tableModel.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this, "Câu hỏi đã được xóa.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi xóa câu hỏi.");
        }
    }

    // Renderer cho nút Play Audio trong bảng
    private static class PlayAudioButtonRenderer extends JButton implements TableCellRenderer {
        public PlayAudioButtonRenderer() {
            setText("Play");
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // Editor cho nút Play Audio trong bảng
    private static class PlayAudioButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String audioPath;
        private User currentUser;

        public PlayAudioButtonEditor(JCheckBox checkBox, User user) {
            super(checkBox);
            button = new JButton("Play");
            button.addActionListener(e -> {
                try {
                    playAudio(audioPath);
                } catch (Exception ex) {
                        throw new RuntimeException(ex);
                }
            });
            this.currentUser = user;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            audioPath = (String) table.getValueAt(row, 2);
            return button;
        }

        private void playAudio(String path) throws Exception {
            AudioClient.getInstance().playAudio(path);
        }
    }

    public MainFrame getFrame() {
        return frame;
    }

    public void setFrame(MainFrame frame) {
        this.frame = frame;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
