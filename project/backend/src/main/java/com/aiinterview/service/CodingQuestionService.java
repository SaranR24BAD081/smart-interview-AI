package com.aiinterview.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Section III-F: Coding Question Generator.
 * Supports Java, Python, C, C++, and JavaScript with real test cases (visible
 * and hidden)
 * matching LeetCode style.
 */
@Service
public class CodingQuestionService {

    public static final String JAVA = "java";
    public static final String PYTHON = "python";
    public static final String C = "c";
    public static final String CPP = "cpp";
    public static final String JAVASCRIPT = "javascript";

    /** A worked example shown under the problem, LeetCode-style. */
    public record Example(String input, String output) {
    }

    /** A test case with visible or hidden status */
    public record TestCase(int id, String name, String input, String expectedOutput, boolean hidden) {
    }

    /**
     * A generated problem with multi-language starter templates and test cases.
     */
    public record Problem(
            String title,
            String statement,
            List<Example> examples,
            List<String> constraints,
            List<String> requiredTokens,
            List<String> checkLabels,
            String language,
            String starterCode,
            Map<String, String> starters,
            List<TestCase> testCases,
            String conceptName,
            int difficulty) {
        // Backwards compatibility constructors
        public Problem(String title, String statement, List<Example> examples, List<String> constraints,
                List<String> requiredTokens, List<String> checkLabels,
                String language, String starterCode) {
            this(title, statement, examples, constraints, requiredTokens, checkLabels, language, starterCode,
                    Map.of(language, starterCode), List.of(), "", 1);
        }

        public Problem(String title, String statement, List<Example> examples, List<String> constraints,
                List<String> requiredTokens, List<String> checkLabels,
                String language, String starterCode,
                Map<String, String> starters, List<TestCase> testCases,
                String conceptName) {
            this(title, statement, examples, constraints, requiredTokens, checkLabels, language, starterCode,
                    starters, testCases, conceptName, 1);
        }
    }

    private record Concept(
            int difficulty,
            Set<String> keywords,
            Set<String> languages,
            String name,
            String title,
            String description,
            String params,
            String javaSig,
            String javaExtras,
            List<Example> examples,
            List<String> constraints,
            List<String> evidence,
            List<String> evidenceLabels) {
        boolean isFallback() {
            return keywords.isEmpty();
        }

        boolean supports(String language) {
            return languages.isEmpty() || languages.contains(language);
        }
    }

    private static final String TREE_NODE = "    static class TreeNode {\n" +
            "        int value;\n" +
            "        TreeNode left, right;\n" +
            "    }\n\n";

