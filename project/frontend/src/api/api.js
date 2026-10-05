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
  // Authentication
  login: (payload) =>
    request("/auth/login", { method: "POST", body: JSON.stringify(payload) }),
  register: (payload) =>
    request("/auth/register", { method: "POST", body: JSON.stringify(payload) }),

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
  skipQuestion: (sessionId, questionId) =>
    request(`/sessions/${sessionId}/questions/skip`, {
      method: "POST",
      body: JSON.stringify({ questionId })
    }),
  listQuestions: (sessionId) => request(`/sessions/${sessionId}/questions`),

  // Section III-F: Coding round
  nextCodingProblem: (sessionId) =>
    request(`/sessions/${sessionId}/coding/next`, { method: "POST" }),
  changeLanguage: (sessionId, language) =>
    request(`/sessions/${sessionId}/coding/language`, {
      method: "POST",
      body: JSON.stringify({ language })
    }),
  submitCode: (sessionId, payload) =>
    request(`/sessions/${sessionId}/coding/submit`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),

  // Section III-G: Performance report
  getReport: (sessionId) => request(`/sessions/${sessionId}/report`)
};
