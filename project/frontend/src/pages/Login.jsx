import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../api/api.js";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.email || !form.password) {
      setError("Please fill in all fields.");
      return;
    }
    setLoading(true);
    try {
      const data = await api.login(form);
      login(data);
      navigate("/", { replace: true });
    } catch (err) {
      // Fallback: local-only auth for when backend has no auth endpoints yet
      const stored = localStorage.getItem("ai_interview_accounts");
      const accounts = stored ? JSON.parse(stored) : [];
      const match = accounts.find(
        (a) => a.email === form.email && a.password === form.password
      );
      if (match) {
        login({ name: match.name, email: match.email });
        navigate("/", { replace: true });
      } else {
        setError(err.message || "Invalid email or password.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      {/* Left decorative panel */}
      <div className="auth-panel auth-panel-left">
        <div className="auth-brand">
          <div className="auth-brand-mark">AI</div>
          <span className="auth-brand-name">InterviewIQ</span>
        </div>
        <div className="auth-hero">
          <h2 className="auth-hero-title">
            Ace your next<br />interview with AI
          </h2>
          <p className="auth-hero-sub">
            Practice with an adaptive AI interviewer, get instant feedback, and
            track your performance across every session.
          </p>
        </div>
        <ul className="auth-features">
          <li><span className="auth-feature-dot">✦</span> Adaptive difficulty engine</li>
          <li><span className="auth-feature-dot">✦</span> Live coding environment</li>
          <li><span className="auth-feature-dot">✦</span> Detailed performance reports</li>
          <li><span className="auth-feature-dot">✦</span> Resume-aware questions</li>
        </ul>
        <div className="auth-panel-glow" />
      </div>

      {/* Right form panel */}
      <div className="auth-panel auth-panel-right">
        <div className="auth-form-wrap">
          <div className="auth-form-header">
            <h1 className="auth-form-title">Welcome back</h1>
            <p className="auth-form-sub">Sign in to continue your practice</p>
          </div>

          {error && <div className="alert-error auth-alert">{error}</div>}

          <form className="auth-form" onSubmit={submit} id="login-form">
            <div className="auth-field">
              <label htmlFor="login-email">Email address</label>
              <div className="auth-input-wrap">
                <span className="auth-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></svg>
                </span>
                <input
                  id="login-email"
                  type="email"
                  name="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={handle}
                  autoComplete="email"
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="login-password">Password</label>
              <div className="auth-input-wrap">
                <span className="auth-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </span>
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  placeholder="••••••••"
                  value={form.password}
                  onChange={handle}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  className="auth-eye-btn"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label="Toggle password visibility"
                >
                  {showPassword ? (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  ) : (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  )}
                </button>
              </div>
            </div>

            <button
              id="login-submit"
              type="submit"
              className="btn btn-primary btn-block auth-submit-btn"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="auth-spinner" />
                  Signing in…
                </>
              ) : (
                "Sign in →"
              )}
            </button>
          </form>

          <p className="auth-switch">
            Don't have an account?{" "}
            <Link to="/create-account" className="auth-link">Create one free</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
