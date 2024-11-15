package org.example.view;

import org.example.client.Client;
import org.example.dto.Message;
import org.example.dto.Parser;
import org.example.dto.User;
import org.example.view.MainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Home extends JPanel {

    private MainFrame frame;

    private JTable playerTable;
    private JButton logoutButton;
    private JButton inviteButton;
    private JLabel currentUserLabel;
    private DefaultTableModel tableModel;
    private Map<String, User> players = new LinkedHashMap<>(); // Khởi tạo map cho danh sách người chơi
    private Set<String> onlineUsernames = new HashSet<>(); // Lưu danh sách username online

    private User currentUser = new User(0, "", "", "", "", 0);
    public Home(User currentUser) {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        this.currentUser = currentUser;
        currentUserLabel = new JLabel(currentUser.getUsername() + " - " + currentUser.getScore() + " điểm");
        currentUserLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        currentUserLabel.setForeground(Color.BLUE);
        currentUserLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                Message message = new Message(
                        currentUser.getId(),
                        "history",
                        null,
                        null
                );

                try {
                    Client.getInstance().sendSocketMessage(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        topPanel.add(currentUserLabel, BorderLayout.WEST);

        logoutButton = new JButton("Log out");
        logoutButton.addActionListener(e -> {
            try {
                logout();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        topPanel.add(logoutButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Tên người chơi", "Điểm", "Trạng thái"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        playerTable = new JTable(tableModel);
        playerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(playerTable);
        add(scrollPane, BorderLayout.CENTER);

        inviteButton = new JButton("Gửi lời mời");
        inviteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sendInvite();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        add(inviteButton, BorderLayout.SOUTH);
    }

    private void openPersonalInfo() {
//        JOptionPane.showMessageDialog(this, "Thông tin cá nhân của " + currentUser + ":\nĐiểm: " + currentUserScore);
    }

    private void logout() throws IOException {
        Message message = new Message(
                currentUser.getId(),
                "logout",
                null,
                null
        );

        Client.getInstance().sendSocketMessage(message);

        this.frame.showScreen("login");
    }

    private void sendInvite() throws IOException {
        int selectedRow = playerTable.getSelectedRow();
        if (selectedRow != -1) {
            String selectedPlayerName = (String) tableModel.getValueAt(selectedRow, 0);
            String status = (String) tableModel.getValueAt(selectedRow, 2);
            User selectedPlayer = players.get(selectedPlayerName);
            if ("Online".equals(status)) {
                Message inviteMsg = new Message(
                        currentUser.getId(),
                        "invite",
                        Parser.toJson(selectedPlayer.getId()),
                        null
                );

                Client.getInstance().sendSocketMessage(inviteMsg);
                JOptionPane.showMessageDialog(this, "Đã gửi lời mời tới " + selectedPlayer.getUsername());
            } else {
                JOptionPane.showMessageDialog(this, selectedPlayer.getUsername() + " hiện không trực tuyến!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người chơi trực tuyến từ danh sách!");
        }
    }

    private void updatePlayerList() {
        tableModel.setRowCount(0);
        players.values().forEach(player -> {
            if (!player.getRole().equals("admin")) {
                String status = onlineUsernames.contains(player.getUsername()) ? "Online" : "Offline";
                if (!player.getUsername().equals(currentUser.getUsername())) {
                    tableModel.addRow(new Object[]{player.getUsername(), player.getScore(), status});
                }
                if (player.getId() == currentUser.getId()) {
                    this.frame.setCurrentUser(player);
                    currentUserLabel.setText( player.getUsername() + " - " + player.getScore() + " điểm");
                }
            }
        });
    }

    public void handleOnline(Message message) {
        List<User> online = Parser.fromJsonArray(message.getData(), User.class);
        onlineUsernames = online.stream()
                .map(user -> {
                    if (!user.getRole().equals("admin")) {
                        if (!players.containsKey(user.getUsername())) {
                            players.put(user.getUsername(), user);
                        }
                        return user.getUsername();
                    }
                    return "";
                })
                .collect(Collectors.toSet());

        updatePlayerList();
    }

    public void handlePlaying(Message message) {
        List<User> playingUsers = Parser.fromJsonArray(message.getData(), User.class);

        Set<String> playingUsernames = playingUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String username = (String) tableModel.getValueAt(i, 0);
            String status;

            if (playingUsernames.contains(username)) {
                status = "Playing";
            } else if (onlineUsernames.contains(username)) {
                status = "Online";
            } else {
                status = "Offline";
            }

            tableModel.setValueAt(status, i, 2);
        }

        revalidate();
        repaint();
    }


    public void handleLeaderBoard(Message message) {
        List<User> leaderboard = Parser.fromJsonArray(message.getData(), User.class);

        players.clear();
        leaderboard.forEach(user -> {
            players.put(user.getUsername(), user);
        });

        updatePlayerList();
    }

    public void setFrame(MainFrame mainFrame) {
        this.frame = mainFrame;
    }

}
