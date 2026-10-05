package com.aiinterview.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Section III-A: Resume Analyzer.
 *
 * Performs lightweight tokenization / skill extraction from resume text and
 * computes a resume-to-role relevance score using cosine similarity over a
 * bag-of-words vector space (a simple, dependency-free stand-in for the
 * transformer-embedding similarity described by Eq. 1 in the paper:
 *
 *     S(r, j) = (Er . Ej) / (||Er|| * ||Ej||)
 *
 * Swap {@link #embed(String)} for a call to a real embedding model/API to
 * upgrade this to the full semantic version without changing any callers.
 */
@Service
public class ResumeAnalyzerService {

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z+.#]{1,}");

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "a", "an", "of", "to", "in", "on", "is", "are",
            "as", "at", "by", "be", "or", "this", "that", "it", "was", "were", "i", "we"
    );

    /**
     * Curated technical/professional skill vocabulary. Only tokens found here
     * are surfaced by {@link #extractSkills} / {@link #rankedSkillsForRole}
     * for use in question generation and coding-problem selection.
     *
     * Without this allowlist, any non-stopword in the resume (a candidate's
     * name, city, "college", etc.) would get treated as a "skill" and end up
     * verbatim in a generated question (e.g. "applied Coimbatore"). The
     * broader, unfiltered {@link #tokenize} is still used as-is for the
     * cosine-similarity relevance score, where treating the resume as a
     * general bag-of-words is the intended behavior.
     */
    private static final Set<String> KNOWN_SKILLS = Set.of(
            "java", "python", "javascript", "typescript", "react", "reactjs", "angular", "vue",
            "node", "nodejs", "express", "spring", "springboot", "django", "flask", "dotnet",
            "sql", "mysql", "postgresql", "postgres", "mongodb", "nosql", "redis", "sqlite",
            "aws", "azure", "gcp", "docker", "kubernetes", "git", "github", "gitlab",
            "html", "css", "sass", "tailwind", "bootstrap", "webpack",
            "c", "c++", "cpp", "c#", "csharp", "php", "ruby", "golang", "go", "rust", "swift", "kotlin", "scala",
            "machine", "learning", "ai", "ml", "nlp", "tensorflow", "pytorch", "pandas", "numpy",
            "data", "structures", "algorithms", "algorithm", "oop",
            "rest", "restful", "api", "apis", "graphql", "grpc", "microservices", "websocket",
            "redux", "testing", "junit", "selenium", "jest", "cypress",
            "agile", "scrum", "jira", "confluence",
            "linux", "bash", "shell", "devops", "cicd", "jenkins", "terraform", "ansible",
            "hadoop", "spark", "kafka", "rabbitmq", "elasticsearch", "nginx", "apache",
            "android", "ios", "flutter", "unity",
            "blockchain", "solidity", "cybersecurity", "networking",
            "excel", "tableau", "powerbi", "figma",
            "hibernate", "jpa", "oauth", "jwt", "firebase",
            "frontend", "backend", "fullstack"
    );

    /** Extract candidate skills/keywords from free-text resume content. */
    public Set<String> extractSkills(String resumeText) {
        if (resumeText == null) return Set.of();
        Set<String> tokens = tokenize(resumeText);
        tokens.retainAll(KNOWN_SKILLS);
        return tokens;
    }

    /** Eq. 1: cosine similarity between resume-term-vector and role-term-vector. */
    public double relevanceScore(String resumeText, String jobRole, String jobRoleKeywords) {
        Map<String, Integer> resumeVec = termFrequency(resumeText);
        Map<String, Integer> roleVec = termFrequency(jobRole + " " + jobRoleKeywords);
        return cosineSimilarity(resumeVec, roleVec);
    }

    private Set<String> tokenize(String text) {
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        Set<String> tokens = new HashSet<>();
        while (m.find()) {
            String tok = m.group();
            if (!STOPWORDS.contains(tok) && tok.length() > 1) tokens.add(tok);
        }
        return tokens;
    }

    private Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        for (String tok : tokenize(text)) {
            freq.merge(tok, 1, Integer::sum);
        }
        return freq;
    }

    private double cosineSimilarity(Map<String, Integer> a, Map<String, Integer> b) {
        Set<String> keys = new HashSet<>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());

        double dot = 0, normA = 0, normB = 0;
        for (String k : keys) {
            int av = a.getOrDefault(k, 0);
            int bv = b.getOrDefault(k, 0);
            dot += (double) av * bv;
            normA += (double) av * av;
            normB += (double) bv * bv;
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Ranked list of resume skills, most relevant to the role first. */
    public List<String> rankedSkillsForRole(String resumeText, String jobRole) {
        Set<String> skills = extractSkills(resumeText);
        Set<String> roleTerms = extractSkills(jobRole);
        return skills.stream()
                .sorted((s1, s2) -> Boolean.compare(roleTerms.contains(s2), roleTerms.contains(s1)))
                .collect(Collectors.toList());
    }
}
