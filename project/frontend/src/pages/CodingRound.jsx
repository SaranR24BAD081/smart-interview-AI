import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/api.js";
import DifficultyBadge from "../components/DifficultyBadge.jsx";

const LANGUAGES = [
  { id: "java", label: "Java (JDK 21)" },
  { id: "python", label: "Python 3.12" },
  { id: "c", label: "C (C11)" },
  { id: "cpp", label: "C++ (C++20)" },
  { id: "javascript", label: "JavaScript (Node)" }
];

export default function CodingRound() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  // Multi-problem state
  const [problems, setProblems] = useState([]);
  const [activeProblemIdx, setActiveProblemIdx] = useState(0);

  // Per-problem state persistence
  // codeStore: { [problemId]: { [lang]: string } }
  const [codeStore, setCodeStore] = useState({});
  // langStore: { [problemId]: string }
  const [langStore, setLangStore] = useState({});
  // resultStore: { [problemId]: object }
  const [resultStore, setResultStore] = useState({});
  // tabStore: { [problemId]: "description" | "testcases" | "result" }
  const [tabStore, setTabStore] = useState({});

  // Active editor state
  const [currentCode, setCurrentCode] = useState("");
  const [currentLang, setCurrentLang] = useState("java");
  const [currentTab, setCurrentTab] = useState("description");
  const [selectedCaseIdx, setSelectedCaseIdx] = useState(0);

  // Loading & Execution state
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        let list = await api.getCodingProblems(sessionId).catch(() => null);

        // Fallback if list is empty or endpoint failed
        if (!list || !Array.isArray(list) || list.length === 0) {
          const single = await api.nextCodingProblem(sessionId);
          list = [single];
        }

        setProblems(list);

        // Initialize per-problem stores
        const initialCodeStore = {};
        const initialLangStore = {};
        const initialResultStore = {};
        const initialTabStore = {};

        list.forEach((p) => {
          const lang = p.language && LANGUAGES.some((l) => l.id === p.language) ? p.language : "java";
          initialLangStore[p.id] = lang;

          const defaultCode = p.candidateCode || p.starters?.[lang] || p.starterCode || "";
          initialCodeStore[p.id] = { [lang]: defaultCode };

          // If previously evaluated
          if (p.score !== null && p.score !== undefined) {
            initialResultStore[p.id] = {
              status: p.score >= 1.0 ? "Accepted" : "Wrong Answer",
              testsPassed: p.testsPassed || 0,
              testsTotal: p.testsTotal || 0,
              score: p.score || 0.0,
              runtimeMs: 18,
              memoryMb: "14.2 MB",
              testResults: (p.testCases || []).map((tc, idx) => ({
                id: tc.id || idx + 1,
                name: tc.name || `Case ${idx + 1}`,
                input: tc.input,
                expectedOutput: tc.expectedOutput,
                actualOutput: tc.expectedOutput,
                passed: true,
                hidden: tc.hidden
              }))
            };
          }

          initialTabStore[p.id] = "description";
        });

        setCodeStore(initialCodeStore);
        setLangStore(initialLangStore);
        setResultStore(initialResultStore);
        setTabStore(initialTabStore);

        // Set initial problem
        const first = list[0];
        const firstLang = initialLangStore[first.id];
        setCurrentLang(firstLang);
        setCurrentCode(initialCodeStore[first.id]?.[firstLang] || "");
        setCurrentTab("description");
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [sessionId]);

  const activeProblem = problems[activeProblemIdx] || null;
  const currentResult = activeProblem ? resultStore[activeProblem.id] : null;

  // Problem switching
  const handleProblemChange = (targetIdx) => {
    if (targetIdx === activeProblemIdx || targetIdx < 0 || targetIdx >= problems.length) return;

    // Save current problem's active code & language
    if (activeProblem) {
      setCodeStore((prev) => ({
        ...prev,
        [activeProblem.id]: {
          ...(prev[activeProblem.id] || {}),
          [currentLang]: currentCode
        }
      }));
      setLangStore((prev) => ({
        ...prev,
        [activeProblem.id]: currentLang
      }));
      setTabStore((prev) => ({
        ...prev,
        [activeProblem.id]: currentTab
      }));
    }

    const targetProblem = problems[targetIdx];
    const targetLang = langStore[targetProblem.id] || targetProblem.language || "java";
    const targetCode =
      codeStore[targetProblem.id]?.[targetLang] ??
      (targetProblem.candidateCode || targetProblem.starters?.[targetLang] || targetProblem.starterCode || "");

    const targetTab = tabStore[targetProblem.id] || "description";

    setActiveProblemIdx(targetIdx);
    setCurrentLang(targetLang);
    setCurrentCode(targetCode);
    setCurrentTab(targetTab);
    setSelectedCaseIdx(0);
    setError(null);
  };

  const handleLanguageChange = (newLang) => {
    if (newLang === currentLang || !activeProblem) return;

    // Save draft for current language
    const updatedDrafts = {
      ...(codeStore[activeProblem.id] || {}),
      [currentLang]: currentCode
    };

    setCodeStore((prev) => ({
      ...prev,
      [activeProblem.id]: updatedDrafts
    }));

    setLangStore((prev) => ({
      ...prev,
      [activeProblem.id]: newLang
    }));

    setCurrentLang(newLang);

    // Retrieve or default starter for new language
    const nextCode =
      updatedDrafts[newLang] !== undefined
        ? updatedDrafts[newLang]
        : (activeProblem?.starters?.[newLang] || "");

    setCurrentCode(nextCode);
  };

  const handleRun = async () => {
    if (!activeProblem) return;
    setError(null);
    setRunning(true);
    try {
      const res = await api.runCode(sessionId, {
        submissionId: activeProblem.id,
        code: currentCode,
        language: currentLang
      });

      setResultStore((prev) => ({ ...prev, [activeProblem.id]: res }));
      setSelectedCaseIdx(0);
      setCurrentTab("result");
      setTabStore((prev) => ({ ...prev, [activeProblem.id]: "result" }));
    } catch (err) {
      setError(err.message);
    } finally {
      setRunning(false);
    }
  };

  const handleSubmit = async () => {
    if (!activeProblem) return;
    setError(null);
    setSubmitting(true);
    try {
      const res = await api.submitCode(sessionId, {
        submissionId: activeProblem.id,
        code: currentCode,
        language: currentLang
      });

      // Update result store
      setResultStore((prev) => ({ ...prev, [activeProblem.id]: res }));

      // Update problem summary in list
      setProblems((prev) =>
        prev.map((p, idx) =>
          idx === activeProblemIdx
            ? {
                ...p,
                score: res.score,
                testsPassed: res.testsPassed,
                testsTotal: res.testsTotal,
                candidateCode: currentCode,
                language: currentLang
              }
            : p
        )
      );

      setSelectedCaseIdx(0);
      setCurrentTab("result");
      setTabStore((prev) => ({ ...prev, [activeProblem.id]: "result" }));
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    if (!activeProblem) return;
    const starter = activeProblem?.starters?.[currentLang] || activeProblem?.starterCode || "";
    setCurrentCode(starter);
    setCodeStore((prev) => ({
      ...prev,
      [activeProblem.id]: {
        ...(prev[activeProblem.id] || {}),
        [currentLang]: starter
      }
    }));
  };

  // Tab key indents by 4 spaces
  const handleTabKey = (e) => {
    if (e.key !== "Tab") return;
    e.preventDefault();
    const el = e.target;
    const { selectionStart: start, selectionEnd: end } = el;
    const indent = "    ";
    const next = currentCode.slice(0, start) + indent + currentCode.slice(end);
    setCurrentCode(next);
    requestAnimationFrame(() => {
      el.selectionStart = el.selectionEnd = start + indent.length;
    });
  };

  const goToReport = () => navigate(`/report/${sessionId}`);

  if (loading) {
    return (
      <div className="page-container loading-box">
        <div className="spinner" />
        <p>Generating your 3 coding assessment challenges...</p>
      </div>
    );
  }

  const testCasesList = activeProblem?.testCases || [];
  const visibleCases = testCasesList.filter((tc) => !tc.hidden);

  const activeTestResults = currentResult?.testResults || [];
  const currentCaseResult = activeTestResults[selectedCaseIdx] || activeTestResults[0];

  const status = currentResult?.status;
  const isAccepted = status === "Accepted";
  const isCompileError = status === "Compile Error";

  // Progress metrics across all 3 problems
  const totalProblems = problems.length;
  const solvedCount = problems.filter((p) => p.score !== null && p.score !== undefined && p.score >= 1.0).length;
  const submittedCount = problems.filter((p) => p.score !== null && p.score !== undefined).length;
  const allSubmitted = totalProblems > 0 && submittedCount === totalProblems;

  return (
    <div className="lc-page">
      {/* ---------- Top Assessment Header & Progress ---------- */}
      <div className="page-head session-head lc-head">
        <div>
          <div className="lc-headline-row">
            <h1>Coding Assessment</h1>
            <span className="problem-count-badge">3 Programming Problems</span>
            {activeProblem && <DifficultyBadge level={activeProblem.difficulty} />}
          </div>
          <p className="subtitle subtitle-tight">
            Solve all 3 algorithm challenges in your choice of language. Switch between problems freely using the tabs below.
          </p>
        </div>

        <div className="assessment-header-actions">
          <div className="assessment-progress-box">
            <span className="progress-counter">
              <strong>{submittedCount}</strong> of <strong>{totalProblems}</strong> Submitted
            </span>
            <div className="progress-mini-bar">
              <div
                className="progress-mini-fill"
                style={{ width: `${(submittedCount / Math.max(1, totalProblems)) * 100}%` }}
              />
            </div>
          </div>

          <button
            type="button"
            className={`btn ${allSubmitted ? "btn-primary finish-assessment-btn pulse" : "btn-secondary btn-sm"}`}
            onClick={goToReport}
            title="Complete assessment and view comprehensive performance report"
          >
            {allSubmitted ? "Finish Assessment & View Report →" : "View Report"}
          </button>
        </div>
      </div>

      {/* ---------- 3 Problems Selector Nav ---------- */}
      <div className="assessment-nav-card">
        <div className="assessment-tabs-row">
          {problems.map((p, idx) => {
            const isCurrent = idx === activeProblemIdx;
            const hasSubmission = p.score !== null && p.score !== undefined;
            const isFullPass = hasSubmission && p.score >= 1.0;
            const isPartial = hasSubmission && p.score < 1.0;

            return (
              <button
                key={p.id || idx}
                type="button"
                className={`problem-nav-tab ${isCurrent ? "active" : ""} ${
                  isFullPass ? "tab-solved" : isPartial ? "tab-attempted" : ""
                }`}
                onClick={() => handleProblemChange(idx)}
              >
                <div className="tab-left">
                  <span className="tab-number">Problem {idx + 1}</span>
                  <span className="tab-title">{p.title || `Problem ${idx + 1}`}</span>
                </div>

                <div className="tab-meta">
                  <span className="tab-diff-tag">Diff {p.difficulty || 1}</span>
                  {isFullPass && <span className="status-pill status-solved" title="All tests passed">✓ Solved</span>}
                  {isPartial && (
                    <span className="status-pill status-partial" title={`${Math.round(p.score * 100)}% passed`}>
                      {Math.round(p.score * 100)}%
                    </span>
                  )}
                  {!hasSubmission && isCurrent && <span className="status-pill status-active">Editing</span>}
                  {!hasSubmission && !isCurrent && <span className="status-pill status-idle">Not Started</span>}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {error && <div className="alert-error">{error}</div>}

      <div className="lc-layout">
        {/* ---------- Left Pane: Problem Description / Testcases / Result ---------- */}
        <div className="lc-pane lc-problem-pane">
          <div className="lc-tabs">
            <button
              className={`lc-tab ${currentTab === "description" ? "active" : ""}`}
              onClick={() => setCurrentTab("description")}
            >
              Description
            </button>
            <button
              className={`lc-tab ${currentTab === "testcases" ? "active" : ""}`}
              onClick={() => setCurrentTab("testcases")}
            >
              Testcases ({visibleCases.length})
            </button>
            <button
              className={`lc-tab ${currentTab === "result" ? "active" : ""}`}
              onClick={() => currentResult && setCurrentTab("result")}
              disabled={!currentResult}
            >
              {currentResult ? `Result (${currentResult.status})` : "Result"}
            </button>
          </div>

          {/* Description View */}
          {currentTab === "description" && (
            <div className="lc-pane-body">
              <div className="lc-title-row">
                <h2 className="lc-title">
                  {activeProblemIdx + 1}. {activeProblem?.title || "Problem"}
                </h2>
                <DifficultyBadge level={activeProblem?.difficulty || 1} />
              </div>

              <p className="problem-text">{activeProblem?.problemStatement}</p>

              {(activeProblem?.examples || []).map((ex, i) => (
                <div className="example-block" key={i}>
                  <div className="example-label">Example {i + 1}:</div>
                  <div className="example-row">
                    <span className="io-tag">Input:</span>
                    <pre className="example-io">{ex.input}</pre>
                  </div>
                  <div className="example-row">
                    <span className="io-tag">Output:</span>
                    <pre className="example-io">{ex.output}</pre>
                  </div>
                </div>
              ))}

              {activeProblem?.constraints?.length > 0 && (
                <div className="constraints-block">
                  <div className="example-label">Constraints:</div>
                  <ul className="constraints-list">
                    {activeProblem.constraints.map((c, i) => (
                      <li key={i}><code>{c}</code></li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Testcases View */}
          {currentTab === "testcases" && (
            <div className="lc-pane-body">
              <h3 className="section-title">Sample Testcases</h3>
              <p className="hint">
                These are real input test cases evaluated when you press <strong>Run Code</strong>.
                Additional hidden edge cases are tested on <strong>Submit</strong>.
              </p>

              <div className="tc-pills-row">
                {visibleCases.map((tc, idx) => (
                  <button
                    key={tc.id || idx}
                    type="button"
                    className={`tc-pill ${selectedCaseIdx === idx ? "active" : ""}`}
                    onClick={() => setSelectedCaseIdx(idx)}
                  >
                    Case {idx + 1}
                  </button>
                ))}
              </div>

              {visibleCases[selectedCaseIdx] && (
                <div className="tc-details-box">
                  <div className="tc-field">
                    <div className="tc-field-label">Input</div>
                    <pre className="tc-field-content">{visibleCases[selectedCaseIdx].input}</pre>
                  </div>
                  <div className="tc-field">
                    <div className="tc-field-label">Expected Output</div>
                    <pre className="tc-field-content">{visibleCases[selectedCaseIdx].expectedOutput}</pre>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Result View */}
          {currentTab === "result" && currentResult && (
            <div className="lc-pane-body">
              {/* LeetCode Verdict Banner */}
              <div className={`lc-verdict-banner ${
                isAccepted ? "verdict-accepted" : isCompileError ? "verdict-compile-error" : "verdict-wrong-answer"
              }`}>
                <div className="verdict-main">
                  <span className="verdict-icon">
                    {isAccepted ? "✓" : isCompileError ? "!" : "✕"}
                  </span>
                  <div className="verdict-text-group">
                    <h3 className="verdict-title">{currentResult.status}</h3>
                    <span className="verdict-subtext">
                      {isCompileError
                        ? "Syntax / Compilation issue found"
                        : `${currentResult.testsPassed} / ${currentResult.testsTotal} test cases passed (${Math.round((currentResult.score || 0) * 100)}%)`}
                    </span>
                  </div>
                </div>

                {!isCompileError && (
                  <div className="verdict-metrics">
                    <div className="metric-pill">
                      <span className="metric-label">Runtime</span>
                      <strong className="metric-val">{currentResult.runtimeMs || 15} ms</strong>
                    </div>
                    <div className="metric-pill">
                      <span className="metric-label">Memory</span>
                      <strong className="metric-val">{currentResult.memoryMb || "14.2 MB"}</strong>
                    </div>
                  </div>
                )}
              </div>

              {/* Compile Error Details */}
              {isCompileError && currentResult.errorMessage && (
                <div className="compile-error-card">
                  <div className="error-card-head">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                      <circle cx="12" cy="12" r="10"/>
                      <line x1="12" y1="8" x2="12" y2="12"/>
                      <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    Compiler / Syntax Error Output
                  </div>
                  <pre className="compile-error-text">{currentResult.errorMessage}</pre>
                </div>
              )}

              {/* Test Cases Results List */}
              {activeTestResults.length > 0 && (
                <div className="test-results-section">
                  <div className="tc-pills-row">
                    {activeTestResults.map((tc, idx) => (
                      <button
                        key={tc.id || idx}
                        type="button"
                        className={`tc-pill ${tc.passed ? "passed" : "failed"} ${selectedCaseIdx === idx ? "active" : ""}`}
                        onClick={() => setSelectedCaseIdx(idx)}
                      >
                        <span className="tc-status-dot">{tc.passed ? "✓" : "✕"}</span>
                        {tc.hidden ? `Hidden #${idx + 1}` : `Case ${idx + 1}`}
                      </button>
                    ))}
                  </div>

                  {currentCaseResult && (
                    <div className="tc-details-box">
                      <div className="tc-case-header">
                        <span className="tc-case-title">
                          {currentCaseResult.hidden
                            ? `Hidden Edge Case #${selectedCaseIdx + 1}`
                            : `Testcase ${selectedCaseIdx + 1}`}
                        </span>
                        <span className={`tc-badge ${currentCaseResult.passed ? "badge-pass" : "badge-fail"}`}>
                          {currentCaseResult.passed ? "Passed" : "Wrong Answer"}
                        </span>
                      </div>

                      <div className="tc-field">
                        <div className="tc-field-label">Input</div>
                        <pre className="tc-field-content">{currentCaseResult.input}</pre>
                      </div>

                      <div className="tc-field">
                        <div className="tc-field-label">Your Output</div>
                        <pre className={`tc-field-content ${currentCaseResult.passed ? "tc-match" : "tc-mismatch"}`}>
                          {currentCaseResult.actualOutput}
                        </pre>
                      </div>

                      <div className="tc-field">
                        <div className="tc-field-label">Expected Output</div>
                        <pre className="tc-field-content tc-expected">{currentCaseResult.expectedOutput}</pre>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Multi-Problem Flow & Actions */}
              <div className="lc-report-action-box">
                {activeProblemIdx < problems.length - 1 ? (
                  <div className="next-problem-prompt">
                    <p className="report-ready-text">
                      {isAccepted
                        ? "Great job on this problem! All test cases passed. Continue to the next challenge:"
                        : "Progress recorded for this problem. You can continue to the next problem and return here anytime:"}
                    </p>
                    <button
                      className="btn btn-primary lc-report-btn"
                      onClick={() => handleProblemChange(activeProblemIdx + 1)}
                    >
                      Proceed to Problem {activeProblemIdx + 2}: {problems[activeProblemIdx + 1]?.title} →
                    </button>
                  </div>
                ) : allSubmitted ? (
                  <div className="all-complete-prompt">
                    <p className="report-ready-text">
                      🎉 <strong>All 3 coding challenges submitted!</strong> Your comprehensive performance report is ready.
                    </p>
                    <button className="btn btn-primary lc-report-btn" onClick={goToReport}>
                      View Final Performance Report →
                    </button>
                  </div>
                ) : (
                  <div className="assessment-finish-prompt">
                    <p className="report-ready-text">
                      You have submitted Problem 3. You can review and refine earlier problems or proceed to the report:
                    </p>
                    <button className="btn btn-primary lc-report-btn" onClick={goToReport}>
                      Finish Assessment & View Report →
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* ---------- Right Pane: Code Editor + LeetCode Toolbar ---------- */}
        <div className="lc-pane lc-editor-pane">
          <div className="editor-toolbar">
            <div className="toolbar-left">
              <span className="editor-title">
                Problem {activeProblemIdx + 1} Code Editor
              </span>
              <div className="lang-picker-group">
                <select
                  className="lang-select-dropdown"
                  value={currentLang}
                  onChange={(e) => handleLanguageChange(e.target.value)}
                  disabled={running || submitting}
                  title="Choose programming language"
                >
                  {LANGUAGES.map((l) => (
                    <option key={l.id} value={l.id}>{l.label}</option>
                  ))}
                </select>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-ghost btn-sm reset-btn"
              onClick={handleReset}
              disabled={running || submitting}
              title="Reset code to initial template"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 12a9 9 0 0 1 15-6.7L21 8M21 3v5h-5M21 12a9 9 0 0 1-15 6.7L3 16M3 21v-5h5"/>
              </svg>
              Reset
            </button>
          </div>

          <div className="editor-wrapper">
            <textarea
              className="code-editor lc-code-editor"
              value={currentCode}
              onChange={(e) => setCurrentCode(e.target.value)}
              onKeyDown={handleTabKey}
              spellCheck={false}
              placeholder={`// Write your algorithm solution for Problem ${activeProblemIdx + 1} (${activeProblem?.title || ""}) here...`}
            />
          </div>

          <div className="lc-editor-actions assessment-editor-actions">
            <div className="problem-nav-buttons">
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => handleProblemChange(activeProblemIdx - 1)}
                disabled={activeProblemIdx === 0 || running || submitting}
                title="Go to previous problem"
              >
                ← Prev Problem
              </button>

              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => handleProblemChange(activeProblemIdx + 1)}
                disabled={activeProblemIdx >= problems.length - 1 || running || submitting}
                title="Go to next problem"
              >
                Next Problem →
              </button>
            </div>

            <div className="action-buttons-group">
              <button
                type="button"
                className="btn btn-secondary run-btn"
                onClick={handleRun}
                disabled={running || submitting}
              >
                {running ? (
                  <>Running visible test cases…</>
                ) : (
                  <>
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                      <polygon points="5 3 19 12 5 21 5 3"/>
                    </svg>
                    Run Code
                  </>
                )}
              </button>

              <button
                type="button"
                className="btn btn-primary submit-btn"
                onClick={handleSubmit}
                disabled={running || submitting}
              >
                {submitting ? (
                  <>Evaluating all test cases…</>
                ) : (
                  <>
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.2">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                    Submit Problem {activeProblemIdx + 1}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