    private static final List<Concept> CATALOG = List.of(
            // ---- difficulty 1 ----
            new Concept(1, Set.of("array", "arrays", "list"), Set.of(), "sumArray", "Sum Array",
                    "returns the sum of all numbers in the array.",
                    "arr", "public static int sumArray(int[] arr)", "",
                    List.of(new Example("arr = [1, 2, 3]", "6"), new Example("arr = []", "0")),
                    List.of("0 <= arr.length <= 10^4", "-10^4 <= arr[i] <= 10^4"),
                    List.of("for|while|stream|reduce"),
                    List.of("Iterates over the array (loop, stream, or reduce) to accumulate a total")),
            new Concept(1, Set.of("string", "strings", "text"), Set.of(), "countVowels", "Count Vowels",
                    "returns how many vowels (a, e, i, o, u) appear in the string, case-insensitive.",
                    "s", "public static int countVowels(String s)", "",
                    List.of(new Example("s = \"Hello World\"", "3"), new Example("s = \"xyz\"", "0")),
                    List.of("0 <= s.length <= 10^4", "s consists of printable ASCII characters"),
                    List.of("for|while|stream|chars|filter|count"),
                    List.of("Iterates over the characters and checks each against a/e/i/o/u")),
            new Concept(1, Set.of(), Set.of(), "isEven", "Is Even",
                    "returns true if n is even and false otherwise.",
                    "n", "public static boolean isEven(int n)", "",
                    List.of(new Example("n = 4", "true"), new Example("n = 7", "false")),
                    List.of("-10^9 <= n <= 10^9"),
                    List.of("%|&"),
                    List.of("Uses modulo (%) or a bitwise check (&) to test divisibility by 2")),

            // ---- difficulty 2 ----
            new Concept(2, Set.of("string", "strings", "text"), Set.of(), "reverseString", "Reverse String",
                    "returns the reverse of the input string.",
                    "s", "public static String reverseString(String s)", "",
                    List.of(new Example("s = \"hello\"", "\"olleh\""), new Example("s = \"a\"", "\"a\"")),
                    List.of("0 <= s.length <= 10^4"),
                    List.of("for|while|reverse|[::-1]"),
                    List.of("Builds/returns the reversed string (loop, built-in reverse, or slicing)")),
            new Concept(2, Set.of("hashmap", "hashing", "collections", "dictionary", "dict"), Set.of(), "isAnagram",
                    "Valid Anagram",
                    "returns true if t is an anagram of s -- the same letters, with the same counts, in any order.",
                    "s, t", "public static boolean isAnagram(String s, String t)", "",
                    List.of(new Example("s = \"anagram\", t = \"nagaram\"", "true"),
                            new Example("s = \"rat\", t = \"car\"", "false")),
                    List.of("1 <= s.length, t.length <= 5*10^4", "s and t consist of lowercase English letters"),
                    List.of("Map|dict|{}|sort|Counter"),
                    List.of("Compares character counts (a hash map/Counter) or sorted forms of both strings")),
            new Concept(2, Set.of(), Set.of(), "countWords", "Count Words",
                    "returns the number of words in a sentence.",
                    "s", "public static int countWords(String s)", "",
                    List.of(new Example("s = \"the quick brown fox\"", "4"), new Example("s = \"\"", "0")),
                    List.of("0 <= s.length <= 10^4"),
                    List.of("split|for|while|stream"),
                    List.of("Splits the sentence into words (or iterates whitespace) to count them")),

            // ---- difficulty 3 ----
            new Concept(3, Set.of("stack", "stacks", "parentheses", "compiler", "compilers", "parsing"), Set.of(),
                    "isValid", "Valid Parentheses",
                    "returns true if the brackets in s -- made only of ( ) [ ] { } -- are closed in the correct order, false otherwise.",
                    "s", "public static boolean isValid(String s)", "",
                    List.of(new Example("s = \"()[]{}\"", "true"), new Example("s = \"(]\"", "false")),
                    List.of("1 <= s.length <= 10^4", "s consists only of the characters ()[]{}"),
                    List.of("push", "pop"),
                    List.of("Pushes each opening bracket onto a stack",
                            "Pops the stack and checks the match when a closing bracket is seen")),
            new Concept(3, Set.of("react", "frontend", "javascript", "js", "vue", "angular", "typescript"), Set.of(),
                    "longestCommonPrefix", "Longest Common Prefix",
                    "returns the longest common prefix shared by every string in strs, or an empty string if there is none.",
                    "strs", "public static String longestCommonPrefix(String[] strs)", "",
                    List.of(new Example("strs = [\"flower\", \"flow\", \"flight\"]", "\"fl\""),
                            new Example("strs = [\"dog\", \"racecar\", \"car\"]", "\"\"")),
                    List.of("1 <= strs.length <= 200", "0 <= strs[i].length <= 200"),
                    List.of("for|while", "charAt|[0]|startswith|startsWith"),
                    List.of("Compares characters across all the strings",
                            "Trims the candidate prefix down whenever a mismatch is found")),
            new Concept(3, Set.of(), Set.of(), "maxProfit", "Best Time to Buy and Sell Stock",
                    "returns the maximum profit from buying on one day and selling on a later day, or 0 if no profit is possible.",
                    "prices", "public static int maxProfit(int[] prices)", "",
                    List.of(new Example("prices = [7, 1, 5, 3, 6, 4]", "5"),
                            new Example("prices = [7, 6, 4, 3, 1]", "0")),
                    List.of("1 <= prices.length <= 10^5", "0 <= prices[i] <= 10^4"),
                    List.of("for|while", "min|Math.min"),
                    List.of("Iterates through the prices once",
                            "Tracks the lowest price seen so far to compute profit against")),

            // ---- difficulty 4 ----
            new Concept(4, Set.of("string", "strings", "text"), Set.of(), "isPalindrome", "Valid Palindrome",
                    "checks whether a string reads the same forwards and backwards, ignoring case and spaces.",
                    "s", "public static boolean isPalindrome(String s)", "",
                    List.of(new Example("s = \"Race car\"", "true"), new Example("s = \"hello\"", "false")),
                    List.of("0 <= s.length <= 10^4"),
                    List.of("for|while|reverse|[::-1]|charAt|toLowerCase|lower"),
                    List.of("Normalizes case/spacing and compares the string to its reverse")),
            new Concept(4, Set.of("oop", "design", "spring", "springboot"), Set.of(), "mergeIntervals",
                    "Merge Intervals",
                    "merges all overlapping [start, end] intervals and returns the merged intervals.",
                    "intervals", "public static int[][] mergeIntervals(int[][] intervals)", "",
                    List.of(new Example("intervals = [[1,3],[2,6],[8,10]]", "[[1,6],[8,10]]"),
                            new Example("intervals = [[1,4],[4,5]]", "[[1,5]]")),
                    List.of("1 <= intervals.length <= 10^4", "intervals[i].length == 2"),
                    List.of("sort", "for|while"),
                    List.of("Sorts the intervals by start value first",
                            "Walks the sorted intervals, merging overlaps")),

            // ---- difficulty 5 ----
            new Concept(5, Set.of("tree", "trees", "graph", "graphs"), Set.of(), "maxDepth",
                    "Maximum Depth of Binary Tree",
                    "returns the maximum depth of a binary tree, where each node has `value`, `left` and `right` fields (null/None for no child).",
                    "root", "public static int maxDepth(TreeNode root)", TREE_NODE,
                    List.of(new Example("root = [3,9,20,null,null,15,7]", "3"), new Example("root = []", "0")),
                    List.of("0 <= number of nodes <= 10^4"),
                    List.of("left", "right", "max"),
                    List.of("Recurses (or traverses) into the left subtree",
                            "Recurses (or traverses) into the right subtree",
                            "Combines both depths with a max/comparison")),
            new Concept(5, Set.of("data", "structures", "algorithms", "hashmap", "hashing", "collections"), Set.of(),
                    "twoSum", "Two Sum",
                    "returns the indices of the two numbers in the array that add up to target, in O(n) time using a hash map.",
                    "nums, target", "public static int[] twoSum(int[] nums, int target)", "",
                    List.of(new Example("nums = [2,7,11,15], target = 9", "[0, 1]"),
                            new Example("nums = [3,2,4], target = 6", "[1, 2]")),
                    List.of("2 <= nums.length <= 10^4", "Exactly one valid answer exists"),
                    List.of("for", "Map|dict|{}"),
                    List.of("Iterates over the array once",
                            "Uses a hash map/dictionary to look up complements in O(1)")));

