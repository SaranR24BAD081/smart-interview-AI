import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api.js";
import AvatarInterviewer from "../components/AvatarInterviewer.jsx";
import DifficultyBadge from "../components/DifficultyBadge.jsx";
import MicCapture from "../components/MicCapture.jsx";

const MAX_QUESTIONS = 4;

export default function InterviewSession() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [session, setSession] = useState(null);
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [answeredCount, setAnsweredCount] = useState(0);
  const [lastResult, setLastResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [skipping, setSkipping] = useState(false);
  const [error, setError] = useState(null);

  const [camState, setCamState] = useState("idle"); // idle | granted | denied
  const selfViewRef = useRef(null);
  const streamRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    navigator.mediaDevices
      ?.getUserMedia({ video: true, audio: true })
      .then((stream) => {
        if (cancelled) { stream.getTracks().forEach((t) => t.stop()); return; }
        streamRef.current = stream;
        if (selfViewRef.current) selfViewRef.current.srcObject = stream;
        setCamState("granted");
      })
      .catch(() => setCamState("denied"));

    return () => {
      cancelled = true;
      streamRef.current?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const s = await api.getSession(sessionId);
        setSession(s);
        await fetchNextQuestion();
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  const fetchNextQuestion = async () => {
    const type = answeredCount % 2 === 0 ? "BEHAVIORAL" : "TECHNICAL";
    const q = await api.nextQuestion(sessionId, type);
    setCurrentQuestion(q);
    setLastResult(null);
  };

  const handleAnswer = async (transcript, responseTimeSeconds) => {
    if (!transcript || transcript.trim().length === 0) {
      setError("Please provide an answer before submitting.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const words = transcript.trim().split(/\s+/).length;
      const wpm = responseTimeSeconds > 0 ? (words / responseTimeSeconds) * 60 : 120;
      const paceScore = clamp01(1 - Math.abs(wpm - 130) / 130);
      const fillerCount = (transcript.match(/\b(um|uh|like|you know)\b/gi) || []).length;
      const fillerScore = clamp01(1 - fillerCount / Math.max(5, words / 10));

      const result = await api.submitAnswer(sessionId, {
        questionId: currentQuestion.id,
        transcript,
        paceScore,
        pauseScore: 0.75,
        fillerScore,
        pitchScore: 0.75,
        responseTimeSeconds
      });

      setLastResult({ ...result, skipped: false });
      setSession((prev) => ({ ...prev, currentDifficulty: result.nextDifficulty }));

      const nextCount = answeredCount + 1;
      setAnsweredCount(nextCount);
      if (nextCount >= MAX_QUESTIONS) {
        setTimeout(() => navigate(`/coding/${sessionId}`), 2000);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSkip = async () => {
    if (!currentQuestion) return;
    setError(null);
    setSkipping(true);
    try {
      // Fire-and-forget the skip record to the backend
      await api.skipQuestion(sessionId, currentQuestion.id);

      const nextCount = answeredCount + 1;
      setAnsweredCount(nextCount);

      if (nextCount >= MAX_QUESTIONS) {
        navigate(`/coding/${sessionId}`);
      } else {
        // Go straight to next question — no analysis panel shown
        await fetchNextQuestion();
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSkipping(false);
    }
  };

  const handleNext = async () => {
    setLastResult(null);
    await fetchNextQuestion();
  };

  if (loading) return <div className="page-container">Loading session...</div>;

  const isLastQuestion = answeredCount >= MAX_QUESTIONS;

  return (
    <div className="page-container">
      <div className="page-head session-head">
        <div>
          <h1>Interview in progress</h1>
          <p className="subtitle subtitle-tight">
            {session?.jobRole ? `Practicing for ${session.jobRole}` : "Behavioral + technical round"}
          </p>
        </div>
        {session && <DifficultyBadge level={session.currentDifficulty} />}
      </div>

      <QuestionProgress current={answeredCount} total={MAX_QUESTIONS} />

      {error && <div className="alert-error">{error}</div>}

      <div className="interview-grid">
        <AvatarInterviewer
          text={currentQuestion?.questionText}
          selfViewRef={selfViewRef}
          camState={camState}
        />

        <div className="card question-card">
          <p className="question-index">
            Question {Math.min(answeredCount + 1, MAX_QUESTIONS)} of {MAX_QUESTIONS} &middot; {currentQuestion?.type}
          </p>
          <p className="question-text">{currentQuestion?.questionText}</p>

          {/* Answer input — hidden after result is shown */}
          {!lastResult && (
            <>
              <MicCapture onResult={handleAnswer} disabled={!currentQuestion || submitting} />

              <div className="skip-row">
                <button
                  className="btn btn-skip"
                  onClick={handleSkip}
                  disabled={!currentQuestion || skipping || submitting}
                  title="Skip this question"
                >
                  {skipping ? "Skipping…" : "⏭ Skip this question"}
                </button>
                <span className="skip-hint">Skip if you don't know the answer</span>
              </div>
            </>
          )}

          {/* Analysis panel — shown after answer or skip */}
          {lastResult && (
            <AnalysisPanel
              result={lastResult}
              isLast={isLastQuestion}
              onNext={handleNext}
            />
          )}
        </div>
      </div>
    </div>
  );
}

/* ─── Analysis Panel ─────────────────────────────────────────────── */
function AnalysisPanel({ result, isLast, onNext }) {
  const correct = result.answerCorrect;
  const skipped = result.skipped;

  const verdict = skipped
    ? { label: "Skipped", icon: "⏭️", cls: "verdict-skip" }
    : correct
      ? { label: "Correct", icon: "✅", cls: "verdict-correct" }
      : { label: "Needs Improvement", icon: "❌", cls: "verdict-incorrect" };

  return (
    <div className={`analysis-panel ${verdict.cls}`}>
      {/* Verdict banner */}
      <div className="verdict-banner">
        <span className="verdict-icon">{verdict.icon}</span>
        <span className="verdict-label">{verdict.label}</span>
      </div>

      {/* Feedback text */}
      {result.feedback && (
        <p className="analysis-feedback">{result.feedback}</p>
      )}

      {/* Score bars — only for answered (not skipped) */}
      {!skipped && (
        <div className="score-bars">
          <ScoreBar label="Communication" value={result.communicationScore} correct={correct} />
          <ScoreBar label="Confidence"    value={result.confidenceScore}    correct={correct} />
          <ScoreBar label="Performance"   value={result.performanceScore}   correct={correct} />
        </div>
      )}

      {/* Difficulty adjustment note */}
      <p className="difficulty-note">
        Next question difficulty adjusted to{" "}
        <strong className="diff-value">{result.nextDifficulty}</strong>
        {skipped ? " (easier — question was skipped)." : correct ? " (harder — good answer!)." : " (slightly easier)."}
      </p>

      {/* CTA */}
      {!isLast && (
        <button className="btn btn-primary analysis-next-btn" onClick={onNext}>
          Next question →
        </button>
      )}
      {isLast && (
        <p className="hint">Proceeding to the coding round…</p>
      )}
    </div>
  );
}

/* ─── Helper components ──────────────────────────────────────────── */
function QuestionProgress({ current, total }) {
  return (
    <div className="q-progress">
      {Array.from({ length: total }).map((_, i) => (
        <span key={i} className={`q-dot ${i < current ? "done" : i === current ? "current" : ""}`} />
      ))}
    </div>
  );
}

function ScoreBar({ label, value, correct }) {
  const pct = Math.round((value || 0) * 100);
  const fillClass = pct >= 60 ? "score-fill-good" : pct >= 40 ? "score-fill-mid" : "score-fill-low";
  return (
    <div className="score-bar">
      <span className="score-label">{label}: <strong>{pct}%</strong></span>
      <div className="score-track">
        <div className={`score-fill ${fillClass}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function clamp01(v) {
  return Math.max(0, Math.min(1, v));
}
