import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { api } from "../api/api.js";

export default function CreateAccount() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const passwordStrength = (pwd) => {
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    return score; // 0-4
  };

  const strength = passwordStrength(form.password);
  const strengthLabels = ["", "Weak", "Fair", "Good", "Strong"];
  const strengthColors = ["", "#f87171", "#f59e0b", "#34d399", "#10b981"];

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.name || !form.email || !form.password || !form.confirm) {
      setError("Please fill in all fields.");
      return;
    }
    if (form.password !== form.confirm) {
      setError("Passwords do not match.");
      return;
    }
    if (form.password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    setLoading(true);
    try {
      const data = await api.register({ name: form.name, email: form.email, password: form.password });
      login(data);
      navigate("/", { replace: true });
    } catch (err) {
      // Fallback: store locally for offline/no-auth-backend use
      const stored = localStorage.getItem("ai_interview_accounts");
      const accounts = stored ? JSON.parse(stored) : [];
      if (accounts.find((a) => a.email === form.email)) {
        setError("An account with this email already exists.");
        setLoading(false);
        return;
      }
      accounts.push({ name: form.name, email: form.email, password: form.password });
      localStorage.setItem("ai_interview_accounts", JSON.stringify(accounts));
      login({ name: form.name, email: form.email });
      navigate("/", { replace: true });
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
            Start your journey<br />to interview success
          </h2>
          <p className="auth-hero-sub">
            Join thousands of candidates who use InterviewIQ to land their
            dream roles. Your free account includes unlimited practice sessions.
          </p>
        </div>
        <ul className="auth-features">
          <li><span className="auth-feature-dot">✦</span> Free to get started</li>
          <li><span className="auth-feature-dot">✦</span> AI-powered feedback in seconds</li>
          <li><span className="auth-feature-dot">✦</span> Progress tracked automatically</li>
          <li><span className="auth-feature-dot">✦</span> Resume-tailored question sets</li>
        </ul>
        <div className="auth-panel-glow" />
      </div>

      {/* Right form panel */}
      <div className="auth-panel auth-panel-right">
        <div className="auth-form-wrap">
          <div className="auth-form-header">
            <h1 className="auth-form-title">Create your account</h1>
            <p className="auth-form-sub">Free forever · No credit card required</p>
          </div>

          {error && <div className="alert-error auth-alert">{error}</div>}

          <form className="auth-form" onSubmit={submit} id="register-form">
            <div className="auth-field">
              <label htmlFor="reg-name">Full name</label>
              <div className="auth-input-wrap">
                <span className="auth-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
                </span>
                <input
                  id="reg-name"
                  type="text"
                  name="name"
                  placeholder="Jane Smith"
                  value={form.name}
                  onChange={handle}
                  autoComplete="name"
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="reg-email">Email address</label>
              <div className="auth-input-wrap">
                <span className="auth-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></svg>
                </span>
                <input
                  id="reg-email"
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
              <label htmlFor="reg-password">Password</label>
              <div className="auth-input-wrap">
                <span className="auth-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </span>
                <input
                  id="reg-password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  placeholder="Min. 6 characters"
                  value={form.password}
                  onChange={handle}
                  autoComplete="new-password"
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
              {/* Password strength meter */}
              {form.password.length > 0 && (
                <div className="auth-strength">
                  <div className="auth-strength-bars">
                    {[1, 2, 3, 4].map((i) => (
                      <div
                        key={i}
                        className="auth-strength-bar"
                        style={{ background: i <= strength ? strengthColors[strength] : "var(--border)" }}
                      />
                    ))}
                  </div>
                  <span className="auth-strength-label" style={{ color: strengthColors[strength] }}>
                    {strengthLabels[strength]}
                  </span>
                </div>
              )}
            </div>

            <div className="auth-field">
              <label htmlFor="reg-confirm">Confirm password</label>
              <div className="auth-input-wrap">
                <span className="auth-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 12l2 2 4-4"/><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </span>
                <input
                  id="reg-confirm"
                  type={showConfirm ? "text" : "password"}
                  name="confirm"
                  placeholder="Re-enter password"
                  value={form.confirm}
                  onChange={handle}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="auth-eye-btn"
                  onClick={() => setShowConfirm((v) => !v)}
                  aria-label="Toggle confirm visibility"
                >
                  {showConfirm ? (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  ) : (
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  )}
                </button>
              </div>
            </div>

            <button
              id="register-submit"
              type="submit"
              className="btn btn-primary btn-block auth-submit-btn"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="auth-spinner" />
                  Creating account…
                </>
              ) : (
                "Create account →"
              )}
            </button>
          </form>

          <p className="auth-switch">
            Already have an account?{" "}
            <Link to="/login" className="auth-link">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
