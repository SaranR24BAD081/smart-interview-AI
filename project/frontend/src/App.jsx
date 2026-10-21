import React from "react";
import { Routes, Route } from "react-router-dom";
import Sidebar from "./components/Sidebar.jsx";
import FlowStepper from "./components/FlowStepper.jsx";
import ResumeUpload from "./pages/ResumeUpload.jsx";
import InterviewSession from "./pages/InterviewSession.jsx";
import CodingRound from "./pages/CodingRound.jsx";
import PerformanceReport from "./pages/PerformanceReport.jsx";

export default function App() {
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
