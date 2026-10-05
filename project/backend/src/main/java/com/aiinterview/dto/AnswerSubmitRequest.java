package com.aiinterview.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload sent by the frontend after Web Speech API STT has transcribed
 * the candidate's spoken answer, together with basic paralinguistic
 * features measured on the client (Section III-D of the paper).
 */
@Getter
@Setter
public class AnswerSubmitRequest {

    private Long questionId;

    private String transcript;

    /** Speaking pace, normalized 0-1 (1 = ideal pace). */
    private double paceScore = 0.7;

    /** Pause pattern quality, normalized 0-1 (1 = few awkward pauses). */
    private double pauseScore = 0.7;

    /** Filler-word measure, normalized 0-1 (1 = few filler words). */
    private double fillerScore = 0.7;

    /** Pitch variation / vocal energy, normalized 0-1. */
    private double pitchScore = 0.7;

    /** Response time in seconds, used only for reporting. */
    private double responseTimeSeconds;
}
