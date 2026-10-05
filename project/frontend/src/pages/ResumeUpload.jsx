import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as pdfjsLib from "pdfjs-dist";
import { api } from "../api/api.js";

// Match the worker to whatever pdfjs-dist version is actually installed,
// served from cdnjs so we don't have to fight Vite's worker bundling.
// Use a CDN worker URL – fall back to empty string (main-thread parsing) if blocked
try {
  pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`;
} catch (_) {
  pdfjsLib.GlobalWorkerOptions.workerSrc = "";
}

const RUBRIC = [
  {
    title: "Communication clarity",
    body: "Answers are structured, on-topic, and easy to follow from start to finish."
  },
  {
    title: "Confidence & pacing",
    body: "Steady delivery with minimal filler words and a natural response time."
  },
  {
    title: "Technical depth",
    body: "Explanations hold up under follow-up questions relevant to the target role."
  },
  {
    title: "Adaptive follow-through",
    body: "Question difficulty rises or eases with every answer (Eq. 3), so effort is rewarded in real time."
  }
];

export default function ResumeUpload() {
  const [candidateName, setCandidateName] = useState("");
  const [jobRole, setJobRole] = useState("");
  const [resumeText, setResumeText] = useState("");
  const [fileName, setFileName] = useState("");
  const [parsing, setParsing] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const [camState, setCamState] = useState("idle"); // idle | requesting | granted | denied
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    return () => streamRef.current?.getTracks().forEach((t) => t.stop());
  }, []);

  const requestCamera = async () => {
    setCamState("requesting");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      streamRef.current = stream;
      if (videoRef.current) videoRef.current.srcObject = stream;
      setCamState("granted");
    } catch (err) {
      setCamState("denied");
    }
  };

  const readFileAsText = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result || "");
      reader.onerror = () => reject(new Error("FileReader failed"));
      reader.readAsText(file);
    });

  const handleFile = async (file) => {
    if (!file) return;
    setError(null);
    setFileName(file.name);

    const isPdf = file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf");

    setParsing(true);
    try {
      if (isPdf) {
        // Try PDF.js first; if it fails for any reason fall back to raw text read
        try {
          const buffer = await file.arrayBuffer();
          const loadingTask = pdfjsLib.getDocument({
            data: buffer,
            verbosity: 0 // suppress console warnings
          });
          const pdf = await loadingTask.promise;
          let text = "";
          for (let p = 1; p <= pdf.numPages; p++) {
            const page = await pdf.getPage(p);
            const content = await page.getTextContent();
            text += content.items.map((item) => item.str).join(" ") + "\n";
          }
          const trimmed = text.trim();
          if (trimmed.length > 20) {
            setResumeText(trimmed);
          } else {
            // PDF had no extractable text (scanned image); fall back
            throw new Error("No text layer in PDF");
          }
        } catch (pdfErr) {
          console.warn("PDF.js parse failed, falling back to text read:", pdfErr.message);
          // Best-effort text read for image-only or protected PDFs
          try {
            const raw = await readFileAsText(file);
            const cleaned = raw.replace(/[^\x20-\x7E\n\r\t]/g, " ").replace(/\s+/g, " ").trim();
            if (cleaned.length > 20) {
              setResumeText(cleaned);
            } else {
              setError(
                "This PDF doesn't have selectable text (it may be scanned). Please paste your resume text in the box below."
              );
              setFileName("");
            }
          } catch {
            setError("Couldn't read the PDF. Please paste your resume text in the box below.");
            setFileName("");
          }
        }
      } else {
        // For every other file type (.txt, .doc, .docx, .rtf, etc.) read as text
        try {
          const raw = await readFileAsText(file);
          // Strip non-printable chars that appear in binary Office formats
          const cleaned = raw.replace(/[^\x20-\x7E\n\r\t]/g, " ").replace(/\s+/g, " ").trim();
          setResumeText(cleaned || raw.trim());
        } catch {
          setError("Couldn't read that file. Please paste your resume text in the box below.");
          setFileName("");
        }
      }
    } finally {
      setParsing(false);
    }
  };

  const wordCount = resumeText.trim() ? resumeText.trim().split(/\s+/).length : 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!candidateName || !jobRole || !resumeText) {
      setError("Please fill in your name, job role, and resume text.");
      return;
    }
    setLoading(true);
    try {
      const session = await api.startSession({ candidateName, jobRole, resumeText });
      navigate(`/interview/${session.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const ready = candidateName && jobRole && resumeText;

  return (
    <div className="page-container">
      <div className="page-head">
        <h1>New practice session</h1>
        <p className="subtitle">
          Tell us who you are and the role you're aiming for. Every question is
          generated against your resume, then the difficulty adapts to how you answer.
        </p>
      </div>

      {error && <div className="alert-error">{error}</div>}

      <div className="setup-grid">
        <form className="card setup-form" onSubmit={handleSubmit}>
          <h3 className="card-title">Your details</h3>

          <label>
            Full name
            <input
              type="text"
              value={candidateName}
              onChange={(e) => setCandidateName(e.target.value)}
              placeholder="Enter Your Name"
            />
          </label>

          <label>
            Target job role
            <input
              type="text"
              value={jobRole}
              onChange={(e) => setJobRole(e.target.value)}
              placeholder="Enter Your Job Role"
            />
          </label>

          <label>
            Resume file
            <div
              className={`dropzone ${dragOver ? "drag" : ""}`}
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragOver(false);
                handleFile(e.dataTransfer.files?.[0]);
              }}
            >
              {fileName ? (
                <div className="dz-file">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <path d="M14 2v6h6" />
                  </svg>
                  <span className="dz-file-name">{fileName}</span>
                  <span className="dz-file-meta">
                    {parsing ? "Reading…" : `${wordCount} words parsed`}
                  </span>
                </div>
              ) : (
                <>
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 15V4M12 4L7.5 8.5M12 4l4.5 4.5" />
                    <path d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
                  </svg>
                  <span>Drop your resume here or <strong>browse</strong></span>
                  <span className="dz-hint">PDF, DOCX, TXT or any file — up to 8 MB</span>
                </>
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="*"
                onChange={(e) => handleFile(e.target.files?.[0])}
                hidden
              />
            </div>
          </label>

          <label>
            Or paste resume text
            <textarea
              rows={7}
              value={resumeText}
              onChange={(e) => setResumeText(e.target.value)}
              placeholder="Paste your resume content here..."
            />
          </label>

          <button className="btn btn-primary btn-block" type="submit" disabled={loading || parsing}>
            {loading ? "Starting session..." : "Start interview"}
          </button>
          <p className="form-note">
            {ready ? "Ready — the interviewer will ask its first question right away." : "Fill in every field to unlock the interview."}
          </p>
        </form>

        <div className="card rubric-panel">
          <h3 className="card-title">Camera & microphone check</h3>
          <div className="camera-box">
            {camState === "granted" ? (
              <video ref={videoRef} autoPlay playsInline muted />
            ) : (
              <div className="camera-placeholder">
                <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="2.5" y="6.5" width="14" height="11" rx="2" />
                  <path d="M16.5 10.2 21.5 7v10l-5-3.2" />
                </svg>
                <span>
                  {camState === "denied"
                    ? "Access denied — check your browser's site settings"
                    : "Grant Panelist camera and mic access"}
                </span>
              </div>
            )}
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-block"
            onClick={requestCamera}
            disabled={camState === "requesting" || camState === "granted"}
          >
            {camState === "granted"
              ? "Camera & mic ready"
              : camState === "requesting"
              ? "Requesting access…"
              : "Allow access"}
          </button>

          <h3 className="card-title rubric-title-spaced">What we'll evaluate</h3>
          <p className="rubric-intro">
            A behavioral round, then a coding round, each scored live and rolled
            into one final report.
          </p>
          <ul className="rubric-list">
            {RUBRIC.map((r) => (
              <li key={r.title}>
                <span className="rubric-check">
                  <svg viewBox="0 0 20 20" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M4 10.5 8 14l8-9" />
                  </svg>
                </span>
                <span>
                  <strong>{r.title}</strong>
                  <span className="rubric-body">{r.body}</span>
                </span>
              </li>
            ))}
          </ul>
          <div className="mic-hint">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
              <rect x="9" y="3" width="6" height="11" rx="3" />
              <path d="M6 11a6 6 0 0 0 12 0M12 19v2" />
            </svg>
            Camera & mic stay on through the interview itself, next to the interviewer.
          </div>
        </div>
      </div>
    </div>
  );
}
