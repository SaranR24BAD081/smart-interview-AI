package com.aiinterview.controller;

import com.aiinterview.dto.AnswerSubmitRequest;
import com.aiinterview.model.InterviewSession;
import com.aiinterview.model.Question;
import com.aiinterview.repository.InterviewSessionRepository;
import com.aiinterview.repository.QuestionRepository;
import com.aiinterview.service.AdaptiveDifficultyService;
import com.aiinterview.service.QuestionGenerationService;
import com.aiinterview.service.ResumeAnalyzerService;
import com.aiinterview.service.SpeechAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Sections III-B (Question Generation), III-C (MetaHuman delivery hand-off),
 * III-D (Speech-to-Text / Answer Analysis), III-E (Adaptive Difficulty).
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/questions")
public class QuestionController {

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final ResumeAnalyzerService resumeAnalyzerService;
    private final QuestionGenerationService questionGenerationService;
    private final SpeechAnalysisService speechAnalysisService;
    private final AdaptiveDifficultyService adaptiveDifficultyService;

    public QuestionController(InterviewSessionRepository sessionRepository,
                               QuestionRepository questionRepository,
                               ResumeAnalyzerService resumeAnalyzerService,
                               QuestionGenerationService questionGenerationService,
                               SpeechAnalysisService speechAnalysisService,
                               AdaptiveDifficultyService adaptiveDifficultyService) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.resumeAnalyzerService = resumeAnalyzerService;
        this.questionGenerationService = questionGenerationService;
        this.speechAnalysisService = speechAnalysisService;
        this.adaptiveDifficultyService = adaptiveDifficultyService;
    }

    /**
     * Generates the next personalized question at the session's current
     * adaptive difficulty. The frontend passes this text to the TTS/avatar
     * layer (Section III-C) for delivery.
     */
    @PostMapping("/next")
    public Question nextQuestion(@PathVariable Long sessionId,
                                  @RequestParam(defaultValue = "BEHAVIORAL") Question.QuestionType type) {
        InterviewSession session = getSession(sessionId);

        List<String> ranked = resumeAnalyzerService.rankedSkillsForRole(session.getResumeText(), session.getJobRole());
        // Count via the repository rather than session.getQuestions(): that collection
        // is LAZY and open-in-view is disabled, so touching it here (outside a
        // transaction) would throw a LazyInitializationException.
        int index = questionRepository.findBySessionIdOrderByIdAsc(sessionId).size();

        String text = questionGenerationService.generateQuestion(
                session.getJobRole(), ranked, session.getCurrentDifficulty(), type, index);

        Question question = new Question();
        question.setSession(session);
        question.setQuestionText(text);
        question.setType(type);
        question.setDifficulty(session.getCurrentDifficulty());

        session.setStatus(InterviewSession.SessionStatus.IN_PROGRESS);
        sessionRepository.save(session);

        return questionRepository.save(question);
    }

    /**
     * Section III-D + III-E: accepts the STT transcript and client-measured
     * paralinguistic features for a question, scores it (Eq. 2), determines
     * correctness, generates feedback, and updates adaptive difficulty (Eq. 3).
     */
    @PostMapping("/answer")
    public Map<String, Object> submitAnswer(@PathVariable Long sessionId,
                                            @RequestBody AnswerSubmitRequest req) {
        InterviewSession session = getSession(sessionId);
        Question question = questionRepository.findById(req.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + req.getQuestionId()));

        double confidence    = speechAnalysisService.confidenceScore(
                req.getPaceScore(), req.getPauseScore(), req.getFillerScore(), req.getPitchScore());
        double communication = speechAnalysisService.communicationScore(
                req.getTranscript(), question.getQuestionText(), confidence);
        double performance   = speechAnalysisService.performanceScore(communication, confidence);
        boolean correct      = speechAnalysisService.analyzeCorrectness(performance);
        String feedback      = speechAnalysisService.generateFeedback(
                req.getTranscript(), question.getQuestionText(),
                confidence, communication, performance, correct);

        question.setAnswerTranscript(req.getTranscript());
        question.setConfidenceScore(confidence);
        question.setCommunicationScore(communication);
        question.setPerformanceScore(performance);
        question.setAnswerCorrect(correct);
        question.setFeedback(feedback);
        questionRepository.save(question);

        int nextDifficulty = adaptiveDifficultyService.nextDifficulty(session.getCurrentDifficulty(), performance);
        session.setCurrentDifficulty(nextDifficulty);
        sessionRepository.save(session);

        return Map.of(
                "confidenceScore",    confidence,
                "communicationScore", communication,
                "performanceScore",   performance,
                "nextDifficulty",     nextDifficulty,
                "answerCorrect",      correct,
                "feedback",           feedback
        );
    }

    /**
     * Allows the candidate to skip a question they don't know the answer to.
     * The question is marked as skipped with a penalised performance score
     * so the adaptive difficulty controller can ease back appropriately.
     */
    @PostMapping("/skip")
    public Map<String, Object> skipQuestion(@PathVariable Long sessionId,
                                            @RequestBody Map<String, Long> body) {
        InterviewSession session = getSession(sessionId);
        Long questionId = body.get("questionId");
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        double penaltyPerformance = 0.20;
        String skipFeedback = "Question skipped. That's okay — use it as a learning opportunity. " +
                "Review the topic related to this question before your real interview.";

        question.setSkipped(true);
        question.setAnswerCorrect(false);
        question.setFeedback(skipFeedback);
        question.setPerformanceScore(penaltyPerformance);
        question.setConfidenceScore(0.0);
        question.setCommunicationScore(0.0);
        questionRepository.save(question);

        int nextDifficulty = adaptiveDifficultyService.nextDifficulty(session.getCurrentDifficulty(), penaltyPerformance);
        session.setCurrentDifficulty(nextDifficulty);
        sessionRepository.save(session);

        return Map.of(
                "skipped",        true,
                "feedback",       skipFeedback,
                "nextDifficulty", nextDifficulty
        );
    }

    @GetMapping
    public List<Question> listQuestions(@PathVariable Long sessionId) {
        return questionRepository.findBySessionIdOrderByIdAsc(sessionId);
    }

    private InterviewSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
    }
}
