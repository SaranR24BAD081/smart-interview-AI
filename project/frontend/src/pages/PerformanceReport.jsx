import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../api/api.js";

export default function PerformanceReport() {
  const { sessionId } = useParams();
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const r = await api.getReport(sessionId);
        setReport(r);
      } catch (err) {
        setError(err.message);
      }
    })();
  }, [sessionId]);

  if (error) return <div className="page-container"><div className="alert-error">{error}</div></div>;
  if (!report) return <div className="page-container">Generating your report...</div>;

  return (
    <div className="page-container">
      <div className="page-head">
        <h1>Performance report</h1>
        <p className="subtitle">
          {report.candidateName} &middot; {report.jobRole}
        </p>
      </div>

      <div className="card">
        <h3 className="card-title">Summary</h3>
        <div className="report-grid">
          <Metric label="Communication" value={report.averageCommunicationScore} />
          <Metric label="Confidence" value={report.averageConfidenceScore} />
          <Metric label="Overall performance" value={report.averageOverallPerformance} />
          <Metric label="Coding score" value={report.averageCodingScore} />
        </div>
        <p className="hint">
          Final adaptive difficulty reached: <strong>{report.finalDifficulty}</strong> / 5
        </p>
      </div>

      <div className="report-cols">
        <div className="card">
          <h3 className="card-title strength-title">Strengths</h3>
          {report.strengths.length ? (
            <ul className="trait-list">{report.strengths.map((s, i) => <li key={i}>{s}</li>)}</ul>
          ) : (
            <p className="hint">Keep practicing &mdash; no standout strengths identified yet.</p>
          )}
        </div>

        <div className="card">
          <h3 className="card-title improve-title">Areas for improvement</h3>
          {report.improvementAreas.length ? (
            <ul className="trait-list">{report.improvementAreas.map((s, i) => <li key={i}>{s}</li>)}</ul>
          ) : (
            <p className="hint">Great job &mdash; no major gaps identified this session.</p>
          )}
        </div>
      </div>

      {report.questionBreakdown?.length > 0 && (
        <div className="card">
          <h3 className="card-title">Question by question</h3>
          <p className="hint qbq-intro">Each question below is scored only against its own answer.</p>
          <div className="qbq-list">
            {report.questionBreakdown.map((q, i) => (
              <div className="qbq-item" key={i}>
                <div className="qbq-head">
                  <span className="qbq-index">Q{i + 1} &middot; {q.type} &middot; difficulty {q.difficulty}</span>
                </div>
                <p className="qbq-question">{q.questionText}</p>
                <p className="qbq-answer">
                  {q.answerTranscript ? `"${q.answerTranscript}"` : <em>No answer recorded.</em>}
                </p>
                <div className="qbq-scores">
                  <MiniScore label="Communication" value={q.communicationScore} />
                  <MiniScore label="Confidence" value={q.confidenceScore} />
                  <MiniScore label="Performance" value={q.performanceScore} />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {report.codingBreakdown?.length > 0 && (
        <div className="card">
          <h3 className="card-title">Coding round</h3>
          {report.codingBreakdown.map((c, i) => (
            <div className="qbq-item" key={i}>
              <div className="qbq-head">
                <span className="qbq-index">{c.title} &middot; {c.language}</span>
                <span className="qbq-index">{c.testsPassed} / {c.testsTotal} checks &middot; {Math.round((c.score || 0) * 100)}%</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <Link className="btn btn-primary" to="/">Start a new session</Link>
    </div>
  );
}

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span className="metric-value">{Math.round((value || 0) * 100)}%</span>
      <span className="metric-label">{label}</span>
    </div>
  );
}

function MiniScore({ label, value }) {
  return (
    <span className="qbq-mini-score">
      {label}: <strong>{Math.round((value || 0) * 100)}%</strong>
    </span>
  );
}
