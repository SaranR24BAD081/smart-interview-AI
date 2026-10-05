package com.aiinterview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one candidate's end-to-end practice session:
 * resume upload -> job role -> generated questions -> answers -> coding
 * round -> final performance report.
 */
@Entity
@Table(name = "interview_sessions")
@Getter
@Setter
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;

    private String jobRole;

    @Lob
    private String resumeText;

    /** Current adaptive difficulty level, 1 (easiest) - 5 (hardest). See Eq. 3. */
    private int currentDifficulty = 3;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.CREATED;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Lazy collections are intentionally excluded from JSON: GET /api/sessions/{id}
    // is called outside an open Hibernate session (no @Transactional on the
    // controller), so letting Jackson touch these proxies throws
    // "could not initialize proxy - no Session". The frontend already fetches
    // questions via GET /api/sessions/{id}/questions instead.
    @JsonIgnore
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CodingSubmission> codingSubmissions = new ArrayList<>();

    public enum SessionStatus {
        CREATED, RESUME_ANALYZED, IN_PROGRESS, CODING_ROUND, COMPLETED
    }
}
