import React from "react";

const LABELS = { 1: "Very Easy", 2: "Easy", 3: "Medium", 4: "Hard", 5: "Very Hard" };

export default function DifficultyBadge({ level }) {
  return (
    <span className={`difficulty-badge level-${level}`}>
      <span className="difficulty-dot" />
      {LABELS[level] || level}
    </span>
  );
}
