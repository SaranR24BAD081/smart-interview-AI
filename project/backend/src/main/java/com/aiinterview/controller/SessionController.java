package com.aiinterview.controller;

import com.aiinterview.dto.StartSessionRequest;
import com.aiinterview.model.InterviewSession;
import com.aiinterview.repository.InterviewSessionRepository;
import com.aiinterview.service.ResumeAnalyzerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sections III-A: Resume Analyzer + session lifecycle management.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final InterviewSessionRepository sessionRepository;
    private final ResumeAnalyzerService resumeAnalyzerService;

    public SessionController(InterviewSessionRepository sessionRepository,
                              ResumeAnalyzerService resumeAnalyzerService) {
        this.sessionRepository = sessionRepository;
        this.resumeAnalyzerService = resumeAnalyzerService;
    }

    /** Create a session from an uploaded resume + selected job role. */
    @PostMapping
    public InterviewSession startSession(@RequestBody StartSessionRequest req) {
        InterviewSession session = new InterviewSession();
        session.setCandidateName(req.getCandidateName());
        session.setJobRole(req.getJobRole());
        session.setResumeText(req.getResumeText());
        session.setStatus(InterviewSession.SessionStatus.RESUME_ANALYZED);
        session.setCurrentDifficulty(3); // start at medium difficulty
        return sessionRepository.save(session);
    }

    /** Eq. 1: resume-to-role relevance score + ranked skills, for display/debugging. */
    @GetMapping("/{id}/resume-analysis")
    public Map<String, Object> resumeAnalysis(@PathVariable Long id) {
        InterviewSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));

        Set<String> skills = resumeAnalyzerService.extractSkills(session.getResumeText());
        List<String> ranked = resumeAnalyzerService.rankedSkillsForRole(session.getResumeText(), session.getJobRole());
        double relevance = resumeAnalyzerService.relevanceScore(
                session.getResumeText(), session.getJobRole(), String.join(" ", skills));

        return Map.of(
                "extractedSkills", skills,
                "rankedSkillsForRole", ranked,
                "relevanceScore", relevance
        );
    }

    @GetMapping("/{id}")
    public InterviewSession getSession(@PathVariable Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
    }
}
