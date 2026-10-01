package com.plagiarism;

import com.plagiarism.TextSimilarityAlgorithms.AnalysisResult;
import com.plagiarism.TextSimilarityAlgorithms.Verdict;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlagiarismApp extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextArea sourceTextArea = new JTextArea(12, 40);
    private final JTextArea targetTextArea = new JTextArea(12, 40);
    private final JLabel verdictLabel = new JLabel("Awaiting analysis");
    private final JLabel overallScoreLabel = new JLabel("0%");
    private final JLabel sourceMeta = new JLabel("Source document: sample text");
    private final JLabel targetMeta = new JLabel("Comparison document: sample text");
    private final JPanel resultMetricsPanel = new JPanel();
    private final javax.swing.JProgressBar scoreBar = new javax.swing.JProgressBar(0, 100);

    private static final Map<String, String> SAMPLE_TEXTS = new LinkedHashMap<>();

    static {
        SAMPLE_TEXTS.put("source", "Artificial intelligence is reshaping how modern businesses evaluate customer behavior and process data. By automating repetitive tasks and uncovering hidden patterns, organizations can improve decision-making quality, reduce operational costs, and deliver faster insight into customer needs.");
        SAMPLE_TEXTS.put("target", "Artificial intelligence is transforming how companies understand consumer behavior and manage information. With automation and pattern discovery, businesses can make stronger decisions, cut unnecessary costs, and gain quicker perspectives on customer needs and preferences.");
    }

    public PlagiarismApp() {
        super("Plagiarism Detection Studio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(20, 20));
        setBackground(new Color(247, 246, 255));

        initializeUi();
        loadSampleTexts();
        analyzeText();
    }

    private void initializeUi() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(247, 246, 255));

        JLabel title = new JLabel("Plagiarism Detection Studio");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel comparePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        comparePanel.setOpaque(false);
        comparePanel.add(createInputCard("Document A", sourceTextArea, sourceMeta, true));
        comparePanel.add(createInputCard("Document B", targetTextArea, targetMeta, false));

        mainPanel.add(comparePanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        actionPanel.setOpaque(false);
        JButton compareButton = createButton("Analyze documents", new Color(108, 92, 231));
        JButton sampleButton = createButton("Load sample", new Color(0, 180, 166));
        JButton clearButton = createButton("Clear", new Color(120, 120, 120));

        compareButton.addActionListener(event -> analyzeText());
        sampleButton.addActionListener(event -> loadSampleTexts());
        clearButton.addActionListener(event -> clearFields());

        actionPanel.add(compareButton);
        actionPanel.add(sampleButton);
        actionPanel.add(clearButton);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(108, 92, 231, 60), 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        resultPanel.setBackground(new Color(255, 255, 255));

        JPanel resultHeader = new JPanel(new BorderLayout());
        resultHeader.setOpaque(false);
        verdictLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        verdictLabel.setForeground(new Color(26, 23, 48));
        overallScoreLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        overallScoreLabel.setForeground(new Color(108, 92, 231));
        overallScoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        resultHeader.add(verdictLabel, BorderLayout.WEST);
        resultHeader.add(overallScoreLabel, BorderLayout.EAST);

        scoreBar.setStringPainted(false);
        scoreBar.setValue(0);
        scoreBar.setForeground(new Color(52, 211, 153));
        scoreBar.setBackground(new Color(238, 237, 249));

        resultMetricsPanel.setLayout(new BoxLayout(resultMetricsPanel, BoxLayout.Y_AXIS));
        resultMetricsPanel.setOpaque(false);
        resultMetricsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultPanel.add(resultHeader);
        resultPanel.add(Box.createVerticalStrut(12));
        resultPanel.add(scoreBar);
        resultPanel.add(Box.createVerticalStrut(16));
        resultPanel.add(resultMetricsPanel);

        add(resultPanel, BorderLayout.EAST);
        resultPanel.setPreferredSize(new Dimension(360, 0));
    }

    private JPanel createInputCard(String title, JTextArea textArea, JLabel infoLabel, boolean isSource) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(108, 92, 231, 40), 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setBackground(new Color(255, 255, 255));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel cardTitle = new JLabel(title);
        cardTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        topBar.add(cardTitle, BorderLayout.WEST);

        JButton uploadButton = new JButton("Upload file");
        uploadButton.addActionListener(event -> handleFileUpload(isSource));
        topBar.add(uploadButton, BorderLayout.EAST);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(textArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(topBar, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        card.add(infoLabel, BorderLayout.SOUTH);
        return card;
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        return button;
    }

    private void handleFileUpload(boolean isSource) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "rtf"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedPath = chooser.getSelectedFile().toPath();
        try {
            String content = Files.readString(selectedPath, StandardCharsets.UTF_8);
            if (isSource) {
                sourceTextArea.setText(content);
                sourceMeta.setText("Source document: " + chooser.getSelectedFile().getName());
            } else {
                targetTextArea.setText(content);
                targetMeta.setText("Comparison document: " + chooser.getSelectedFile().getName());
            }
            analyzeText();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Unable to read the selected file. Please paste content manually instead.\n\n" + ex.getMessage(),
                "File read error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSampleTexts() {
        sourceTextArea.setText(SAMPLE_TEXTS.get("source"));
        targetTextArea.setText(SAMPLE_TEXTS.get("target"));
        sourceMeta.setText("Source document: sample text");
        targetMeta.setText("Comparison document: sample text");
        analyzeText();
    }

    private void clearFields() {
        sourceTextArea.setText("");
        targetTextArea.setText("");
        sourceMeta.setText("Source document: no file loaded");
        targetMeta.setText("Comparison document: no file loaded");
        verdictLabel.setText("Awaiting analysis");
        overallScoreLabel.setText("0%");
        scoreBar.setValue(0);
        resultMetricsPanel.removeAll();
        resultMetricsPanel.revalidate();
        resultMetricsPanel.repaint();
    }

    private void analyzeText() {
        String docA = sourceTextArea.getText().trim();
        String docB = targetTextArea.getText().trim();

        if (docA.isEmpty() || docB.isEmpty()) {
            verdictLabel.setText("Enter both texts");
            verdictLabel.setForeground(new Color(74, 69, 104));
            overallScoreLabel.setText("0%");
            scoreBar.setValue(0);
            resultMetricsPanel.removeAll();
            JLabel emptyState = new JLabel("Add text or upload a document to view similarity metrics.");
            emptyState.setForeground(new Color(123, 115, 143));
            resultMetricsPanel.add(emptyState);
            resultMetricsPanel.revalidate();
            resultMetricsPanel.repaint();
            return;
        }

        AnalysisResult result = TextSimilarityAlgorithms.computeOverallSimilarity(docA, docB);
        renderMetrics(result.getMetrics());
        updateSummary(result.getAverage());
    }

    private void renderMetrics(Map<String, Double> metrics) {
        resultMetricsPanel.removeAll();

        String[] labels = {"Jaccard", "Cosine", "Dice", "Levenshtein", "LCS", "Rabin-Karp"};
        Map<String, String> metricKeys = new LinkedHashMap<>();
        metricKeys.put("Jaccard", "jaccard");
        metricKeys.put("Cosine", "cosine");
        metricKeys.put("Dice", "dice");
        metricKeys.put("Levenshtein", "levenshtein");
        metricKeys.put("LCS", "lcs");
        metricKeys.put("Rabin-Karp", "rabinKarp");

        for (String label : labels) {
            String key = metricKeys.get(label);
            double value = metrics.getOrDefault(key, 0.0);
            JPanel metricRow = new JPanel(new BorderLayout(10, 4));
            metricRow.setOpaque(false);

            JLabel metricLabel = new JLabel(label);
            metricLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            metricLabel.setForeground(new Color(26, 23, 48));

            JLabel metricValue = new JLabel(formatPercent(value));
            metricValue.setFont(new Font("SansSerif", Font.BOLD, 13));
            metricValue.setForeground(new Color(108, 92, 231));
            metricValue.setHorizontalAlignment(SwingConstants.RIGHT);

            javax.swing.JProgressBar bar = new javax.swing.JProgressBar(0, 100);
            bar.setValue((int) Math.round(value * 100));
            bar.setStringPainted(false);
            bar.setForeground(new Color(108, 92, 231));
            bar.setBackground(new Color(238, 237, 249));

            metricRow.add(metricLabel, BorderLayout.WEST);
            metricRow.add(metricValue, BorderLayout.EAST);
            metricRow.add(bar, BorderLayout.SOUTH);
            resultMetricsPanel.add(metricRow);
            resultMetricsPanel.add(Box.createVerticalStrut(8));
        }

        resultMetricsPanel.revalidate();
        resultMetricsPanel.repaint();
    }

    private void updateSummary(double score) {
        Verdict verdict = TextSimilarityAlgorithms.getVerdict(score);
        verdictLabel.setText(verdict.getLabel());
        verdictLabel.setForeground(getToneColor(verdict.getTone()));
        overallScoreLabel.setText(formatPercent(score));
        overallScoreLabel.setForeground(getToneColor(verdict.getTone()));
        scoreBar.setValue((int) Math.round(score * 100));
        scoreBar.setForeground(getScoreColor(verdict.getTone()));
    }

    private static String formatPercent(double value) {
        return Math.round(value * 100) + "%";
    }

    private Color getToneColor(String tone) {
        switch (tone) {
            case "danger":
                return new Color(232, 64, 64);
            case "warning":
                return new Color(245, 158, 11);
            case "neutral":
                return new Color(96, 165, 250);
            case "safe":
                return new Color(16, 185, 129);
            default:
                return new Color(26, 23, 48);
        }
    }

    private Color getScoreColor(String tone) {
        switch (tone) {
            case "danger":
                return new Color(255, 107, 107);
            case "warning":
                return new Color(255, 159, 67);
            case "neutral":
                return new Color(96, 165, 250);
            case "safe":
                return new Color(53, 211, 153);
            default:
                return new Color(108, 92, 231);
        }
    }
}
