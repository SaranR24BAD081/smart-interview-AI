package com.aiinterview.service;

import org.springframework.stereotype.Service;

/**
 * Section III-D: Speech-to-Text and Answer Analysis.
 *
 * Implements the confidence-score formula from Eq. 2:
 *
 *     C_conf = w1*f_pace + w2*f_pause + w3*f_filler + w4*f_pitch
 *
 * Paralinguistic features (f_pace, f_pause, f_filler, f_pitch) are expected
 * to already be normalized to [0,1] by the client (see AnswerSubmitRequest);
 * in a production system these would be extracted from the raw audio signal
 * server-side. Communication score additionally folds in a semantic
 * relevance check between the transcript and the question.
 */
@Service
public class SpeechAnalysisService {

    // Learned/tunable weights, w1..w4, must sum to 1.0.
    private static final double W_PACE = 0.30;
    private static final double W_PAUSE = 0.25;
    private static final double W_FILLER = 0.25;
    private static final double W_PITCH = 0.20;

    /** Eq. 2: weighted confidence score in [0,1]. */
    public double confidenceScore(double pace, double pause, double filler, double pitch) {
        double score = W_PACE * clamp01(pace)
                + W_PAUSE * clamp01(pause)
                + W_FILLER * clamp01(filler)
                + W_PITCH * clamp01(pitch);
        return clamp01(score);
    }

    /**
     * Communication score = blend of confidence score and simple lexical
     * relevance between transcript and question (word overlap ratio, as a
     * lightweight stand-in for a semantic-similarity model).
     */
    public double communicationScore(String transcript, String questionText, double confidenceScore) {
        double relevance = lexicalOverlap(transcript, questionText);
        return clamp01(0.5 * confidenceScore + 0.5 * relevance);
    }

    /** Overall per-question performance score p_t feeding Eq. 3. */
    public double performanceScore(double communicationScore, double confidenceScore) {
        return clamp01(0.6 * communicationScore + 0.4 * confidenceScore);
    }

    private double lexicalOverlap(String transcript, String question) {
        if (transcript == null || transcript.isBlank()) return 0.0;
        var tWords = wordSet(transcript);
        var qWords = wordSet(question);
        if (qWords.isEmpty()) return Math.min(1.0, tWords.size() / 40.0);
        long overlap = qWords.stream().filter(tWords::contains).count();
        double topicOverlap = (double) overlap / qWords.size();
        double lengthFactor = Math.min(1.0, tWords.size() / 30.0); // rewards substantive answers
        return clamp01(0.7 * topicOverlap + 0.3 * lengthFactor);
    }

    private java.util.Set<String> wordSet(String text) {
        if (text == null) return java.util.Set.of();
        java.util.Set<String> out = new java.util.HashSet<>();
        for (String w : text.toLowerCase(java.util.Locale.ROOT).split("[^a-zA-Z]+")) {
            if (w.length() > 2) out.add(w);
        }
        return out;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
