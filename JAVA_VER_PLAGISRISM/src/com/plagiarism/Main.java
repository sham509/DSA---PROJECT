package com.plagiarism;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PlagiarismApp app = new PlagiarismApp();
            app.setVisible(true);
        });
    }
}
