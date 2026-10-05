# AI Interview & Coding Assessment -- Backend

Spring Boot (Java 17) + Spring Data JPA REST backend for the **AI-Based
Interview and Coding Assessment System**, implementing the server-side
modules described in Section III of the accompanying paper:

| Paper Section | Module                              | Code                                   |
|----------------|--------------------------------------|-----------------------------------------|
| III-A          | Resume Analyzer (Eq. 1)              | `service/ResumeAnalyzerService.java`    |
| III-B          | Personalized Question Generation     | `service/QuestionGenerationService.java`|
| III-C          | MetaHuman Interviewer (hand-off)     | `controller/QuestionController.java`    |
| III-D          | Speech-to-Text & Answer Analysis (Eq. 2) | `service/SpeechAnalysisService.java`|
| III-E          | Adaptive Difficulty Controller (Eq. 3)| `service/AdaptiveDifficultyService.java`|
| III-F          | Coding Question Generator + Assessment| `service/CodingQuestionService.java`, `service/CodeEvaluationService.java` |
| III-G          | Performance Report Generation        | `service/ReportService.java`            |

## Running locally (no MySQL needed)

By default the app runs against an in-memory H2 database, so you can start
it with nothing else installed but a JDK and Maven:

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api`, and the H2
console at `http://localhost:8080/h2-console` (JDBC URL:
`jdbc:h2:mem:ai_interview_db`).

## Switching to MySQL

Uncomment the MySQL block in `src/main/resources/application.properties`,
comment out the H2 block, create a schema, and set your credentials:

```sql
CREATE DATABASE ai_interview_db;
```

## Connecting a real LLM

`QuestionGenerationService.callLlm()` is a clearly marked extension point --
point it at your provider's chat/completions endpoint using the
`llm.api.url` / `llm.api.key` properties (or environment variables
`LLM_API_URL` / `LLM_API_KEY`) and the app will use it automatically;
without it, a deterministic template-based fallback keeps the app fully
functional offline.

## API summary

```
POST   /api/sessions                          Start a session (resume + job role)
GET    /api/sessions/{id}                      Fetch a session
GET    /api/sessions/{id}/resume-analysis       Eq.1 relevance score + ranked skills

POST   /api/sessions/{id}/questions/next?type=BEHAVIORAL|TECHNICAL   Generate next question
POST   /api/sessions/{id}/questions/answer      Submit STT transcript + features -> Eq.2/Eq.3
GET    /api/sessions/{id}/questions             List all questions asked so far

POST   /api/sessions/{id}/coding/next           Generate next coding problem
POST   /api/sessions/{id}/coding/submit         Submit code for automated assessment
GET    /api/sessions/{id}/coding                List coding submissions

GET    /api/sessions/{id}/report                Final structured performance report
```

## Notes on production hardening

- **Code execution**: `CodeEvaluationService` intentionally does *not*
  execute untrusted candidate code in-process. Wire `runAgainstSandbox()`
  up to an isolated judge (Docker sandbox, Judge0, etc.) before using this
  in a real deployment.
- **MetaHuman rendering**: this backend exposes text + audio-ready question
  content; the actual photorealistic avatar rendering and lip-sync happens
  in Unreal Engine / MetaHuman on the client side, as described in the
  paper -- the frontend included here uses the browser's Speech Synthesis
  API as a lightweight stand-in avatar so the whole app is runnable without
  Unreal Engine installed.
