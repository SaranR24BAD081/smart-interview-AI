import React, { useEffect, useRef, useState } from "react";

/**
 * Section III-C: MetaHuman delivery.
 *
 * Speaks each question aloud via the Web Speech API and animates a simple
 * face (eyes + mouth) in sync with speaking state, standing in for a full
 * MetaHuman render until that pipeline is wired up. Swap the markup inside
 * .avatar-circle for an <iframe>/<video> embed of a real MetaHuman stream
 * without changing the speaking-state contract below.
 *
 * If `selfViewRef` is passed, a small local camera preview is rendered
 * under the avatar so the candidate can see themselves during the round.
 */
export default function AvatarInterviewer({ text, onSpeechEnd, selfViewRef, camState }) {
  const [speaking, setSpeaking] = useState(false);
  const utterRef = useRef(null);

  useEffect(() => {
    if (!text) return;
    if (!("speechSynthesis" in window)) {
      onSpeechEnd?.();
      return;
    }

    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.rate = 0.98;
    utter.pitch = 1.0;
    utter.onstart = () => setSpeaking(true);
    utter.onend = () => {
      setSpeaking(false);
      onSpeechEnd?.();
    };
    utterRef.current = utter;
    window.speechSynthesis.speak(utter);

    return () => window.speechSynthesis.cancel();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text]);

  return (
    <div className="avatar-wrap">
      <div className={`avatar-circle ${speaking ? "speaking" : ""}`}>
        <div className="avatar-face">
          <span className="avatar-eye left" />
          <span className="avatar-eye right" />
          <span className="avatar-mouth" />
        </div>
      </div>
      <p className="avatar-name">Interviewer</p>
      <p className="avatar-caption">{speaking ? "Speaking\u2026" : "Waiting for your answer"}</p>

      {selfViewRef && (
        <div className="camera-box self-view">
          {camState === "denied" ? (
            <div className="camera-placeholder self-view-placeholder">
              <span>Camera off</span>
            </div>
          ) : (
            <video ref={selfViewRef} autoPlay playsInline muted />
          )}
          <span className="self-view-badge">You</span>
        </div>
      )}
    </div>
  );
}
