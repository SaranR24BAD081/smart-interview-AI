# AI Interview & Coding Assessment -- Frontend

React (Vite) single-page app implementing the client-side experience for
the **AI-Based Interview and Coding Assessment System**.

| Paper Section | Feature                              | Code                                |
|----------------|----------------------------------------|--------------------------------------|
| III-A          | Resume upload / job role selection     | `src/pages/ResumeUpload.jsx`         |
| III-C          | MetaHuman-interviewer stand-in (TTS)   | `src/components/AvatarInterviewer.jsx` |
| III-D          | Speech-to-Text capture                 | `src/components/MicCapture.jsx`      |
| III-E          | Adaptive difficulty display            | `src/components/DifficultyBadge.jsx` |
| III-F          | Coding round + code editor             | `src/pages/CodingRound.jsx`          |
| III-G          | Performance report                     | `src/pages/PerformanceReport.jsx`    |

## Running locally

Requires Node.js 18+.

```bash
cd frontend
npm install
npm run dev
```

The app runs at `http://localhost:5173` and proxies `/api/*` requests to
the backend at `http://localhost:8080` (see `vite.config.js`). Start the
backend first (see `../backend/README.md`).

## Notes / extension points

- **MetaHuman avatar**: `AvatarInterviewer.jsx` uses the browser's Speech
  Synthesis API and a simple animated avatar as a stand-in for the
  photorealistic Unreal Engine MetaHuman described in the paper, so the
  full flow can be demoed in any browser. Replace it with your Unreal
  Pixel Streaming embed for the real avatar.
- **Speech-to-Text**: `MicCapture.jsx` uses the Web Speech API
  (`SpeechRecognition`), supported in Chrome/Edge. For broader browser or
  language support, swap it for a server-side STT call (e.g. Whisper).
- **Code editor**: a plain `<textarea>` is used to keep the project
  dependency-light. Swap in `@monaco-editor/react` for a full-featured,
  VS Code-like editing experience -- `CodingRound.jsx` is structured so
  only the editor markup needs to change.
- **Resume parsing**: only `.txt` upload / paste is wired up client-side.
  For PDF/DOCX resumes, add a parsing library (`pdf.js`, `mammoth.js`) or
  extract text server-side before calling `POST /api/sessions`.
