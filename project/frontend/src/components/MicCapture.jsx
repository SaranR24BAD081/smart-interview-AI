import React, { useEffect, useRef, useState } from "react";

/**
 * Voice-only answer capture. There is no text input or typing fallback —
 * the candidate must record an answer with the microphone; the live
 * transcript is shown read-only for their own reference while speaking.
 *
 * Browser quirk this works around: Chrome's SpeechRecognition ends a
 * session after a few seconds of silence even with `continuous: true`.
 * Without handling that, any brief pause after clicking "Start answering"
 * (e.g. taking a breath before speaking) looks identical to the candidate
 * being finished, and the answer gets force-submitted empty. Instead, an
 * automatic end (no explicit "Stop & submit" click) silently restarts
 * listening and keeps accumulating the transcript, up to a small number of
 * silent retries before giving up with a clear message.
 */
const MAX_SILENT_RESTARTS = 6;

export default function MicCapture({ onResult, disabled }) {
  const [recording, setRecording] = useState(false);
  const [transcript, setTranscript] = useState("");
  const [statusMessage, setStatusMessage] = useState(null); // { type: 'error'|'info', text }

  const recognitionRef = useRef(null);
  const restartTimeoutRef = useRef(null);
  const startTimeRef = useRef(null);
  const confirmedTranscriptRef = useRef(""); // accumulated final text, persists across auto-restarts
  const manualStopRef = useRef(false); // true only once the user clicks "Stop & submit"
  const hardErrorRef = useRef(false); // true once we've already shown a specific status message that finishRecording() must not overwrite
  const submittedRef = useRef(false);
  const restartAttemptsRef = useRef(0);
  const isAutoRestartRef = useRef(false); // true while the *current* recognition instance was started automatically (not by a user click)

  const SpeechRecognition =
    typeof window !== "undefined" &&
    (window.SpeechRecognition || window.webkitSpeechRecognition);

  useEffect(() => {
    return () => {
      clearTimeout(restartTimeoutRef.current);
      recognitionRef.current?.abort?.();
    };
  }, []);

  const finishRecording = () => {
    setRecording(false);
    if (submittedRef.current) return; // already handled
    submittedRef.current = true;

    const finalTranscript = confirmedTranscriptRef.current.trim();
    const responseTimeSeconds = startTimeRef.current
      ? (performance.now() - startTimeRef.current) / 1000
      : 0;

    if (!finalTranscript) {
      if (!hardErrorRef.current) {
        setStatusMessage({
          type: "error",
          text: "We didn't catch any speech. Press \u201cStart answering\u201d and try again."
        });
      }
      return;
    }

    setStatusMessage(null);
    onResult(finalTranscript, responseTimeSeconds);
  };

  const createRecognition = () => {
    const recognition = new SpeechRecognition();
    recognition.lang = "en-US";
    recognition.interimResults = true;
    recognition.continuous = true;

    recognition.onresult = (event) => {
      let interim = "";
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const chunk = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          confirmedTranscriptRef.current = (confirmedTranscriptRef.current + " " + chunk).trim();
          restartAttemptsRef.current = 0; // candidate is talking; reset the silent-retry budget
        } else {
          interim += chunk;
        }
      }
      setTranscript((confirmedTranscriptRef.current + " " + interim).trim());
    };

    recognition.onerror = (event) => {
      if (event.error === "aborted" && manualStopRef.current) {
        return; // expected: we stopped it ourselves
      }
      if (event.error === "not-allowed" || event.error === "service-not-allowed") {
        manualStopRef.current = true;
        if (isAutoRestartRef.current) {
          // This wasn't a real permission denial: some Chrome versions refuse
          // to silently restart the mic from a background timer (not a fresh
          // click), even though the candidate already granted mic access.
          // Don't claim mic access is "blocked" -- it isn't. End gracefully
          // via onend -> finishRecording, which submits whatever was already
          // captured, or asks for one more click if nothing was captured yet.
          hardErrorRef.current = true;
          setStatusMessage({
            type: "info",
            text: "Paused after a quiet moment \u2014 press \u201cStart answering\u201d to continue. Anything you already said is saved."
          });
          return;
        }
        hardErrorRef.current = true;
        setStatusMessage({
          type: "error",
          text: "Microphone access was blocked. Enable mic permission for this site and try again."
        });
        return;
      }
      if (event.error === "audio-capture") {
        hardErrorRef.current = true;
        manualStopRef.current = true;
        setStatusMessage({
          type: "error",
          text: "No microphone was found. Connect a microphone and try again."
        });
        return;
      }
      if (event.error === "no-speech") {
        // Expected/frequent: Chrome's own silence timeout. Let onend restart it.
        setStatusMessage({ type: "info", text: "Still listening \u2014 go ahead whenever you're ready." });
        return;
      }
      // network or other transient errors: stop retrying and surface it plainly
      hardErrorRef.current = true;
      manualStopRef.current = true;
      setStatusMessage({ type: "error", text: "Something interrupted the microphone. Press \u201cStart answering\u201d to try again." });
    };

    recognition.onend = () => {
      if (manualStopRef.current) {
        finishRecording();
        return;
      }
      // Auto-ended (browser silence timeout), candidate hasn't clicked Stop.
      if (restartAttemptsRef.current >= MAX_SILENT_RESTARTS) {
        hardErrorRef.current = true;
        setStatusMessage({
          type: "error",
          text: "We still aren't picking up any audio. Check that your microphone is connected and allowed for this site, then press \u201cStart answering\u201d again."
        });
        finishRecording();
        return;
      }
      restartAttemptsRef.current += 1;
      restartTimeoutRef.current = setTimeout(() => {
        if (manualStopRef.current) return;
        try {
          const r = createRecognition();
          recognitionRef.current = r;
          isAutoRestartRef.current = true;
          r.start();
        } catch {
          finishRecording();
        }
      }, 250);
    };

    return recognition;
  };

  const start = () => {
    if (!SpeechRecognition) {
      setStatusMessage({
        type: "error",
        text:
          "Voice answers require browser speech recognition, which isn't available here. Please switch to Chrome or Edge to continue."
      });
      return;
    }

    clearTimeout(restartTimeoutRef.current);
    manualStopRef.current = false;
    hardErrorRef.current = false;
    isAutoRestartRef.current = false;
    submittedRef.current = false;
    restartAttemptsRef.current = 0;
    confirmedTranscriptRef.current = "";
    setTranscript("");
    setStatusMessage(null);
    startTimeRef.current = performance.now();

    const recognition = createRecognition();
    recognitionRef.current = recognition;
    recognition.start();
    setRecording(true);
  };

  const stop = () => {
    manualStopRef.current = true;
    clearTimeout(restartTimeoutRef.current);
    recognitionRef.current?.stop();
    setRecording(false);
  };

  return (
    <div className="mic-capture">
      <textarea
        className="transcript-box"
        placeholder="Your spoken answer will be transcribed here as you talk..."
        value={transcript}
        readOnly
        rows={4}
      />
      {statusMessage && (
        <p className={statusMessage.type === "error" ? "alert-error" : "hint"}>{statusMessage.text}</p>
      )}
      <div className="mic-controls">
        {!recording ? (
          <button className="btn btn-record" onClick={start} disabled={disabled}>
            <span className="record-dot" />
            Start answering
          </button>
        ) : (
          <button className="btn btn-danger" onClick={stop}>
            <span className="record-dot recording" />
            Stop &amp; submit
          </button>
        )}
      </div>
    </div>
  );
}
