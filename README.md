# AI-Based Interview and Coding Assessment System

Reference implementation accompanying the paper *"AI-Based Interview and
Coding Assessment System Using MetaHuman and Large Language Models"*.

This repository is split into two independent projects, matching the
system architecture in Section III of the paper:

```
project/
├── backend/     Java 17 + Spring Boot + Spring Data JPA REST API
│                (Resume Analyzer, Question Generation, Answer/Confidence
│                 scoring, Adaptive Difficulty Controller, Coding
│                 Assessment, Performance Report)
│
└── frontend/    React (Vite) single-page app
                 (Resume upload, MetaHuman-avatar stand-in + TTS,
                  Speech-to-Text capture, code editor, performance report)
```

## Quick start

1. **Backend** (runs on `http://localhost:8080`, zero external setup needed --
   uses an in-memory H2 database by default):

   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Frontend** (runs on `http://localhost:5173`, proxies API calls to the
   backend):

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Open `http://localhost:5173`, upload/paste a resume, pick a job role,
   and walk through the interview -> coding round -> performance report
   flow described in the paper.

See `backend/README.md` and `frontend/README.md` for module-by-module
details, configuration (MySQL, LLM provider), and extension points (real
MetaHuman/Unreal Engine integration, sandboxed code execution, etc.).

## Mapping to the paper's architecture (Fig. 1)

| Fig. 1 module                          | Backend                              | Frontend                              |
|-----------------------------------------|----------------------------------------|-----------------------------------------|
| Resume upload + job role                | `SessionController`                   | `ResumeUpload.jsx`                     |
| Resume Analyzer (Eq. 1)                 | `ResumeAnalyzerService`               | --                                      |
| LLM / Question Generation               | `QuestionGenerationService`           | --                                      |
| MetaHuman Interviewer                   | question text handed to client        | `AvatarInterviewer.jsx` (TTS stand-in) |
| Speech-to-Text                          | --                                     | `MicCapture.jsx` (Web Speech API)      |
| Answer Analysis (Eq. 2)                 | `SpeechAnalysisService`               | result display in `InterviewSession.jsx` |
| Adaptive Difficulty (Eq. 3)             | `AdaptiveDifficultyService`           | `DifficultyBadge.jsx`                  |
| Coding Question Generator + Editor      | `CodingQuestionService`, `CodeEvaluationService` | `CodingRound.jsx`            |
| Final Performance Report                | `ReportService`                       | `PerformanceReport.jsx`                |

This is a runnable *scaffold*: the NLP/LLM/speech-scoring logic uses
lightweight, dependency-free approximations so the whole system works out
of the box, with clearly marked extension points to plug in a real LLM,
a production STT/TTS provider, an Unreal Engine MetaHuman avatar, and a
sandboxed code-execution judge.
