package com.aiinterview.service;

import com.aiinterview.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Section III-B: Personalized Question Generation.
 *
 * Builds a structured prompt from the candidate's ranked resume skills and
 * job role, then calls an LLM to obtain a personalized question. When no
 * LLM endpoint/key is configured (llm.api.url / llm.api.key in
 * application.properties), a deterministic template-based fallback is used
 * so the whole application remains runnable offline/out of the box.
 *
 * To integrate a real LLM: implement {@link #callLlm(String)} to POST the
 * prompt to your provider (e.g. the Anthropic or OpenAI Messages API) and
 * return the generated question text.
 */
@Service
public class QuestionGenerationService {

    @Value("${llm.api.url:}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    private static final String[] BEHAVIORAL_TEMPLATES = {
            "Tell me about a time you used %s to solve a challenging problem in %s.",
            "Walk me through your experience with %s and how it applies to a %s role.",
            "Describe a project where you applied %s. What was the outcome?",
            "How would you use %s to handle a difficult situation in a %s team?"
    };

    private static final String[] TECHNICAL_TEMPLATES = {
            "Explain how %s works and where you have used it in practice.",
            "What are the trade-offs of using %s compared to alternatives, in the context of %s?",
            "How would you design a system component using %s for a %s application?"
    };

    /**
     * Generates the next personalized question for a session.
     *
     * @param jobRole          selected job role
     * @param rankedSkills     resume skills ranked by relevance to the role (Eq. 1)
     * @param difficulty       current adaptive difficulty (1-5)
     * @param type             BEHAVIORAL or TECHNICAL
     * @param questionIndex    index of this question within the session
     */
    public String generateQuestion(String jobRole, List<String> rankedSkills, int difficulty,
                                    Question.QuestionType type, int questionIndex) {
        String skill = rankedSkills.isEmpty()
                ? "your core technical skills"
                : rankedSkills.get(questionIndex % rankedSkills.size());

        String prompt = buildPrompt(jobRole, skill, difficulty, type);

        if (llmApiUrl != null && !llmApiUrl.isBlank() && llmApiKey != null && !llmApiKey.isBlank()) {
            String llmResult = callLlm(prompt);
            if (llmResult != null && !llmResult.isBlank()) {
                return llmResult;
            }
        }

        return fallbackTemplate(skill, jobRole, difficulty, type, questionIndex);
    }

    private String buildPrompt(String jobRole, String skill, int difficulty, Question.QuestionType type) {
        return "You are an AI interviewer. Candidate is applying for the role of '" + jobRole + "'. "
                + "Their resume highlights the skill/topic '" + skill + "'. "
                + "Generate one " + type.name().toLowerCase() + " interview question at difficulty level "
                + difficulty + " out of 5 that specifically references this skill and role. "
                + "Return only the question text.";
    }

    /** Placeholder for a real LLM call. Wire this up to your provider of choice. */
    private String callLlm(String prompt) {
        // Example (pseudo-code):
        // return httpClient.post(llmApiUrl, headers(llmApiKey), body(prompt)).getText();
        return null;
    }

    private String fallbackTemplate(String skill, String jobRole, int difficulty,
                                     Question.QuestionType type, int idx) {
        String[] templates = (type == Question.QuestionType.TECHNICAL)
                ? TECHNICAL_TEMPLATES : BEHAVIORAL_TEMPLATES;
        String template = templates[idx % templates.length];
        String base = String.format(template, skill, jobRole);
        if (difficulty >= 4) {
            base += " Also discuss how you would scale or optimize your approach under real-world constraints.";
        } else if (difficulty <= 2) {
            base += " Keep your answer high-level and beginner-friendly.";
        }
        return base;
    }
}