    /**
     * Generates a batch of distinct problems (e.g. 3 programming problems)
     * spanning appropriate difficulty progression and matched against the
     * candidate's skills.
     */
    public List<Problem> generateProblems(int difficulty, String jobRole, List<String> rankedResumeSkills, int count,
            Set<String> excludeConceptNames) {
        String language = detectLanguage(jobRole, rankedResumeSkills);
        int level = clampDifficulty(difficulty);

        // Determine target difficulties for the problems to provide a realistic
        // progression
        // e.g. for count=3: [level - 1, level, level + 1] clamped to [1, 5]
        List<Integer> targetDiffs = new ArrayList<>();
        if (count == 3) {
            if (level <= 2) {
                targetDiffs = List.of(1, 2, 3);
            } else if (level == 3) {
                targetDiffs = List.of(2, 3, 4);
            } else {
                targetDiffs = List.of(3, 4, 5);
            }
        } else {
            for (int i = 0; i < count; i++) {
                targetDiffs.add(level);
            }
        }

        Set<String> usedConcepts = new HashSet<>(excludeConceptNames != null ? excludeConceptNames : Set.of());
        List<Problem> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int targetLevel = (i < targetDiffs.size()) ? targetDiffs.get(i) : level;
            List<Concept> candidates = new ArrayList<>();
            for (Concept c : CATALOG) {
                if (c.difficulty() == targetLevel && !usedConcepts.contains(c.name())) {
                    candidates.add(c);
                }
            }

            // If none found at exact target level, widen to other levels
            if (candidates.isEmpty()) {
                for (Concept c : CATALOG) {
                    if (!usedConcepts.contains(c.name())) {
                        candidates.add(c);
                    }
                }
            }

            // Fallback if all catalog concepts were somehow used
            if (candidates.isEmpty()) {
                candidates.add(CATALOG.get(i % CATALOG.size()));
            }

            Concept chosen = pickBestMatch(candidates, rankedResumeSkills, jobRole);
            usedConcepts.add(chosen.name());
            result.add(render(chosen, language, jobRole));
        }

