import React from "react";
import { useLocation } from "react-router-dom";

const STEPS = [
  { key: "setup", label: "Setup", match: (p) => p === "/" },
  { key: "interview", label: "Interview", match: (p) => p.startsWith("/interview") },
  { key: "coding", label: "Coding", match: (p) => p.startsWith("/coding") },
  { key: "report", label: "Report", match: (p) => p.startsWith("/report") }
];

export default function FlowStepper() {
  const { pathname } = useLocation();
  const activeIndex = STEPS.findIndex((s) => s.match(pathname));

  return (
    <div className="flow-stepper">
      {STEPS.map((step, i) => {
        const state = i < activeIndex ? "done" : i === activeIndex ? "current" : "upcoming";
        return (
          <div className={`flow-step ${state}`} key={step.key}>
            <span className="flow-dot">{state === "done" ? "\u2713" : i + 1}</span>
            <span className="flow-label">{step.label}</span>
            {i < STEPS.length - 1 && <span className="flow-line" />}
          </div>
        );
      })}
    </div>
  );
}
