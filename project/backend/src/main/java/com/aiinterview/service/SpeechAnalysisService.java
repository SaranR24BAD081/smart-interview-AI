package com.aiinterview.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Section III-D: Speech-to-Text and Answer Analysis.
 *
 * Implements the confidence-score formula from Eq. 2:
 *
 *     C_conf = w1*f_pace + w2*f_pause + w3*f_filler + w4*f_pitch
 *
 * Paralinguistic features (f_pace, f_pause, f_filler, f_pitch) are expected
 * to already be normalized to [0,1] by the client (see AnswerSubmitRequest).
 * Communication score folds in a semantic relevance check between the
 * transcript and the question.
 */
@Service
public class SpeechAnalysisService {

    // Learned/tunable weights, w1..w4, must sum to 1.0.
    private static final double W_PACE   = 0.30;
    private static final double W_PAUSE  = 0.25;
    private static final double W_FILLER = 0.25;
    private static final double W_PITCH  = 0.20;

    /**
     * Threshold above which an answer is considered correct/acceptable.
     * 0.45 gives reasonable tolerance for natural speech imperfections.
     */
    private static final double CORRECT_THRESHOLD = 0.45;

    /** Eq. 2: weighted confidence score in [0,1]. */
    public double confidenceScore(double pace, double pause, double filler, double pitch) {
        return clamp01(W_PACE   * clamp01(pace)
                     + W_PAUSE  * clamp01(pause)
                     + W_FILLER * clamp01(filler)
                     + W_PITCH  * clamp01(pitch));
    }

    /**
     * Communication score = blend of confidence score and simple lexical
     * relevance between transcript and question (word-overlap ratio as a
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

    /**
     * Determines whether the answer is correct/acceptable.
     * An answer is correct if performanceScore >= CORRECT_THRESHOLD.
     */
    public boolean analyzeCorrectness(double performanceScore) {
        return performanceScore >= CORRECT_THRESHOLD;
    }

    /**
     * Generates a concise, human-readable feedback string explaining the
     * analysis result — what was strong and what can be improved.
     */
    public String generateFeedback(String transcript, String questionText,
                                   double confidenceScore, double communicationScore,
                                   double performanceScore, boolean correct) {

        if (transcript == null || transcript.isBlank()) {
            return "No answer was provided. Take time to think and attempt the question — " +
                   "even a partial answer demonstrates engagement.";
        }

        List<String> strengths    = new ArrayList<>();
        List<String> improvements = new ArrayList<>();

        // --- Confidence sub-score feedback ---
        if (confidenceScore >= 0.75) {
            strengths.add("strong vocal delivery (confident pace and few filler words)");
        } else if (confidenceScore >= 0.50) {
            improvements.add("reduce filler words (e.g. 'um', 'uh', 'like') and maintain a steady pace");
        } else {
            improvements.add("practice pacing and minimising filler words to sound more confident");
        }

        // --- Communication / relevance sub-score feedback ---
        double relevance = lexicalOverlap(transcript, questionText);
        int wordCount = transcript.trim().split("\\s+").length;

        if (relevance >= 0.60) {
            strengths.add("answer directly addressed the question");
        } else if (relevance >= 0.30) {
            improvements.add("connect your answer more directly to the question keywords");
        } else {
            improvements.add("re-read the question carefully and focus your answer on what is being asked");
        }

        if (wordCount >= 40) {
            strengths.add("substantive, well-developed response");
        } else if (wordCount >= 15) {
            improvements.add("expand with specific examples or reasoning");
        } else {
            improvements.add("provide a more complete answer — aim for at least 30-50 words");
        }

        // --- Build the feedback string ---
        StringBuilder sb = new StringBuilder();
        if (correct) {
            sb.append("✅ Good answer! ");
        } else {
            sb.append("❌ Needs improvement. ");
        }
        if (!strengths.isEmpty()) {
            sb.append("Strengths: ").append(String.join("; ", strengths)).append(". ");
        }
        if (!improvements.isEmpty()) {
            sb.append("To improve: ").append(String.join("; ", improvements)).append(".");
        }
        return sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private double lexicalOverlap(String transcript, String question) {
        if (transcript == null || transcript.isBlank()) return 0.0;
        Set<String> tWords = wordSet(transcript);
        Set<String> qWords = wordSet(question);
        if (qWords.isEmpty()) return Math.min(1.0, tWords.size() / 40.0);
        long overlap       = qWords.stream().filter(tWords::contains).count();
        double topicOverlap = (double) overlap / qWords.size();
        double lengthFactor = Math.min(1.0, tWords.size() / 30.0);
        return clamp01(0.7 * topicOverlap + 0.3 * lengthFactor);
    }

    private Set<String> wordSet(String text) {
        if (text == null) return Set.of();
        Set<String> out = new HashSet<>();
        for (String w : text.toLowerCase(Locale.ROOT).split("[^a-zA-Z]+")) {
            if (w.length() > 2) out.add(w);
        }
        return out;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
