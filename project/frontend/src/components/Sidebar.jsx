import React from "react";
import { Link, useLocation, useParams } from "react-router-dom";

const NAV = [
  { key: "home", label: "New Session", path: "/", match: ["/"] },
  { key: "practice", label: "Practice", path: "/", match: ["/interview", "/coding"] },
  { key: "report", label: "Report", path: "/report", match: ["/report"] }
];

export default function Sidebar() {
  const location = useLocation();
  const { sessionId } = useParams();

  const isActive = (item) => item.match.some((m) => location.pathname.startsWith(m) && (m !== "/" || location.pathname === "/"));

  const reportHref = sessionId ? `/report/${sessionId}` : null;

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-mark">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
        </div>
        <div className="brand-text-col">
          <span className="brand-name">AI Interview</span>
          <span className="brand-tagline">Coding Assessment</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <Link to="/" className={`nav-item ${isActive(NAV[0]) ? "active" : ""}`}>
          <NavIcon name="home" />
          New Session
        </Link>
        <span className={`nav-item ${isActive(NAV[1]) ? "active" : ""} ${!sessionId ? "disabled" : ""}`}>
          <NavIcon name="mic" />
          Practice
        </span>
        {reportHref ? (
          <Link to={reportHref} className={`nav-item ${isActive(NAV[2]) ? "active" : ""}`}>
            <NavIcon name="chart" />
            Report
          </Link>
        ) : (
          <span className="nav-item disabled">
            <NavIcon name="chart" />
            Report
          </span>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="usage-card">
          <p className="usage-label">Adaptive difficulty engine</p>
          <p className="usage-sub">Questions recalibrate after every answer, live.</p>
        </div>
      </div>
    </aside>
  );
}

function NavIcon({ name }) {
  const paths = {
    home: <path d="M4 11.5 12 5l8 6.5M6 10v9h5v-5h2v5h5v-9" />,
    mic: (
      <>
        <rect x="9" y="3" width="6" height="11" rx="3" />
        <path d="M6 11a6 6 0 0 0 12 0M12 19v2" />
      </>
    ),
    chart: <path d="M5 20V10M12 20V4M19 20v-7" />
  };
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {paths[name]}
    </svg>
  );
}
