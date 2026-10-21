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
  const [error, setError] = useState(null);

  // Camera/mic for the live round. If the candidate already granted access
  // on the setup screen the browser won't prompt again — this just attaches
  // a fresh stream to the video element here.
  const [camState, setCamState] = useState("idle"); // idle | granted | denied
  const selfViewRef = useRef(null);
  const streamRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    navigator.mediaDevices
      ?.getUserMedia({ video: true, audio: true })
      .then((stream) => {
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
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

      setLastResult(result);
      setSession((prev) => ({ ...prev, currentDifficulty: result.nextDifficulty }));

      const nextCount = answeredCount + 1;
      setAnsweredCount(nextCount);

      if (nextCount >= MAX_QUESTIONS) {
        setTimeout(() => navigate(`/coding/${sessionId}`), 1200);
      }
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) return <div className="page-container">Loading session...</div>;

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
            Question {answeredCount + 1} of {MAX_QUESTIONS} &middot; {currentQuestion?.type}
          </p>
          <p className="question-text">{currentQuestion?.questionText}</p>

          <MicCapture onResult={handleAnswer} disabled={!currentQuestion} />

          {lastResult && (
            <div className="result-panel">
              <h3>Answer analysis</h3>
              <ScoreBar label="Communication" value={lastResult.communicationScore} />
              <ScoreBar label="Confidence" value={lastResult.confidenceScore} />
              <ScoreBar label="Overall performance" value={lastResult.performanceScore} />
              <p className="hint">
                Next question difficulty adjusted to{" "}
                <strong>{lastResult.nextDifficulty}</strong> based on this answer.
              </p>
              {answeredCount < MAX_QUESTIONS && (
                <button className="btn btn-primary" onClick={fetchNextQuestion}>
                  Next question
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function QuestionProgress({ current, total }) {
  return (
    <div className="q-progress">
      {Array.from({ length: total }).map((_, i) => (
        <span key={i} className={`q-dot ${i < current ? "done" : i === current ? "current" : ""}`} />
      ))}
    </div>
  );
}

function ScoreBar({ label, value }) {
  const pct = Math.round((value || 0) * 100);
  return (
    <div className="score-bar">
      <span className="score-label">{label}: {pct}%</span>
      <div className="score-track">
        <div className="score-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function clamp01(v) {
  return Math.max(0, Math.min(1, v));
}