        return result;
    }

    public List<Problem> generateProblems(int difficulty, String jobRole, List<String> rankedResumeSkills, int count) {
        return generateProblems(difficulty, jobRole, rankedResumeSkills, count, Set.of());
    }

    public Problem generateProblem(int difficulty, String jobRole, List<String> rankedResumeSkills) {
        List<Problem> problems = generateProblems(difficulty, jobRole, rankedResumeSkills, 1);
        return problems.isEmpty() ? render(CATALOG.get(0), detectLanguage(jobRole, rankedResumeSkills), jobRole)
                : problems.get(0);
    }

    public Problem generateProblem(int difficulty, String jobRole) {
        return generateProblem(difficulty, jobRole, List.of());
    }

    String detectLanguage(String jobRole, List<String> rankedResumeSkills) {
        String fromRole = languageOf(tokens(jobRole));
        if (fromRole != null)
            return fromRole;
        for (String skill : rankedResumeSkills) {
            String lang = languageOf(List.of(skill.toLowerCase(Locale.ROOT)));
            if (lang != null)
                return lang;
        }
        return JAVA;
    }

    private String languageOf(List<String> words) {
        for (String w : words) {
            switch (w) {
                case "java", "spring", "springboot", "hibernate", "jpa" -> {
                    return JAVA;
                }
                case "python", "django", "flask", "pandas", "numpy", "ai", "ml" -> {
                    return PYTHON;
                }
                case "c", "embedded", "firmware", "kernel" -> {
                    return C;
                }
                case "cpp", "c++" -> {
                    return CPP;
                }
                case "javascript", "js", "typescript", "react", "reactjs", "node", "nodejs", "angular", "vue",
                        "frontend" -> {
                    return JAVASCRIPT;
                }
                default -> {
                }
            }
        }
        return null;
    }

    private List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        if (text == null)
            return out;
        for (String t : text.toLowerCase(Locale.ROOT).split("[^a-z0-9+]+")) {
            if (!t.isBlank())
                out.add(t);
        }
        return out;
    }

    private Concept pickBestMatch(List<Concept> options, List<String> rankedResumeSkills, String jobRole) {
        List<String> walk = new ArrayList<>();
        for (String s : rankedResumeSkills)
            walk.add(s.toLowerCase(Locale.ROOT));
        walk.addAll(tokens(jobRole));

        for (String skill : walk) {
            for (Concept c : options) {
                if (c.keywords().contains(skill))
                    return c;
            }
        }
        return options.stream().filter(Concept::isFallback).findFirst().orElse(options.get(0));
    }

    private Problem render(Concept c, String language, String jobRole) {
        String snake = c.name().replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        String statement = "Implement the algorithm for `" + c.title() + "`. It " + c.description();

        Map<String, String> starters = buildStarters(c, snake);
        String defaultStarter = starters.getOrDefault(language, starters.get(JAVA));

        List<String> tokens = new ArrayList<>();
        tokens.add("return");
        tokens.addAll(c.evidence());

        List<String> labels = new ArrayList<>();
        labels.add("Includes a return statement");
        labels.addAll(c.evidenceLabels());

        if (jobRole != null && !jobRole.isBlank()) {
            statement += " (Role context: " + jobRole + ")";
        }

        List<TestCase> testCases = buildTestCases(c.name());

        return new Problem(
                c.title(),
                statement,
                c.examples(),
                c.constraints(),
                tokens,
                labels,
                language,
                defaultStarter,
                starters,
                testCases,
                c.name(),
                c.difficulty());
    }

    private Map<String, String> buildStarters(Concept c, String snake) {
        Map<String, String> map = new LinkedHashMap<>();

        // 1. JAVA
        String javaCode = "import java.util.*;\n\n"
                + "public class Solution {\n"
                + c.javaExtras()
                + "    " + c.javaSig() + " {\n"
                + "        // Write your code here\n"
                + "        \n"
                + "    }\n"
                + "}\n";
        map.put(JAVA, javaCode);

        // 2. PYTHON
        String pyCode = "def " + snake + "(" + c.params() + "):\n"
                + "    # Write your code here\n"
                + "    pass\n";
        map.put(PYTHON, pyCode);

        // 3. C
        String cCode = buildCStarter(c.name());
        map.put(C, cCode);

        // 4. C++
        String cppCode = buildCppStarter(c.name());
        map.put(CPP, cppCode);

        // 5. JAVASCRIPT
        String jsCode = "function " + c.name() + "(" + c.params() + ") {\n"
                + "    // Write your code here\n"
                + "}\n";
        map.put(JAVASCRIPT, jsCode);

        return map;
    }

    private String buildCStarter(String name) {
        return switch (name) {
            case "sumArray" -> """
                    #include <stdio.h>
                    #include <stdlib.h>

                    int sumArray(int* arr, int arrSize) {
                        // Write your code here
                        return 0;
                    }
                    """;
            case "countVowels" -> """
                    #include <stdio.h>
                    #include <string.h>
                    #include <ctype.h>

                    int countVowels(const char* s) {
                        // Write your code here
                        return 0;
                    }
                    """;
            case "isEven" -> """
                    #include <stdbool.h>

                    bool isEven(int n) {
                        // Write your code here
                        return false;
                    }
                    """;
            case "reverseString" -> """
                    #include <stdio.h>
                    #include <string.h>

                    char* reverseString(char* s) {
                        // Write your code here
                        return s;
                    }
                    """;
            case "isAnagram" -> """
                    #include <stdbool.h>
                    #include <string.h>

                    bool isAnagram(const char* s, const char* t) {
                        // Write your code here
                        return false;
                    }
                    """;
            case "countWords" -> """
                    #include <stdio.h>
                    #include <string.h>
                    #include <ctype.h>

                    int countWords(const char* s) {
                        // Write your code here
                        return 0;
                    }
                    """;
            case "isValid" -> """
                    #include <stdbool.h>
                    #include <string.h>

                    bool isValid(const char* s) {
                        // Write your code here
                        return false;
                    }
                    """;
            case "longestCommonPrefix" -> """
                    #include <stdio.h>
                    #include <string.h>
                    #include <stdlib.h>

                    char* longestCommonPrefix(char** strs, int strsSize) {
                        // Write your code here
                        return "";
                    }
                    """;
            case "maxProfit" -> """
                    #include <stdio.h>

                    int maxProfit(int* prices, int pricesSize) {
                        // Write your code here
                        return 0;
                    }
                    """;
            case "isPalindrome" -> """
                    #include <stdbool.h>
                    #include <string.h>
                    #include <ctype.h>

                    bool isPalindrome(const char* s) {
                        // Write your code here
                        return false;
                    }
                    """;
            case "mergeIntervals" ->
                """
                        #include <stdio.h>
                        #include <stdlib.h>

                        int** mergeIntervals(int** intervals, int intervalsSize, int* intervalsColSize, int* returnSize, int** returnColumnSizes) {
                            // Write your code here
                            *returnSize = 0;
                            return NULL;
                        }
                        """;
            case "maxDepth" -> """
                    #include <stdio.h>

                    struct TreeNode {
                        int val;
                        struct TreeNode *left;
                        struct TreeNode *right;
                    };

                    int maxDepth(struct TreeNode* root) {
                        // Write your code here
                        return 0;
                    }
                    """;
            case "twoSum" -> """
                    #include <stdio.h>
                    #include <stdlib.h>

                    int* twoSum(int* nums, int numsSize, int target, int* returnSize) {
                        // Write your code here
                        *returnSize = 0;
                        return NULL;
                    }
                    """;
            default -> """
                    #include <stdio.h>

                    int solve() {
                        return 0;
                    }
                    """;
        };
    }

    private String buildCppStarter(String name) {
        return switch (name) {
            case "sumArray" -> """
                    #include <iostream>
                    #include <vector>
                    #include <numeric>
                    using namespace std;

                    class Solution {
                    public:
                        int sumArray(vector<int>& arr) {
                            // Write your code here
                            return 0;
                        }
                    };
                    """;
            case "countVowels" -> """
                    #include <iostream>
                    #include <string>
                    using namespace std;

                    class Solution {
                    public:
                        int countVowels(string s) {
                            // Write your code here
                            return 0;
                        }
                    };
                    """;
            case "isEven" -> """
                    using namespace std;

                    class Solution {
                    public:
                        bool isEven(int n) {
                            // Write your code here
                            return false;
                        }
                    };
                    """;
            case "reverseString" -> """
                    #include <iostream>
                    #include <string>
                    #include <algorithm>
                    using namespace std;

                    class Solution {
                    public:
                        string reverseString(string s) {
                            // Write your code here
                            return s;
                        }
                    };
                    """;
            case "isAnagram" -> """
                    #include <iostream>
                    #include <string>
                    #include <vector>
                    #include <algorithm>
                    using namespace std;

                    class Solution {
                    public:
                        bool isAnagram(string s, string t) {
                            // Write your code here
                            return false;
                        }
                    };
                    """;
            case "countWords" -> """
                    #include <iostream>
                    #include <string>
                    #include <sstream>
                    using namespace std;

                    class Solution {
                    public:
                        int countWords(string s) {
                            // Write your code here
                            return 0;
                        }
                    };
                    """;
            case "isValid" -> """
                    #include <iostream>
                    #include <string>
                    #include <stack>
                    using namespace std;

                    class Solution {
                    public:
                        bool isValid(string s) {
                            // Write your code here
                            return false;
                        }
                    };
                    """;
            case "longestCommonPrefix" -> """
                    #include <iostream>
                    #include <vector>
                    #include <string>
                    using namespace std;

                    class Solution {
                    public:
                        string longestCommonPrefix(vector<string>& strs) {
                            // Write your code here
                            return "";
                        }
                    };
                    """;
            case "maxProfit" -> """
                    #include <iostream>
                    #include <vector>
                    #include <algorithm>
                    using namespace std;

                    class Solution {
                    public:
                        int maxProfit(vector<int>& prices) {
                            // Write your code here
                            return 0;
                        }
                    };
                    """;
            case "isPalindrome" -> """
                    #include <iostream>
                    #include <string>
                    #include <cctype>
                    using namespace std;

                    class Solution {
                    public:
                        bool isPalindrome(string s) {
                            // Write your code here
                            return false;
                        }
                    };
                    """;
            case "mergeIntervals" -> """
                    #include <iostream>
                    #include <vector>
                    #include <algorithm>
                    using namespace std;

                    class Solution {
                    public:
                        vector<vector<int>> mergeIntervals(vector<vector<int>>& intervals) {
                            // Write your code here
                            return {};
                        }
                    };
                    """;
            case "maxDepth" -> """
                    #include <iostream>
                    #include <algorithm>
                    using namespace std;

                    struct TreeNode {
                        int val;
                        TreeNode *left;
                        TreeNode *right;
                        TreeNode(int x) : val(x), left(NULL), right(NULL) {}
                    };

                    class Solution {
                    public:
                        int maxDepth(TreeNode* root) {
                            // Write your code here
                            return 0;
                        }
                    };
                    """;
            case "twoSum" -> """
                    #include <iostream>
                    #include <vector>
                    #include <unordered_map>
                    using namespace std;

                    class Solution {
                    public:
                        vector<int> twoSum(vector<int>& nums, int target) {
                            // Write your code here
                            return {};
                        }
                    };
                    """;
            default -> """
                    class Solution {
                    public:
                        int solve() {
                            return 0;
                        }
                    };
                    """;
        };
    }

    private List<TestCase> buildTestCases(String name) {
        return switch (name) {
            case "sumArray" -> List.of(
                    new TestCase(1, "Case 1", "arr = [1, 2, 3]", "6", false),
                    new TestCase(2, "Case 2", "arr = []", "0", false),
                    new TestCase(3, "Case 3", "arr = [10, -5, 20, -15]", "10", false),
                    new TestCase(4, "Case 4 (Hidden)", "arr = [-1000]", "-1000", true),
                    new TestCase(5, "Case 5 (Hidden)", "arr = [0, 0, 0, 0]", "0", true));
            case "countVowels" -> List.of(
                    new TestCase(1, "Case 1", "s = \"Hello World\"", "3", false),
                    new TestCase(2, "Case 2", "s = \"xyz\"", "0", false),
                    new TestCase(3, "Case 3", "s = \"AeIoU\"", "5", false),
                    new TestCase(4, "Case 4 (Hidden)", "s = \"\"", "0", true),
                    new TestCase(5, "Case 5 (Hidden)", "s = \"Programming in Python and C\"", "7", true));
            case "isEven" -> List.of(
                    new TestCase(1, "Case 1", "n = 4", "true", false),
                    new TestCase(2, "Case 2", "n = 7", "false", false),
                    new TestCase(3, "Case 3", "n = 0", "true", false),
                    new TestCase(4, "Case 4 (Hidden)", "n = -6", "true", true),
                    new TestCase(5, "Case 5 (Hidden)", "n = -11", "false", true));
            case "reverseString" -> List.of(
                    new TestCase(1, "Case 1", "s = \"hello\"", "\"olleh\"", false),
                    new TestCase(2, "Case 2", "s = \"a\"", "\"a\"", false),
                    new TestCase(3, "Case 3", "s = \"LeetCode\"", "\"edoCteeL\"", false),
                    new TestCase(4, "Case 4 (Hidden)", "s = \"\"", "\"\"", true),
                    new TestCase(5, "Case 5 (Hidden)", "s = \"racecar\"", "\"racecar\"", true));
            case "isAnagram" -> List.of(
                    new TestCase(1, "Case 1", "s = \"anagram\", t = \"nagaram\"", "true", false),
                    new TestCase(2, "Case 2", "s = \"rat\", t = \"car\"", "false", false),
                    new TestCase(3, "Case 3", "s = \"listen\", t = \"silent\"", "true", false),
                    new TestCase(4, "Case 4 (Hidden)", "s = \"a\", t = \"ab\"", "false", true),
                    new TestCase(5, "Case 5 (Hidden)", "s = \"triangle\", t = \"integral\"", "true", true));
            case "countWords" -> List.of(
                    new TestCase(1, "Case 1", "s = \"the quick brown fox\"", "4", false),
                    new TestCase(2, "Case 2", "s = \"\"", "0", false),
                    new TestCase(3, "Case 3", "s = \"one two three\"", "3", false),
                    new TestCase(4, "Case 4 (Hidden)", "s = \"hello\"", "1", true),
                    new TestCase(5, "Case 5 (Hidden)", "s = \"  leading and trailing spaces  \"", "4", true));
            case "isValid" -> List.of(
                    new TestCase(1, "Case 1", "s = \"()[]{}\"", "true", false),
                    new TestCase(2, "Case 2", "s = \"(]\"", "false", false),
                    new TestCase(3, "Case 3", "s = \"([{}])\"", "true", false),
                    new TestCase(4, "Case 4 (Hidden)", "s = \"(((\"", "false", true),
                    new TestCase(5, "Case 5 (Hidden)", "s = \"({[)]}\"", "false", true));
            case "longestCommonPrefix" -> List.of(
                    new TestCase(1, "Case 1", "strs = [\"flower\", \"flow\", \"flight\"]", "\"fl\"", false),
                    new TestCase(2, "Case 2", "strs = [\"dog\", \"racecar\", \"car\"]", "\"\"", false),
                    new TestCase(3, "Case 3", "strs = [\"interspecies\", \"interstellar\", \"interstate\"]",
                            "\"inters\"", false),
                    new TestCase(4, "Case 4 (Hidden)", "strs = [\"throne\", \"throne\"]", "\"throne\"", true),
                    new TestCase(5, "Case 5 (Hidden)", "strs = [\"a\"]", "\"a\"", true));
            case "maxProfit" -> List.of(
                    new TestCase(1, "Case 1", "prices = [7, 1, 5, 3, 6, 4]", "5", false),
                    new TestCase(2, "Case 2", "prices = [7, 6, 4, 3, 1]", "0", false),
                    new TestCase(3, "Case 3", "prices = [2, 4, 1]", "2", false),
                    new TestCase(4, "Case 4 (Hidden)", "prices = [1, 2, 3, 4, 5]", "4", true),
                    new TestCase(5, "Case 5 (Hidden)", "prices = [3, 3, 3]", "0", true));
            case "isPalindrome" -> List.of(
                    new TestCase(1, "Case 1", "s = \"Race car\"", "true", false),
                    new TestCase(2, "Case 2", "s = \"hello\"", "false", false),
                    new TestCase(3, "Case 3", "s = \"A man a plan a canal Panama\"", "true", false),
                    new TestCase(4, "Case 4 (Hidden)", "s = \" \"", "true", true),
                    new TestCase(5, "Case 5 (Hidden)", "s = \"0P\"", "false", true));
            case "mergeIntervals" -> List.of(
                    new TestCase(1, "Case 1", "intervals = [[1,3],[2,6],[8,10]]", "[[1,6],[8,10]]", false),
                    new TestCase(2, "Case 2", "intervals = [[1,4],[4,5]]", "[[1,5]]", false),
                    new TestCase(3, "Case 3", "intervals = [[1,4],[0,4]]", "[[0,4]]", false),
                    new TestCase(4, "Case 4 (Hidden)", "intervals = [[1,4],[2,3]]", "[[1,4]]", true),
                    new TestCase(5, "Case 5 (Hidden)", "intervals = [[6,8],[1,9],[2,4],[4,7]]", "[[1,9]]", true));
            case "maxDepth" -> List.of(
                    new TestCase(1, "Case 1", "root = [3,9,20,null,null,15,7]", "3", false),
                    new TestCase(2, "Case 2", "root = []", "0", false),
                    new TestCase(3, "Case 3", "root = [1,null,2]", "2", false),
                    new TestCase(4, "Case 4 (Hidden)", "root = [1]", "1", true),
                    new TestCase(5, "Case 5 (Hidden)", "root = [1,2,3,4,5]", "3", true));
            case "twoSum" -> List.of(
                    new TestCase(1, "Case 1", "nums = [2,7,11,15], target = 9", "[0, 1]", false),
                    new TestCase(2, "Case 2", "nums = [3,2,4], target = 6", "[1, 2]", false),
                    new TestCase(3, "Case 3", "nums = [3,3], target = 6", "[0, 1]", false),
                    new TestCase(4, "Case 4 (Hidden)", "nums = [-1, -2, -3, -4, -5], target = -8", "[2, 4]", true),
                    new TestCase(5, "Case 5 (Hidden)", "nums = [0, 4, 3, 0], target = 0", "[0, 3]", true));
            default -> List.of(
                    new TestCase(1, "Case 1", "input = 1", "1", false),
                    new TestCase(2, "Case 2", "input = 2", "2", false));
        };
    }

    private int clampDifficulty(int d) {
        return Math.max(1, Math.min(5, d));
    }
}
