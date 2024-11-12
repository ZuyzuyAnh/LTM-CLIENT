package org.example.view;

import org.example.client.Client;
import org.example.dto.LoginRequest;
import org.example.dto.Message;
import org.example.dto.Parser;
import org.example.dto.User;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SignUp extends JPanel {
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton loginButton;
    private JButton signUpButton;

    private MainFrame frame;

    public SignUp() {
        setLayout(new GridBagLayout());

        JLabel usernameLabel = new JLabel("Username:");
        JLabel emailLabel = new JLabel("Email:");
        JLabel passwordLabel = new JLabel("Password:");
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");

        usernameField = new JTextField(15);
        emailField = new JTextField(15);
        passwordField = new JPasswordField(15);
        confirmPasswordField = new JPasswordField(15);
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
        add(emailLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(confirmPasswordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        add(confirmPasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(loginButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        add(signUpButton, gbc);

        loginButton.addActionListener(e -> {
            this.frame.showScreen("login");
        });

        signUpButton.addActionListener(e -> {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            User newUser = new User(0, username, email, password, "user", 0);

            Message message = new Message(
                    -1,
                    "signup",
                    Parser.toJson(newUser),
                    null
            );

            try {
                Client.getInstance().sendSocketMessage(message);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to send sign up request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public void setFrame(MainFrame frame) {
        this.frame = frame;
    }

    public void handle(Message message) {
        JOptionPane.showMessageDialog(this, message.getData());
    }
}
