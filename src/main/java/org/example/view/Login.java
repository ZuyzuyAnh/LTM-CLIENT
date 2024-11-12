package org.example.view;

import org.example.client.Client;
import org.example.dto.LoginRequest;
import org.example.dto.Message;
import org.example.dto.Parser;
import org.example.dto.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.http.WebSocket;

public class Login extends JPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signUpButton;

    private MainFrame frame;

    public Login() {
        setLayout(new GridBagLayout());

        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        loginButton = new JButton("Login");
        signUpButton = new JButton("Sign Up");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(loginButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(signUpButton, gbc);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            LoginRequest request = new LoginRequest(username, password);

            Message message = new Message(
                    -1,
                    "login",
                    Parser.toJson(request),
                    null
            );

            try {
                Client.getInstance().sendSocketMessage(message);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        signUpButton.addActionListener(e -> {
            this.frame.showScreen("signUp");
        });
    }

    public void setFrame(MainFrame frame) {
        this.frame = frame;
    }

    public void submit(User user) throws IOException {
        Message message;
        if (user.getRole().equals("admin")) {
            message = new Message(
                    user.getId(),
                    "questions",
                    null,
                    null
            );

        }else {
            message = new Message(
                    user.getId(),
                    "leaderboard",
                    null,
                    null
            );
        }
        Client.getInstance().sendSocketMessage(message);
    }
}


