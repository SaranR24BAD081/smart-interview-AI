import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext.jsx";
import Sidebar from "./components/Sidebar.jsx";
import FlowStepper from "./components/FlowStepper.jsx";
import ResumeUpload from "./pages/ResumeUpload.jsx";
import InterviewSession from "./pages/InterviewSession.jsx";
import CodingRound from "./pages/CodingRound.jsx";
import PerformanceReport from "./pages/PerformanceReport.jsx";
import Login from "./pages/Login.jsx";
import CreateAccount from "./pages/CreateAccount.jsx";

/** Redirects unauthenticated users to /login */
function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

/** Redirects already-authenticated users away from auth pages */
function AuthRoute({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/" replace /> : children;
}

function AppShell() {
  return (
    <div className="shell">
      <Sidebar />
      <div className="shell-main">
        <header className="topbar">
          <FlowStepper />
        </header>
        <main className="content">
          <Routes>
            <Route path="/" element={<ResumeUpload />} />
            <Route path="/interview/:sessionId" element={<InterviewSession />} />
            <Route path="/coding/:sessionId" element={<CodingRound />} />
            <Route path="/report/:sessionId" element={<PerformanceReport />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Auth pages (public) */}
        <Route
          path="/login"
          element={
            <AuthRoute>
              <Login />
            </AuthRoute>
          }
        />
        <Route
          path="/create-account"
          element={
            <AuthRoute>
              <CreateAccount />
            </AuthRoute>
          }
        />

        {/* Protected app shell + all inner routes */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        />
      </Routes>
    </AuthProvider>
  );
}
