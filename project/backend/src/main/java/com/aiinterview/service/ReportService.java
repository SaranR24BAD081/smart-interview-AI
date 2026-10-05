package com.aiinterview.service;

import com.aiinterview.model.CodingSubmission;
import com.aiinterview.model.InterviewSession;
import com.aiinterview.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Section III-G: Performance Report Generation.
 *
 * Aggregates resume-relevance, communication/confidence scores, difficulty
 * progression, and coding-assessment results into a single structured
 * report highlighting strengths and improvement areas, as described in the
 * paper.
 *
 * IMPORTANT: this takes the session's questions and coding submissions as
 * explicit parameters (fetched by the caller via QuestionRepository /
 * CodingSubmissionRepository) rather than reading session.getQuestions() /
 * session.getCodingSubmissions() directly. Those are LAZY collections and
 * open-in-view is disabled, so touching them here (outside a transaction)
 * would throw "could not initialize proxy - no Session".
 */
@Service
public class ReportService {

    public Map<String, Object> buildReport(InterviewSession session, List<Question> questions, List<CodingSubmission> submissions) {
        double avgCommunication = average(questions, Question::getCommunicationScore);
        double avgConfidence = average(questions, Question::getConfidenceScore);
        double avgPerformance = average(questions, Question::getPerformanceScore);
        double avgCoding = submissions.stream()
                .mapToDouble(s -> s.getScore() == null ? 0.0 : s.getScore())
                .average().orElse(0.0);

        List<Integer> difficultyProgression = new ArrayList<>();
        for (Question q : questions) difficultyProgression.add(q.getDifficulty());

        List<String> strengths = new ArrayList<>();
        List<String> improvementAreas = new ArrayList<>();

        addFeedback(strengths, improvementAreas, "Communication clarity", avgCommunication);
        addFeedback(strengths, improvementAreas, "Confidence / delivery", avgConfidence);
        if (!submissions.isEmpty()) {
            addFeedback(strengths, improvementAreas, "Coding proficiency", avgCoding);
        }

        // Per-question breakdown: each question's own answer with its own
        // scores, kept separate rather than folded into the averages above.
        // (The averages above are still useful as a one-line summary, but on
        // their own they hide which specific answer earned which score.)
        List<Map<String, Object>> questionBreakdown = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("questionText", q.getQuestionText());
            entry.put("type", q.getType());
            entry.put("difficulty", q.getDifficulty());
            entry.put("answerTranscript", q.getAnswerTranscript());
            entry.put("communicationScore", round(orZero(q.getCommunicationScore())));
            entry.put("confidenceScore", round(orZero(q.getConfidenceScore())));
            entry.put("performanceScore", round(orZero(q.getPerformanceScore())));
            questionBreakdown.add(entry);
        }

        List<Map<String, Object>> codingBreakdown = new ArrayList<>();
        for (CodingSubmission s : submissions) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("title", s.getTitle());
            entry.put("problemStatement", s.getProblemStatement());
            entry.put("language", s.getLanguage());
            entry.put("candidateCode", s.getCandidateCode());
            entry.put("testsPassed", s.getTestsPassed());
            entry.put("testsTotal", s.getTestsTotal());
            entry.put("score", round(orZero(s.getScore())));
            codingBreakdown.add(entry);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("sessionId", session.getId());
        report.put("candidateName", session.getCandidateName());
        report.put("jobRole", session.getJobRole());
        report.put("questionsAnswered", questions.size());
        report.put("averageCommunicationScore", round(avgCommunication));
        report.put("averageConfidenceScore", round(avgConfidence));
        report.put("averageOverallPerformance", round(avgPerformance));
        report.put("averageCodingScore", round(avgCoding));
        report.put("difficultyProgression", difficultyProgression);
        report.put("finalDifficulty", session.getCurrentDifficulty());
        report.put("strengths", strengths);
        report.put("improvementAreas", improvementAreas);
        report.put("questionBreakdown", questionBreakdown);
        report.put("codingBreakdown", codingBreakdown);
        return report;
    }

    private double orZero(Double v) {
        return v == null ? 0.0 : v;
    }


    private void addFeedback(List<String> strengths, List<String> improvementAreas, String label, double score) {
        if (score >= 0.7) {
            strengths.add(label + " is strong (" + round(score * 100) + "%).");
        } else if (score < 0.5) {
            improvementAreas.add(label + " needs focused practice (" + round(score * 100) + "%).");
        } else {
            improvementAreas.add(label + " is developing steadily (" + round(score * 100) + "%) -- keep practicing.");
        }
    }

    private double average(List<Question> questions, java.util.function.Function<Question, Double> extractor) {
        return questions.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
