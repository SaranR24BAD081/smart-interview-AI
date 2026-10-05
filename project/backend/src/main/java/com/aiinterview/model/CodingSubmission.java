package com.aiinterview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "coding_submissions")
@Getter
@Setter
public class CodingSubmission {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private InterviewSession session;

    private String title;

    @Lob
    private String problemStatement;

    /** JSON array of {"input": "...", "output": "..."}, rendered as the LeetCode-style Examples section. */
    @JsonIgnore
    @Lob
    private String examplesJson;

    /** JSON array of constraint strings (e.g. "1 <= arr.length <= 10^4"). */
    @JsonIgnore
    @Lob
    private String constraintsJson;

    /**
     * Comma-separated tokens CodeEvaluationService checks the candidate's code
     * for. Stored per-submission (rather than re-derived from difficulty)
     * because the problem is now chosen per-candidate from the resume, not
     * purely by difficulty level. Not shown to the candidate.
     */
    @JsonIgnore
    private String expectedTokensCsv;

    /** JSON array of human-readable labels, same order as expectedTokensCsv, for the Result panel. */
    @JsonIgnore
    @Lob
    private String checkLabelsJson;

    private int difficulty;

    /** Starter code shown in the editor (signature stub in the problem's language). */
    @Lob
    private String starterCode;

    @Lob
    private String candidateCode;

    /** javascript | java | python -- chosen from the job role / resume. */
    private String language = "javascript";

    private int testsPassed;

    private int testsTotal;

    /** testsPassed / testsTotal, in [0,1]. */
    private Double score;

    // ---- Derived, read-only JSON views for the frontend (kept separate from
    // the raw *Json storage fields above so Jackson serializes real arrays
    // instead of escaped JSON strings). ----

    public List<Map<String, String>> getExamples() {
        return parseList(examplesJson, new TypeReference<>() {});
    }

    public List<String> getConstraints() {
        return parseList(constraintsJson, new TypeReference<>() {});
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return JSON.readValue(json, type);
        } catch (Exception e) {
            return List.of();
        }
    }
}
