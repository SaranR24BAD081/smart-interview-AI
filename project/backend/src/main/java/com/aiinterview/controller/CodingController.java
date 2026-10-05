package com.aiinterview.controller;

import com.aiinterview.dto.CodeSubmitRequest;
import com.aiinterview.model.CodingSubmission;
import com.aiinterview.model.InterviewSession;
import com.aiinterview.repository.CodingSubmissionRepository;
import com.aiinterview.repository.InterviewSessionRepository;
import com.aiinterview.service.CodeEvaluationService;
import com.aiinterview.service.CodingQuestionService;
import com.aiinterview.service.ResumeAnalyzerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Section III-F: Coding Question Generator + Code Editor (backend half --
 * problem generation and automated assessment; the editor UI itself is a
 * LeetCode-style split view in frontend/src/pages/CodingRound.jsx).
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/coding")
public class CodingController {

    private final InterviewSessionRepository sessionRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final CodingQuestionService codingQuestionService;
    private final CodeEvaluationService codeEvaluationService;
    private final ResumeAnalyzerService resumeAnalyzerService;
    private final ObjectMapper objectMapper;

    public CodingController(InterviewSessionRepository sessionRepository,
                             CodingSubmissionRepository submissionRepository,
                             CodingQuestionService codingQuestionService,
                             CodeEvaluationService codeEvaluationService,
                             ResumeAnalyzerService resumeAnalyzerService,
                             ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.submissionRepository = submissionRepository;
        this.codingQuestionService = codingQuestionService;
        this.codeEvaluationService = codeEvaluationService;
        this.resumeAnalyzerService = resumeAnalyzerService;
        this.objectMapper = objectMapper;
    }

    /** Generates a new coding problem, matched to the candidate's resume, at the session's current adaptive difficulty.
     *  Already-used question titles for this session are excluded so every question is unique. */
    @PostMapping("/next")
    public CodingSubmission nextProblem(@PathVariable Long sessionId) {
        InterviewSession session = getSession(sessionId);

        // Collect titles of questions already served in this session
        List<CodingSubmission> existing = submissionRepository.findBySessionIdOrderByIdAsc(sessionId);
        Set<String> usedTitles = new HashSet<>();
        for (CodingSubmission s : existing) {
            if (s.getTitle() != null) usedTitles.add(s.getTitle());
        }

        List<String> rankedSkills = resumeAnalyzerService.rankedSkillsForRole(
                session.getResumeText(), session.getJobRole());
        CodingQuestionService.Problem problem =
                codingQuestionService.generateProblem(
                        session.getCurrentDifficulty(), session.getJobRole(),
                        rankedSkills, usedTitles);

        CodingSubmission submission = new CodingSubmission();
        submission.setSession(session);
        submission.setDifficulty(session.getCurrentDifficulty());
        submission.setTitle(problem.title());
        submission.setProblemStatement(problem.statement());
        submission.setExamplesJson(toJson(problem.examples()));
        submission.setConstraintsJson(toJson(problem.constraints()));
        submission.setExpectedTokensCsv(String.join(",", problem.requiredTokens()));
        submission.setCheckLabelsJson(toJson(problem.checkLabels()));
        submission.setLanguage(problem.language());
        submission.setStarterCode(problem.starterCode());

        // Note: we deliberately do NOT touch session.getCodingSubmissions() here.
        // That collection is LAZY and open-in-view is disabled, so reading/adding
        // to it outside a transaction throws "could not initialize proxy - no
        // Session". submission.setSession(session) above already sets the FK;
        // saving the submission directly is all persistence needs.
        session.setStatus(InterviewSession.SessionStatus.CODING_ROUND);
        sessionRepository.save(session);

        return submissionRepository.save(submission);
    }

    /** Evaluates submitted code against the expected building blocks for its problem. */
    @PostMapping("/submit")
    public Map<String, Object> submitCode(@PathVariable Long sessionId, @RequestBody CodeSubmitRequest req) {
        CodingSubmission submission = submissionRepository.findById(req.getSubmissionId())
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + req.getSubmissionId()));

        // Back-compat for submissions created before these fields existed.
        List<String> expectedTokens = (submission.getExpectedTokensCsv() == null || submission.getExpectedTokensCsv().isBlank())
                ? codingQuestionService.generateProblem(submission.getDifficulty(), null).requiredTokens()
                : Arrays.asList(submission.getExpectedTokensCsv().split(","));
        List<String> checkLabels = fromJsonList(submission.getCheckLabelsJson());

        CodeEvaluationService.EvaluationResult result = codeEvaluationService.evaluate(
                codeEvaluationService.stripStarter(req.getCode(), submission.getStarterCode()),
                expectedTokens, checkLabels);

        submission.setCandidateCode(req.getCode());
        // language stays as chosen when the problem was generated (java/python/javascript)
        submission.setTestsPassed(result.testsPassed);
        submission.setTestsTotal(result.testsTotal);
        submission.setScore(result.score);
        submissionRepository.save(submission);

        return Map.of(
                "testsPassed", result.testsPassed,
                "testsTotal", result.testsTotal,
                "score", result.score,
                "checks", result.checks
        );
    }

    /**
     * Regenerates the current coding problem in the given language
     * (java | python | javascript | c). Called when the candidate
     * switches language from the frontend language selector.
     */
    @PostMapping("/language")
    public CodingSubmission changeLanguage(@PathVariable Long sessionId,
                                           @RequestBody Map<String, String> body) {
        InterviewSession session = getSession(sessionId);
        String language = body.getOrDefault("language", "javascript").toLowerCase();

        // Validate
        if (!List.of("java", "python", "javascript", "c").contains(language)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        List<String> rankedSkills = resumeAnalyzerService.rankedSkillsForRole(
                session.getResumeText(), session.getJobRole());

        // Force the desired language by passing it as the first ranked skill
        // so detectLanguage() picks it up immediately.
        List<String> skillsWithLang = new ArrayList<>();
        skillsWithLang.add(language);
        skillsWithLang.addAll(rankedSkills);

        CodingQuestionService.Problem problem =
                codingQuestionService.generateProblem(session.getCurrentDifficulty(), language, skillsWithLang);

        CodingSubmission submission = new CodingSubmission();
        submission.setSession(session);
        submission.setDifficulty(session.getCurrentDifficulty());
        submission.setTitle(problem.title());
        submission.setProblemStatement(problem.statement());
        submission.setExamplesJson(toJson(problem.examples()));
        submission.setConstraintsJson(toJson(problem.constraints()));
        submission.setExpectedTokensCsv(String.join(",", problem.requiredTokens()));
        submission.setCheckLabelsJson(toJson(problem.checkLabels()));
        submission.setLanguage(language);
        submission.setStarterCode(problem.starterCode());
        session.setStatus(InterviewSession.SessionStatus.CODING_ROUND);
        sessionRepository.save(session);
        return submissionRepository.save(submission);
    }

    @GetMapping
    public List<CodingSubmission> listSubmissions(@PathVariable Long sessionId) {
        return submissionRepository.findBySessionIdOrderByIdAsc(sessionId);
    }

    private InterviewSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
