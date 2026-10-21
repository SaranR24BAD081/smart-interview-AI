package com.aiinterview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    @JsonIgnore
    private InterviewSession session;

    @Lob
    private String questionText;

    @Enumerated(EnumType.STRING)
    private QuestionType type = QuestionType.BEHAVIORAL;

    /** Difficulty this question was generated at (1-5). */
    private int difficulty;

    /** Candidate's transcribed spoken answer (from STT). */
    @Lob
    private String answerTranscript;

    /** Communication score in [0,1] from Answer Analysis Engine. */
    private Double communicationScore;

    /** Confidence score in [0,1], computed via Eq. 2. */
    private Double confidenceScore;

    /** Overall performance score p_t in [0,1] used to drive Eq. 3. */
    private Double performanceScore;

    public enum QuestionType {
        BEHAVIORAL, TECHNICAL, CODING
    }
}
