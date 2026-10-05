package com.aiinterview.controller;

import com.aiinterview.model.InterviewSession;
import com.aiinterview.repository.CodingSubmissionRepository;
import com.aiinterview.repository.InterviewSessionRepository;
import com.aiinterview.repository.QuestionRepository;
import com.aiinterview.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Section III-G: Performance Report Generation.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/report")
public class ReportController {

    private final InterviewSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final ReportService reportService;

    public ReportController(InterviewSessionRepository sessionRepository,
                             QuestionRepository questionRepository,
                             CodingSubmissionRepository codingSubmissionRepository,
                             ReportService reportService) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.codingSubmissionRepository = codingSubmissionRepository;
        this.reportService = reportService;
    }

    @GetMapping
    public Map<String, Object> getReport(@PathVariable Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        session.setStatus(InterviewSession.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        // Fetch via repositories (not session.getQuestions()/getCodingSubmissions(),
        // which are lazy collections and would throw outside a transaction --
        // see the note on ReportService.buildReport).
        var questions = questionRepository.findBySessionIdOrderByIdAsc(sessionId);
        var submissions = codingSubmissionRepository.findBySessionIdOrderByIdAsc(sessionId);

        return reportService.buildReport(session, questions, submissions);
    }
}
