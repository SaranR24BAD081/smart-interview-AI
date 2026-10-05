import React from "react";
import { Link, useLocation, useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

const NAV = [
  { key: "home", label: "New Session", path: "/", match: ["/"] },
  { key: "practice", label: "Practice", path: "/", match: ["/interview", "/coding"] },
  { key: "report", label: "Report", path: "/report", match: ["/report"] }
];

export default function Sidebar() {
  const location = useLocation();
  const { sessionId } = useParams();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const isActive = (item) => item.match.some((m) => location.pathname.startsWith(m) && (m !== "/" || location.pathname === "/"));

  const reportHref = sessionId ? `/report/${sessionId}` : null;

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="brand-mark">S</span>
        <span className="brand-name">Smart Interview</span>
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
        {user && (
          <div className="sidebar-user">
            <div className="sidebar-user-avatar">
              {user.name ? user.name[0].toUpperCase() : "U"}
            </div>
            <div className="sidebar-user-info">
              <span className="sidebar-user-name">{user.name || "User"}</span>
              <span className="sidebar-user-email">{user.email}</span>
            </div>
            <button
              className="sidebar-logout-btn"
              onClick={handleLogout}
              title="Sign out"
              aria-label="Sign out"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
            </button>
          </div>
        )}
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
