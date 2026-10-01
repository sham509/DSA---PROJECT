package com.plagiarism;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class TextSimilarityAlgorithms {
    private TextSimilarityAlgorithms() {
    }

    public static final class AnalysisResult {
        private final Map<String, Double> metrics;
        private final double average;

        public AnalysisResult(Map<String, Double> metrics, double average) {
            this.metrics = metrics;
            this.average = average;
        }

        public Map<String, Double> getMetrics() {
            return metrics;
        }

        public double getAverage() {
            return average;
        }
    }

    public static final class Verdict {
        private final String label;
        private final String tone;

        public Verdict(String label, String tone) {
            this.label = label;
            this.tone = tone;
        }

        public String getLabel() {
            return label;
        }

        public String getTone() {
            return tone;
        }
    }

    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }

        return input.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    public static List<String> tokenize(String input) {
        String normalized = normalizeText(input);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(normalized.split("\\s+"))
            .filter(token -> !token.isBlank())
            .toList();
    }

    public static Map<String, Integer> createFrequencyMap(List<String> tokens) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String token : tokens) {
            map.put(token, map.getOrDefault(token, 0) + 1);
        }
        return map;
    }

    public static long hashWord(String word) {
        long hash = 0;
        for (char ch : word.toCharArray()) {
            hash = (hash * 31L + ch) >>> 0;
        }
        return hash;
    }

    public static List<Long> rollingHashWindows(List<String> tokens, int windowSize) {
        if (tokens == null || tokens.isEmpty() || windowSize <= 0) {
            return new ArrayList<>();
        }

        List<Long> values = new ArrayList<>();
        for (String token : tokens) {
            values.add(hashWord(token));
        }

        long base = 911382629L;
        long mod = 1_000_000_007L;

        if (values.size() < windowSize) {
            long hashValue = 0L;
            for (long value : values) {
                hashValue = (hashValue * base + value) % mod;
            }
            return List.of(hashValue);
        }

        long hashValue = 0L;
        for (int index = 0; index < windowSize; index++) {
            hashValue = (hashValue * base + values.get(index)) % mod;
        }

        List<Long> hashes = new ArrayList<>();
        hashes.add(hashValue);
        long power = (long) Math.pow(base, windowSize - 1) % mod;

        for (int index = windowSize; index < values.size(); index++) {
            long outgoing = values.get(index - windowSize);
            long incoming = values.get(index);
            hashValue = ((hashValue - outgoing * power) % mod + mod) % mod;
            hashValue = (hashValue * base + incoming) % mod;
            hashes.add(hashValue);
        }

        return hashes;
    }

    public static double rabinKarpSimilarity(String textA, String textB) {
        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);

        if (tokensA.isEmpty() && tokensB.isEmpty()) {
            return 1.0;
        }
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        int windowSize = Math.min(3, Math.max(1, Math.min(tokensA.size(), tokensB.size())));
        List<Long> hashesA = rollingHashWindows(tokensA, windowSize);
        List<Long> hashesB = rollingHashWindows(tokensB, windowSize);

        if (hashesA.isEmpty() || hashesB.isEmpty()) {
            return 0.0;
        }

        Set<Long> setA = new TreeSet<>(hashesA);
        Set<Long> setB = new TreeSet<>(hashesB);
        long intersection = setA.stream().filter(setB::contains).count();
        Set<Long> union = new TreeSet<>();
        union.addAll(setA);
        union.addAll(setB);

        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection / union.size();
    }

    public static double jaccardSimilarity(String textA, String textB) {
        Set<String> setA = new TreeSet<>(tokenize(textA));
        Set<String> setB = new TreeSet<>(tokenize(textB));

        if (setA.isEmpty() && setB.isEmpty()) {
            return 1.0;
        }
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new TreeSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new TreeSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }

    public static double diceCoefficient(String textA, String textB) {
        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);

        if (tokensA.isEmpty() && tokensB.isEmpty()) {
            return 1.0;
        }
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> setA = new TreeSet<>(tokensA);
        Set<String> setB = new TreeSet<>(tokensB);
        Set<String> intersection = new TreeSet<>(setA);
        intersection.retainAll(setB);

        return (2.0 * intersection.size()) / (setA.size() + setB.size());
    }

    public static double cosineSimilarity(String textA, String textB) {
        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);

        if (tokensA.isEmpty() && tokensB.isEmpty()) {
            return 1.0;
        }
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Map<String, Integer> freqA = createFrequencyMap(tokensA);
        Map<String, Integer> freqB = createFrequencyMap(tokensB);
        Set<String> terms = new TreeSet<>();
        terms.addAll(freqA.keySet());
        terms.addAll(freqB.keySet());

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (String term : terms) {
            int a = freqA.getOrDefault(term, 0);
            int b = freqB.getOrDefault(term, 0);
            dotProduct += a * b;
            magnitudeA += a * a;
            magnitudeB += b * b;
        }

        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }

    public static int levenshteinDistance(String textA, String textB) {
        String source = normalizeText(textA);
        String target = normalizeText(textB);

        if (source.isEmpty() && target.isEmpty()) {
            return 0;
        }
        if (source.isEmpty()) {
            return target.length();
        }
        if (target.isEmpty()) {
            return source.length();
        }

        int[][] matrix = new int[source.length() + 1][target.length() + 1];

        for (int i = 0; i <= source.length(); i++) {
            matrix[i][0] = i;
        }
        for (int j = 0; j <= target.length(); j++) {
            matrix[0][j] = j;
        }

        for (int i = 1; i <= source.length(); i++) {
            for (int j = 1; j <= target.length(); j++) {
                int substitutionCost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                matrix[i][j] = Math.min(
                    Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1),
                    matrix[i - 1][j - 1] + substitutionCost
                );
            }
        }

        return matrix[source.length()][target.length()];
    }

    public static double levenshteinSimilarity(String textA, String textB) {
        String source = normalizeText(textA);
        String target = normalizeText(textB);
        int maxLength = Math.max(source.length(), Math.max(target.length(), 1));
        int distance = levenshteinDistance(source, target);
        return 1.0 - (distance / (double) maxLength);
    }

    public static double longestCommonSubsequenceSimilarity(String textA, String textB) {
        List<String> source = tokenize(textA);
        List<String> target = tokenize(textB);

        if (source.isEmpty() && target.isEmpty()) {
            return 1.0;
        }
        if (source.isEmpty() || target.isEmpty()) {
            return 0.0;
        }

        int[][] matrix = new int[source.size() + 1][target.size() + 1];

        for (int i = 1; i <= source.size(); i++) {
            for (int j = 1; j <= target.size(); j++) {
                if (source.get(i - 1).equals(target.get(j - 1))) {
                    matrix[i][j] = matrix[i - 1][j - 1] + 1;
                } else {
                    matrix[i][j] = Math.max(matrix[i - 1][j], matrix[i][j - 1]);
                }
            }
        }

        int lcsLength = matrix[source.size()][target.size()];
        int totalWords = Math.max(source.size(), Math.max(target.size(), 1));
        return lcsLength / (double) totalWords;
    }

    public static AnalysisResult computeOverallSimilarity(String textA, String textB) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("jaccard", jaccardSimilarity(textA, textB));
        metrics.put("cosine", cosineSimilarity(textA, textB));
        metrics.put("dice", diceCoefficient(textA, textB));
        metrics.put("levenshtein", levenshteinSimilarity(textA, textB));
        metrics.put("lcs", longestCommonSubsequenceSimilarity(textA, textB));
        metrics.put("rabinKarp", rabinKarpSimilarity(textA, textB));

        double average = metrics.values().stream().mapToDouble(value -> value).average().orElse(0.0);
        return new AnalysisResult(metrics, average);
    }

    public static Verdict getVerdict(double score) {
        if (score >= 0.8) {
            return new Verdict("High similarity risk", "danger");
        }
        if (score >= 0.5) {
            return new Verdict("Moderate similarity risk", "warning");
        }
        if (score >= 0.2) {
            return new Verdict("Low similarity risk", "neutral");
        }
        return new Verdict("Distinct content", "safe");
    }
}
