package org.example;

import org.example.view.MainFrame;

import javax.swing.*;

public class Main2 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
