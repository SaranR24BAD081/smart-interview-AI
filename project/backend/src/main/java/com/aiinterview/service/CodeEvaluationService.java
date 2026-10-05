package com.aiinterview.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Section III-F: automated assessment of submitted code.
 *
 * NOTE: Executing arbitrary, untrusted candidate code safely requires an
 * isolated sandbox (e.g. a locked-down Docker container or a judging
 * service such as Judge0) and is intentionally NOT done in-process here.
 * This service instead performs a structural/static check -- confirming the
 * submission defines the required function(s) and contains the expected
 * building blocks for the problem -- and reports a pass ratio, plus a
 * per-check breakdown (label + pass/fail) so the frontend can show a
 * LeetCode-style test result list. Swap {@link #runAgainstSandbox} in for
 * real execution once a sandbox runner is wired up in your deployment.
 */
@Service
public class CodeEvaluationService {

    public record CheckResult(String label, boolean passed) {}

    public static class EvaluationResult {
        public final int testsPassed;
        public final int testsTotal;
        public final double score;
        public final List<CheckResult> checks;

        public EvaluationResult(int testsPassed, int testsTotal, double score, List<CheckResult> checks) {
            this.testsPassed = testsPassed;
            this.testsTotal = testsTotal;
            this.score = score;
            this.checks = checks;
        }
    }

    /**
     * Removes the untouched starter-stub lines from the candidate's code so
     * only what they actually wrote is scored (otherwise an empty submission
     * would earn credit for tokens that appear in the stub itself, such as
     * the method name).
     */
    public String stripStarter(String code, String starterCode) {
        if (code == null || starterCode == null || starterCode.isBlank()) return code;
        java.util.Set<String> stubLines = new java.util.HashSet<>();
        for (String line : starterCode.split("\\R")) {
            if (!line.isBlank()) stubLines.add(line.strip());
        }
        StringBuilder kept = new StringBuilder();
        for (String line : code.split("\\R")) {
            if (!stubLines.contains(line.strip())) kept.append(line).append('\n');
        }
        return kept.toString();
    }

    /** Backward-compatible aggregate-only entry point (generic "Check N" labels). */
    public EvaluationResult evaluate(String code, List<String> expectedTokens) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < expectedTokens.size(); i++) labels.add("Check " + (i + 1));
        return evaluate(code, expectedTokens, labels);
    }

    /** Evaluates code against expected tokens, returning both the aggregate score and a per-check breakdown. */
    public EvaluationResult evaluate(String code, List<String> expectedTokens, List<String> checkLabels) {
        List<CheckResult> checks = new ArrayList<>();
        if (code == null || code.isBlank() || expectedTokens.isEmpty()) {
            for (int i = 0; i < expectedTokens.size(); i++) {
                checks.add(new CheckResult(labelFor(checkLabels, i), false));
            }
            return new EvaluationResult(0, Math.max(1, expectedTokens.size()), 0.0, checks);
        }

        int passed = 0;
        for (int i = 0; i < expectedTokens.size(); i++) {
            // "a|b|c" means any one alternative satisfies the check, so valid
            // solutions written in different styles (loop vs. stream, etc.)
            // are not penalised.
            boolean ok = false;
            for (String alternative : expectedTokens.get(i).split("\\|")) {
                if (!alternative.isEmpty() && code.contains(alternative)) {
                    ok = true;
                    break;
                }
            }
            if (ok) passed++;
            checks.add(new CheckResult(labelFor(checkLabels, i), ok));
        }
        double score = (double) passed / expectedTokens.size();
        return new EvaluationResult(passed, expectedTokens.size(), score, checks);
    }

    private String labelFor(List<String> labels, int i) {
        return (labels != null && i < labels.size()) ? labels.get(i) : "Check " + (i + 1);
    }

    /**
     * Hook for real sandboxed execution. Left unimplemented by design --
     * integrate with your judging infrastructure of choice here.
     */
    public EvaluationResult runAgainstSandbox(String code, String language, List<String> testCases) {
        throw new UnsupportedOperationException(
            "Wire this up to an isolated code-execution sandbox (e.g. Judge0) in your deployment.");
    }
}
