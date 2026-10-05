import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api.js";
import DifficultyBadge from "../components/DifficultyBadge.jsx";

const LANGUAGES = [
  { id: "java",       label: "Java",       icon: "☕" },
  { id: "python",     label: "Python",     icon: "🐍" },
  { id: "c",          label: "C",          icon: "⚙️" },
  { id: "javascript", label: "JavaScript", icon: "🟨" },
];

const MAX_QUESTIONS = 3; // 1st question + 2 more after submit

export default function CodingRound() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [questionIndex, setQuestionIndex] = useState(1);  // 1-based display counter
  const [difficulty, setDifficulty]       = useState(3);
  const [problem, setProblem]             = useState(null);
  const [code, setCode]                   = useState("");
  const [result, setResult]               = useState(null);
  const [error, setError]                 = useState(null);
  const [loading, setLoading]             = useState(true);
  const [submitting, setSubmitting]       = useState(false);
  const [loadingNext, setLoadingNext]     = useState(false);
  const [langSwitching, setLangSwitching] = useState(false);
  const [selectedLang, setSelectedLang]   = useState("javascript");
  const [tab, setTab]                     = useState("description");

  useEffect(() => {
    (async () => {
      try {
        const p = await api.nextCodingProblem(sessionId);
        applyProblem(p);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId]);

  const applyProblem = (p) => {
    setProblem(p);
    setDifficulty(p.difficulty ?? 3);
    setCode(p.starterCode || "");
    setSelectedLang(p.language || "javascript");
    setResult(null);
    setTab("description");
  };

  const handleLanguageChange = async (lang) => {
    if (lang === selectedLang || langSwitching || result) return;
    setError(null);
    setLangSwitching(true);
    try {
      const p = await api.changeLanguage(sessionId, lang);
      applyProblem(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLangSwitching(false);
    }
  };

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const res = await api.submitCode(sessionId, {
        submissionId: problem.id,
        code,
        language: problem.language || selectedLang,
      });
      setResult(res);
      setTab("result");
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleNextQuestion = async () => {
    if (loadingNext) return;
    setError(null);
    setLoadingNext(true);
    try {
      const p = await api.nextCodingProblem(sessionId);
      applyProblem(p);
      setQuestionIndex((prev) => prev + 1);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingNext(false);
    }
  };

  const handleReset = () => setCode(problem?.starterCode || "");

  // Tab key indentation
  const handleTab = (e) => {
    if (e.key !== "Tab") return;
    e.preventDefault();
    const el = e.target;
    const { selectionStart: start, selectionEnd: end } = el;
    const indent = "    ";
    const next = code.slice(0, start) + indent + code.slice(end);
    setCode(next);
    requestAnimationFrame(() => { el.selectionStart = el.selectionEnd = start + indent.length; });
  };

  if (loading) return <div className="page-container">Loading coding problem...</div>;

  const isLastQuestion    = questionIndex >= MAX_QUESTIONS;
  const activeLang        = LANGUAGES.find((l) => l.id === selectedLang) || LANGUAGES[0];
  const passed            = result?.testsPassed ?? 0;
  const total             = result?.testsTotal  ?? 0;
  const scorePct          = result ? Math.round(result.score * 100) : 0;
  const verdict           = !result ? null : scorePct === 100 ? "solid" : scorePct >= 50 ? "partial" : "weak";
  const verdictLabel      = { solid: "✅ Looks solid", partial: "🟡 Partially there", weak: "❌ Needs another pass" }[verdict];

  return (
    <div className="lc-page">
      <div className="page-head session-head lc-head">
        <div>
          <h1>Coding round</h1>
          <p className="subtitle subtitle-tight">Solve the prompt, then submit for review.</p>
        </div>
        {/* Question progress indicator */}
        <div className="coding-progress">
          {Array.from({ length: MAX_QUESTIONS }, (_, i) => (
            <div
              key={i}
              className={`coding-progress-dot ${
                i + 1 < questionIndex ? "done" :
                i + 1 === questionIndex ? "active" : ""
              }`}
              title={`Question ${i + 1}`}
            />
          ))}
          <span className="coding-progress-label">Q {questionIndex} / {MAX_QUESTIONS}</span>
        </div>
      </div>
      {error && <div className="alert-error">{error}</div>}

      <div className="lc-layout">
        {/* ── Left: problem description ── */}
        <div className="lc-pane lc-problem-pane">
          <div className="lc-tabs">
            <button
              className={`lc-tab ${tab === "description" ? "active" : ""}`}
              onClick={() => setTab("description")}
            >
              Description
            </button>
            <button
              className={`lc-tab ${tab === "result" ? "active" : ""}`}
              onClick={() => result && setTab("result")}
              disabled={!result}
            >
              Result {result && <span className={`lc-result-badge ${verdict}`}>{scorePct}%</span>}
            </button>
          </div>

          {tab === "description" && (
            <div className="lc-pane-body">
              <div className="lc-title-row">
                <h2 className="lc-title">{problem?.title || "Problem"}</h2>
                <DifficultyBadge level={difficulty} />
              </div>
              <p className="problem-text">{problem?.problemStatement}</p>

              {(problem?.examples || []).map((ex, i) => (
                <div className="example-block" key={i}>
                  <div className="example-label">Example {i + 1}:</div>
                  <pre className="example-io"><strong>Input:</strong>  {ex.input}</pre>
                  <pre className="example-io"><strong>Output:</strong> {ex.output}</pre>
                </div>
              ))}

              {problem?.constraints?.length > 0 && (
                <div className="constraints-block">
                  <div className="example-label">Constraints:</div>
                  <ul className="constraints-list">
                    {problem.constraints.map((c, i) => <li key={i}><code>{c}</code></li>)}
                  </ul>
                </div>
              )}
            </div>
          )}

          {tab === "result" && result && (
            <div className="lc-pane-body">
              <div className={`lc-verdict lc-verdict-${verdict}`}>
                <span className="lc-verdict-dot" />
                {verdictLabel}
              </div>
              <p className="lc-verdict-sub">
                {passed} / {total} checks passed &middot; <strong>{scorePct}%</strong>
              </p>
              <p className="hint lc-review-note">
                This is a structural review of your code (does it use the right building blocks),
                not a real execution against test cases.
              </p>
              <ul className="check-list">
                {(result.checks || []).map((c, i) => (
                  <li className={`check-item ${c.passed ? "pass" : "fail"}`} key={i}>
                    <span className="check-icon">{c.passed ? "✓" : "✕"}</span>
                    {c.label}
                  </li>
                ))}
              </ul>

              {/* Navigation after submit */}
              <div className="lc-after-submit">
                {isLastQuestion ? (
                  <button
                    className="btn btn-primary lc-report-btn"
                    onClick={() => navigate(`/report/${sessionId}`)}
                  >
                    View final performance report →
                  </button>
                ) : (
                  <button
                    className="btn btn-primary lc-next-btn"
                    onClick={handleNextQuestion}
                    disabled={loadingNext}
                  >
                    {loadingNext ? "Loading…" : `Next question (${questionIndex + 1}/${MAX_QUESTIONS}) →`}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        {/* ── Right: editor ── */}
        <div className="lc-pane lc-editor-pane">
          {/* Language selector — disabled after submit */}
          <div className="lang-selector-bar">
            <span className="lang-selector-label">Language:</span>
            <div className="lang-pills">
              {LANGUAGES.map((lang) => (
                <button
                  key={lang.id}
                  className={`lang-pill-btn ${selectedLang === lang.id ? "active" : ""}`}
                  onClick={() => handleLanguageChange(lang.id)}
                  disabled={langSwitching || submitting || !!result}
                  title={`Switch to ${lang.label}`}
                >
                  <span className="lang-pill-icon">{lang.icon}</span>
                  {lang.label}
                </button>
              ))}
            </div>
            {langSwitching && <span className="lang-switching-hint">Switching…</span>}
          </div>

          {/* Editor toolbar */}
          <div className="editor-toolbar">
            <h3 className="card-title">Code editor</h3>
            <span className="lang-pill">{activeLang.icon} {activeLang.label}</span>
          </div>

          <textarea
            className="code-editor lc-code-editor"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            onKeyDown={handleTab}
            spellCheck={false}
            readOnly={!!result}
            placeholder={langSwitching ? "Loading starter code…" : ""}
          />

          <div className="lc-editor-actions">
            <button
              className="btn btn-secondary"
              onClick={handleReset}
              disabled={submitting || langSwitching || !!result}
            >
              Reset
            </button>
            {!result ? (
              <button
                className="btn btn-primary"
                onClick={handleSubmit}
                disabled={submitting || langSwitching}
              >
                {submitting ? "Submitting…" : "Submit"}
              </button>
            ) : isLastQuestion ? (
              <button
                className="btn btn-primary"
                onClick={() => navigate(`/report/${sessionId}`)}
              >
                Finish →
              </button>
            ) : (
              <button
                className="btn btn-primary"
                onClick={handleNextQuestion}
                disabled={loadingNext}
              >
                {loadingNext ? "Loading…" : "Next question →"}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
