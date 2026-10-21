const BASE = "/api";

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Request failed: ${res.status}`);
  }
  return res.json();
}

export const api = {
  // Section III-A: Resume Analyzer / session lifecycle
  startSession: (payload) =>
    request("/sessions", { method: "POST", body: JSON.stringify(payload) }),
  getSession: (id) => request(`/sessions/${id}`),
  resumeAnalysis: (id) => request(`/sessions/${id}/resume-analysis`),

  // Section III-B/D/E: Question generation, answer scoring, adaptive difficulty
  nextQuestion: (sessionId, type = "BEHAVIORAL") =>
    request(`/sessions/${sessionId}/questions/next?type=${type}`, { method: "POST" }),
  submitAnswer: (sessionId, payload) =>
    request(`/sessions/${sessionId}/questions/answer`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  listQuestions: (sessionId) => request(`/sessions/${sessionId}/questions`),

  // Section III-F: Coding round
  getCodingProblems: (sessionId) =>
    request(`/sessions/${sessionId}/coding/problems`),
  nextCodingProblem: (sessionId) =>
    request(`/sessions/${sessionId}/coding/next`, { method: "POST" }),
  runCode: (sessionId, payload) =>
    request(`/sessions/${sessionId}/coding/run`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  submitCode: (sessionId, payload) =>
    request(`/sessions/${sessionId}/coding/submit`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),

  // Section III-G: Performance report
  getReport: (sessionId) => request(`/sessions/${sessionId}/report`)
};
