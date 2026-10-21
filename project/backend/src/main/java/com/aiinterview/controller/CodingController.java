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

import java.util.*;

/**
 * Section III-F: Coding Question Generator + Code Assessment Controller.
 * Supports multiple programming languages (Java, Python, C, C++, JavaScript),
 * visible and hidden test case evaluation, syntax/error checking, and LeetCode-style diagnostics.
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

    public static final int CODING_PROBLEMS_COUNT = 3;

    /**
     * Retrieves all 3 coding problems for this session, generating them if not already created.
     */
    @GetMapping("/problems")
    public List<CodingSubmission> getProblems(@PathVariable Long sessionId) {
        InterviewSession session = getSession(sessionId);
        List<CodingSubmission> existing = submissionRepository.findBySessionIdOrderByIdAsc(sessionId);

        if (existing.size() >= CODING_PROBLEMS_COUNT) {
            return existing;
        }

        int needed = CODING_PROBLEMS_COUNT - existing.size();
        Set<String> usedConcepts = new HashSet<>();
        for (CodingSubmission s : existing) {
            if (s.getConceptName() != null && !s.getConceptName().isBlank()) {
                usedConcepts.add(s.getConceptName());
            }
        }

        List<String> rankedSkills = resumeAnalyzerService.rankedSkillsForRole(
                session.getResumeText(), session.getJobRole());

        List<CodingQuestionService.Problem> generated = codingQuestionService.generateProblems(
                session.getCurrentDifficulty(), session.getJobRole(), rankedSkills, needed, usedConcepts);

        List<CodingSubmission> saved = new ArrayList<>(existing);
        for (CodingQuestionService.Problem problem : generated) {
            CodingSubmission submission = createSubmissionFromProblem(session, problem);
            saved.add(submissionRepository.save(submission));
        }

        session.setStatus(InterviewSession.SessionStatus.CODING_ROUND);
        sessionRepository.save(session);

        return saved;
    }

    /** Returns the next unsubmitted problem, or the first problem. */
    @PostMapping("/next")
    public CodingSubmission nextProblem(@PathVariable Long sessionId) {
        List<CodingSubmission> problems = getProblems(sessionId);
        for (CodingSubmission s : problems) {
            if (s.getScore() == null) {
                return s;
            }
        }
        return problems.get(0);
    }

    private CodingSubmission createSubmissionFromProblem(InterviewSession session, CodingQuestionService.Problem problem) {
        CodingSubmission submission = new CodingSubmission();
        submission.setSession(session);
        submission.setDifficulty(problem.difficulty());
        submission.setTitle(problem.title());
        submission.setConceptName(problem.conceptName());
        submission.setProblemStatement(problem.statement());
        submission.setExamplesJson(toJson(problem.examples()));
        submission.setConstraintsJson(toJson(problem.constraints()));
        submission.setStartersJson(toJson(problem.starters()));
        submission.setTestCasesJson(toJson(problem.testCases()));
        submission.setExpectedTokensCsv(String.join(",", problem.requiredTokens()));
        submission.setCheckLabelsJson(toJson(problem.checkLabels()));
        submission.setLanguage(problem.language());
        submission.setStarterCode(problem.starterCode());
        return submission;
    }

    /** Runs the candidate's code against visible test cases only (LeetCode 'Run' button). */
    @PostMapping("/run")
    public Map<String, Object> runCode(@PathVariable Long sessionId, @RequestBody CodeSubmitRequest req) {
        CodingSubmission submission = submissionRepository.findById(req.getSubmissionId())
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + req.getSubmissionId()));

        List<String> expectedTokens = (submission.getExpectedTokensCsv() == null || submission.getExpectedTokensCsv().isBlank())
                ? codingQuestionService.generateProblem(submission.getDifficulty(), null).requiredTokens()
                : Arrays.asList(submission.getExpectedTokensCsv().split(","));
        List<String> checkLabels = fromJsonList(submission.getCheckLabelsJson());

        String lang = req.getLanguage() != null && !req.getLanguage().isBlank() ? req.getLanguage() : submission.getLanguage();

        CodeEvaluationService.EvaluationResult result = codeEvaluationService.evaluateDetailed(
                req.getCode(),
                lang,
                submission.getStarterCode(),
                submission.getConceptName(),
                submission.getTestCases(),
                expectedTokens,
                checkLabels,
                false // isSubmit = false: run only visible test cases
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", result.status);
        response.put("testsPassed", result.testsPassed);
        response.put("testsTotal", result.testsTotal);
        response.put("score", result.score);
        response.put("runtimeMs", result.runtimeMs);
        response.put("memoryMb", result.memoryMb);
        response.put("errorMessage", result.errorMessage);
        response.put("testResults", result.testResults);
        response.put("checks", result.checks);
        return response;
    }

    /** Evaluates submitted code against all test cases including hidden test cases (LeetCode 'Submit' button). */
    @PostMapping("/submit")
    public Map<String, Object> submitCode(@PathVariable Long sessionId, @RequestBody CodeSubmitRequest req) {
        CodingSubmission submission = submissionRepository.findById(req.getSubmissionId())
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + req.getSubmissionId()));

        List<String> expectedTokens = (submission.getExpectedTokensCsv() == null || submission.getExpectedTokensCsv().isBlank())
                ? codingQuestionService.generateProblem(submission.getDifficulty(), null).requiredTokens()
                : Arrays.asList(submission.getExpectedTokensCsv().split(","));
        List<String> checkLabels = fromJsonList(submission.getCheckLabelsJson());

        String lang = req.getLanguage() != null && !req.getLanguage().isBlank() ? req.getLanguage() : submission.getLanguage();

        CodeEvaluationService.EvaluationResult result = codeEvaluationService.evaluateDetailed(
                req.getCode(),
                lang,
                submission.getStarterCode(),
                submission.getConceptName(),
                submission.getTestCases(),
                expectedTokens,
                checkLabels,
                true // isSubmit = true: run visible + hidden test cases
        );

        submission.setCandidateCode(req.getCode());
        submission.setLanguage(lang);
        submission.setTestsPassed(result.testsPassed);
        submission.setTestsTotal(result.testsTotal);
        submission.setScore(result.score);
        submissionRepository.save(submission);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", result.status);
        response.put("testsPassed", result.testsPassed);
        response.put("testsTotal", result.testsTotal);
        response.put("score", result.score);
        response.put("runtimeMs", result.runtimeMs);
        response.put("memoryMb", result.memoryMb);
        response.put("errorMessage", result.errorMessage);
        response.put("testResults", result.testResults);
        response.put("checks", result.checks);
        return response;
    }

    @GetMapping
    public List<CodingSubmission> listSubmissions(@PathVariable Long sessionId) {
        return getProblems(sessionId);
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
