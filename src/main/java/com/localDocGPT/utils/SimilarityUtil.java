package com.localDocGPT.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SimilarityUtil {

    public static double cosineSimilarity(List<Float> questionVector, List<Double> b) {
        if (questionVector == null || b == null) return 0.0;
        if (questionVector.size() != b.size()) throw new IllegalArgumentException("Vectors must be the same length.");

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < questionVector.size(); i++) {
            double aVal = questionVector.get(i) == null ? 0.0 : questionVector.get(i);
            double bVal = b.get(i) == null ? 0.0 : b.get(i);
            dot += aVal * bVal;
            normA += aVal * aVal;
            normB += bVal * bVal;
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        if (denom == 0.0) return 0.0;
        return dot / denom;
    }

    public static List<Double> parseEmbedding(String embeddingString) {
        if (embeddingString == null) return List.of();
        // remove brackets if present and normalize separators
        String cleaned = embeddingString.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) return List.of();
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Double.parseDouble(s);
                    } catch (NumberFormatException ex) {
                        // fallback to 0.0 for malformed numbers
                        return 0.0;
                    }
                })
                .collect(Collectors.toList());
    }
}
