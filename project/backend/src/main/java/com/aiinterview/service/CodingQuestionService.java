package com.aiinterview.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Section III-F: Coding Question Generator (the "+ Integrated Code Editor"
 * half lives on the frontend, see CodingRound.jsx, styled as a LeetCode-style
 * split view: problem/description pane on the left, editor + test results on
 * the right).
 *
 * Picks a role/resume-aligned coding problem at the requested difficulty AND
 * in the candidate's own language: a "java developer" gets Java problems
 * (a Java method signature inside a `Solution` class), a Python developer
 * gets Python, a frontend/JS developer gets JavaScript. The language is
 * detected from the job role first, then from the ranked resume skills
 * (Section III-A). Within that language, the problem topic is chosen by
 * matching resume skills against each problem's topic keywords, with a
 * topic-neutral fallback at every difficulty level.
 *
 * Swap {@link #CATALOG} for an LLM call to produce fully novel, per-candidate
 * problems without changing any callers.
 */
@Service
public class CodingQuestionService {

    private final Random random = new Random();

    public static final String JAVA = "java";
    public static final String PYTHON = "python";
    public static final String JAVASCRIPT = "javascript";
    public static final String C = "c";

    /** A worked example shown under the problem, LeetCode-style. */
    public record Example(String input, String output) {}

    /**
     * A generated problem.
     *
     * @param requiredTokens checks used by CodeEvaluationService. A token may
     *                       list alternatives separated by "|" (any one passes),
     *                       so valid solutions written in different styles
     *                       are not penalised.
     * @param checkLabels    human-readable description of each requiredTokens
     *                       entry, same order/length, shown in the Result panel.
     */
    public record Problem(String title, String statement, List<Example> examples, List<String> constraints,
                           List<String> requiredTokens, List<String> checkLabels,
                           String language, String starterCode) {}

    /**
     * One problem idea that can be rendered in any supported language.
     *
     * @param params      parameter list for Python/JavaScript signatures
     * @param javaSig     full Java method signature, or "" if not applicable in Java
     * @param evidence    body checks (alternatives split by "|") the solution should contain
     * @param evidenceLabels human-readable label for each entry in evidence, same order/length
     * @param languages   restricts the concept to these languages; empty = all
     */
    private record Concept(int difficulty, Set<String> keywords, Set<String> languages,
                           String name, String title, String description, String params, String javaSig,
                           String javaExtras, List<Example> examples, List<String> constraints,
                           List<String> evidence, List<String> evidenceLabels) {
        boolean isFallback() {
            return keywords.isEmpty();
        }

        boolean supports(String language) {
            return languages.isEmpty() || languages.contains(language);
        }
    }

    private static final String TREE_NODE =
            "    static class TreeNode {\n" +
            "        int value;\n" +
            "        TreeNode left, right;\n" +
            "    }\n\n";

    private static final List<Concept> CATALOG = List.of(

        // ══════════════════════════════════════════════════
        // DIFFICULTY 1 — Very Easy
        // ══════════════════════════════════════════════════

        // General fallbacks (no keyword restriction)
        new Concept(1, Set.of(), Set.of(), "isEven", "Is Even",
            "returns true if n is even and false otherwise.",
            "n", "public static boolean isEven(int n)", "",
            List.of(new Example("n = 4", "true"), new Example("n = 7", "false")),
            List.of("-10^9 <= n <= 10^9"),
            List.of("%|&"),
            List.of("Uses modulo (%) or a bitwise check (&) to test divisibility by 2")),

        new Concept(1, Set.of(), Set.of(), "absolute", "Absolute Value",
            "returns the absolute value of n without using any built-in abs() function.",
            "n", "public static int absolute(int n)", "",
            List.of(new Example("n = -5", "5"), new Example("n = 3", "3"), new Example("n = 0", "0")),
            List.of("-10^9 <= n <= 10^9"),
            List.of("if|<|>|ternary|?"),
            List.of("Uses a conditional/ternary to negate n when it is negative")),

        new Concept(1, Set.of(), Set.of(), "maxOfThree", "Maximum of Three Numbers",
            "returns the largest of three integers a, b, and c.",
            "a, b, c", "public static int maxOfThree(int a, int b, int c)", "",
            List.of(new Example("a=1, b=3, c=2", "3"), new Example("a=-1, b=-2, c=-3", "-1")),
            List.of("-10^9 <= a, b, c <= 10^9"),
            List.of("if|>|max|Math.max"),
            List.of("Compares all three values and returns the greatest")),

        // Arrays / Lists (difficulty 1)
        new Concept(1, Set.of("array", "arrays", "list", "collections"), Set.of(), "sumArray", "Sum Array",
            "returns the sum of all numbers in the array.",
            "arr", "public static int sumArray(int[] arr)", "",
            List.of(new Example("arr = [1, 2, 3]", "6"), new Example("arr = []", "0")),
            List.of("0 <= arr.length <= 10^4", "-10^4 <= arr[i] <= 10^4"),
            List.of("for|while|stream|reduce"),
            List.of("Iterates over the array (loop, stream, or reduce) to accumulate a total")),

        new Concept(1, Set.of("array", "arrays", "list", "data", "structures"), Set.of(), "findMax", "Find Maximum Element",
            "returns the maximum value in a non-empty integer array.",
            "arr", "public static int findMax(int[] arr)", "",
            List.of(new Example("arr = [3, 1, 4, 1, 5]", "5"), new Example("arr = [-2, -1]", "-1")),
            List.of("1 <= arr.length <= 10^4"),
            List.of("for|while|max|Math.max|stream"),
            List.of("Iterates the array tracking the largest element seen so far")),

        // Strings (difficulty 1)
        new Concept(1, Set.of("string", "strings", "text", "parsing"), Set.of(), "countVowels", "Count Vowels",
            "returns how many vowels (a, e, i, o, u) appear in the string, case-insensitive.",
            "s", "public static int countVowels(String s)", "",
            List.of(new Example("s = \"Hello World\"", "3"), new Example("s = \"xyz\"", "0")),
            List.of("0 <= s.length <= 10^4"),
            List.of("for|while|stream|chars|filter|count"),
            List.of("Iterates over the characters and checks each against a/e/i/o/u")),

        new Concept(1, Set.of("string", "strings", "text"), Set.of(), "isPalindromSimple", "Simple Palindrome Check",
            "returns true if the string s reads the same forwards and backwards (case-sensitive, no spaces to ignore).",
            "s", "public static boolean isPalindromSimple(String s)", "",
            List.of(new Example("s = \"racecar\"", "true"), new Example("s = \"hello\"", "false")),
            List.of("1 <= s.length <= 10^4"),
            List.of("for|while|reverse|equals|charAt"),
            List.of("Compares the string to its reverse (or uses two pointers)")),

        // Python/ML/Data (difficulty 1)
        new Concept(1, Set.of("python", "pandas", "numpy", "data", "ml", "machine", "learning", "ai"), Set.of(), "listAverage", "List Average",
            "returns the arithmetic mean of a list of numbers, or 0.0 if the list is empty.",
            "nums", "public static double listAverage(int[] nums)", "",
            List.of(new Example("nums = [1, 2, 3, 4]", "2.5"), new Example("nums = []", "0.0")),
            List.of("0 <= nums.length <= 10^4"),
            List.of("for|while|sum|stream|reduce"),
            List.of("Sums all elements and divides by the count")),

        // JS/Frontend (difficulty 1)
        new Concept(1, Set.of("javascript", "js", "typescript", "react", "reactjs", "frontend", "node", "nodejs", "vue", "angular"), Set.of(), "repeatString", "Repeat String",
            "returns the string s repeated n times (return an empty string when n <= 0).",
            "s, n", "public static String repeatString(String s, int n)", "",
            List.of(new Example("s = \"ab\", n = 3", "\"ababab\""), new Example("s = \"x\", n = 0", "\"\"")),
            List.of("0 <= n <= 1000", "0 <= s.length <= 100"),
            List.of("for|while|repeat|append|+"),
            List.of("Builds the result by repeating/concatenating the string n times")),

        // Java (difficulty 1)
        new Concept(1, Set.of("java", "spring", "springboot", "jpa", "hibernate"), Set.of(), "factorial", "Factorial",
            "returns the factorial of n (n!). You may assume n >= 0 and the result fits in a long.",
            "n", "public static long factorial(int n)", "",
            List.of(new Example("n = 5", "120"), new Example("n = 0", "1")),
            List.of("0 <= n <= 20"),
            List.of("for|while|factorial|*=|return"),
            List.of("Computes the product of all integers from 1 to n (or recurses)")),

        // ══════════════════════════════════════════════════
        // DIFFICULTY 2 — Easy
        // ══════════════════════════════════════════════════

        // General fallbacks
        new Concept(2, Set.of(), Set.of(), "countWords", "Count Words",
            "returns the number of words in a sentence (words are separated by one or more spaces).",
            "s", "public static int countWords(String s)", "",
            List.of(new Example("s = \"the quick brown fox\"", "4"), new Example("s = \"\"", "0")),
            List.of("0 <= s.length <= 10^4"),
            List.of("split|for|while|stream"),
            List.of("Splits the sentence into words (or iterates whitespace) to count them")),

        new Concept(2, Set.of(), Set.of(), "reverseString", "Reverse String",
            "returns the reverse of the input string.",
            "s", "public static String reverseString(String s)", "",
            List.of(new Example("s = \"hello\"", "\"olleh\""), new Example("s = \"a\"", "\"a\"")),
            List.of("0 <= s.length <= 10^4"),
            List.of("for|while|reverse|[::-1]"),
            List.of("Builds/returns the reversed string (loop, built-in reverse, or slicing)")),

        new Concept(2, Set.of(), Set.of(), "removeDuplicates", "Remove Duplicates from Sorted Array",
            "removes duplicates from a sorted integer array in-place and returns the new length. Elements beyond the new length are ignored.",
            "nums", "public static int removeDuplicates(int[] nums)", "",
            List.of(new Example("nums = [1,1,2]", "2, nums = [1,2,...]"), new Example("nums = [0,0,1,1,2]", "3")),
            List.of("0 <= nums.length <= 3*10^4", "nums is sorted in non-decreasing order"),
            List.of("for|while", "!=|=="),
            List.of("Walks the sorted array with two pointers", "Copies each new unique element forward")),

        // Arrays / Collections (difficulty 2)
        new Concept(2, Set.of("array", "arrays", "list", "collections", "data", "structures"), Set.of(), "rotateArray", "Rotate Array",
            "rotates the array to the right by k steps in-place (modify the array, do not return anything).",
            "nums, k", "public static void rotateArray(int[] nums, int k)", "",
            List.of(new Example("nums = [1,2,3,4,5,6,7], k = 3", "[5,6,7,1,2,3,4]"),
                    new Example("nums = [-1,-100,3,99], k = 2", "[3,99,-1,-100]")),
            List.of("1 <= nums.length <= 10^5", "0 <= k <= 10^5"),
            List.of("for|while|reverse|%|mod"),
            List.of("Uses reversal or cyclic replacement to rotate in O(1) space")),

        // Strings (difficulty 2)
        new Concept(2, Set.of("string", "strings", "text", "parsing"), Set.of(), "isAnagram", "Valid Anagram",
            "returns true if t is an anagram of s -- the same letters, with the same counts, in any order.",
            "s, t", "public static boolean isAnagram(String s, String t)", "",
            List.of(new Example("s = \"anagram\", t = \"nagaram\"", "true"), new Example("s = \"rat\", t = \"car\"", "false")),
            List.of("1 <= s.length, t.length <= 5*10^4", "s and t consist of lowercase English letters"),
            List.of("Map|dict|{}|sort|Counter"),
            List.of("Compares character counts (a hash map/Counter) or sorted forms of both strings")),

        new Concept(2, Set.of("string", "strings", "text"), Set.of(), "titleCase", "Title Case",
            "converts every word in sentence to Title Case (first letter uppercase, the rest lowercase).",
            "sentence", "public static String titleCase(String sentence)", "",
            List.of(new Example("sentence = \"hello world\"", "\"Hello World\""),
                    new Example("sentence = \"tHE QUICK fox\"", "\"The Quick Fox\"")),
            List.of("1 <= sentence.length <= 10^4"),
            List.of("split|for|while|toUpperCase|capitalize|substring"),
            List.of("Splits into words and capitalises the first letter of each")),

        // Algorithms (difficulty 2)
        new Concept(2, Set.of("algorithm", "algorithms", "recursion", "dynamic", "dp", "math"), Set.of(), "fibonacci", "Fibonacci Number",
            "returns the n-th Fibonacci number where fib(0)=0, fib(1)=1, and fib(n) = fib(n-1) + fib(n-2).",
            "n", "public static int fibonacci(int n)", "",
            List.of(new Example("n = 0", "0"), new Example("n = 5", "5"), new Example("n = 10", "55")),
            List.of("0 <= n <= 30"),
            List.of("for|while|fibonacci|fib|return", "if|==|<="),
            List.of("Uses iteration or recursion to compute the n-th term",
                    "Handles the base cases (n=0 or n=1) correctly")),

        new Concept(2, Set.of("algorithm", "algorithms", "math", "data", "structures"), Set.of(), "isPrime", "Is Prime",
            "returns true if n is a prime number, false otherwise.",
            "n", "public static boolean isPrime(int n)", "",
            List.of(new Example("n = 7", "true"), new Example("n = 12", "false"), new Example("n = 1", "false")),
            List.of("0 <= n <= 10^6"),
            List.of("for|while|sqrt|Math.sqrt|%"),
            List.of("Checks divisibility up to sqrt(n) to determine primality")),

        // Java/Spring (difficulty 2)
        new Concept(2, Set.of("java", "spring", "springboot", "jpa", "hibernate", "collections"), Set.of(), "firstUniqChar", "First Unique Character in a String",
            "returns the index of the first non-repeating character in string s, or -1 if every character repeats.",
            "s", "public static int firstUniqChar(String s)", "",
            List.of(new Example("s = \"leetcode\"", "0"), new Example("s = \"loveleet\"", "2"), new Example("s = \"aabb\"", "-1")),
            List.of("1 <= s.length <= 10^5", "s consists of only lowercase English letters"),
            List.of("for|while|Map|LinkedHashMap|int[]|freq", "return", "-1"),
            List.of("Counts character frequencies (HashMap, array, or similar)",
                    "Includes a return statement for the found index",
                    "Returns -1 when no unique character exists")),

        // Python/ML (difficulty 2)
        new Concept(2, Set.of("python", "pandas", "numpy", "data", "ml", "machine", "learning", "ai", "nlp"), Set.of(), "countOccurrences", "Count Occurrences",
            "counts how many times each word appears in a list of words and returns a dictionary/map of word → count.",
            "words", "public static Map<String,Integer> countOccurrences(String[] words)", "",
            List.of(new Example("words = [\"apple\",\"banana\",\"apple\"]", "{apple=2, banana=1}"),
                    new Example("words = []", "{}")),
            List.of("0 <= words.length <= 10^4"),
            List.of("for|while|Map|HashMap|put|getOrDefault|merge"),
            List.of("Uses a HashMap/dictionary to count each word's frequency")),

        // JS/Frontend (difficulty 2)
        new Concept(2, Set.of("javascript", "js", "typescript", "react", "reactjs", "frontend", "node", "nodejs", "vue", "angular"), Set.of(), "flattenArray", "Flatten One Level",
            "flattens a 2D array (array of arrays) by one level and returns the resulting 1D array.",
            "arr", "public static int[] flattenArray(int[][] arr)", "",
            List.of(new Example("arr = [[1,2],[3,4],[5]]", "[1,2,3,4,5]"),
                    new Example("arr = []", "[]")),
            List.of("0 <= arr.length <= 1000", "0 <= arr[i].length <= 1000"),
            List.of("for|while|flat|concat|addAll|stream"),
            List.of("Iterates over each sub-array and appends its elements to the result")),

        // C/Embedded (difficulty 2)
        new Concept(2, Set.of("c", "embedded", "firmware", "microcontroller", "arduino", "cpp", "c++"), Set.of(), "reverseArray", "Reverse an Array In-Place",
            "reverses the given integer array in-place without using any extra array.",
            "arr", "public static void reverseArray(int[] arr)", "",
            List.of(new Example("arr = [1,2,3,4,5]", "[5,4,3,2,1]"),
                    new Example("arr = [1,2]", "[2,1]")),
            List.of("1 <= arr.length <= 10^5"),
            List.of("for|while|swap|temp"),
            List.of("Swaps elements from both ends moving towards the centre")),

        // ══════════════════════════════════════════════════
        // DIFFICULTY 3 — Medium
        // ══════════════════════════════════════════════════

        // General fallbacks
        new Concept(3, Set.of(), Set.of(), "maxProfit", "Best Time to Buy and Sell Stock",
            "returns the maximum profit from buying on one day and selling on a later day, or 0 if no profit is possible.",
            "prices", "public static int maxProfit(int[] prices)", "",
            List.of(new Example("prices = [7, 1, 5, 3, 6, 4]", "5"), new Example("prices = [7, 6, 4, 3, 1]", "0")),
            List.of("1 <= prices.length <= 10^5", "0 <= prices[i] <= 10^4"),
            List.of("for|while", "min|Math.min"),
            List.of("Iterates through the prices once", "Tracks the lowest price seen so far to compute profit against")),

        new Concept(3, Set.of(), Set.of(), "moveZeroes", "Move Zeroes",
            "moves all 0s to the end of the array in-place while keeping the relative order of the non-zero elements.",
            "nums", "public static void moveZeroes(int[] nums)", "",
            List.of(new Example("nums = [0,1,0,3,12]", "[1,3,12,0,0]"),
                    new Example("nums = [0,0,1]", "[1,0,0]")),
            List.of("1 <= nums.length <= 10^4"),
            List.of("for|while", "!=|0|swap|insert"),
            List.of("Iterates with a write pointer for non-zero values",
                    "Fills remaining positions with zeros")),

        // Data Structures (difficulty 3)
        new Concept(3, Set.of("stack", "stacks", "queue", "deque", "data", "structures", "algorithm", "algorithms"), Set.of(), "isValid", "Valid Parentheses",
            "returns true if the brackets in s -- made only of ( ) [ ] { } -- are closed in the correct order, false otherwise.",
            "s", "public static boolean isValid(String s)", "",
            List.of(new Example("s = \"()[]{}\"", "true"), new Example("s = \"(]\"", "false")),
            List.of("1 <= s.length <= 10^4", "s consists only of the characters ()[]{}"),
            List.of("push|Stack|Deque", "pop"),
            List.of("Pushes each opening bracket onto a stack", "Pops the stack and checks the match when a closing bracket is seen")),

        new Concept(3, Set.of("hashmap", "hashing", "collections", "data", "structures", "algorithm", "algorithms"), Set.of(), "groupAnagrams", "Group Anagrams",
            "groups a list of strings so that anagrams are together and returns the list of groups.",
            "strs", "public static List<List<String>> groupAnagrams(String[] strs)", "",
            List.of(new Example("strs = [\"eat\",\"tea\",\"tan\",\"ate\",\"nat\",\"bat\"]", "[[\"eat\",\"tea\",\"ate\"],[\"tan\",\"nat\"],[\"bat\"]]")),
            List.of("1 <= strs.length <= 10^4", "0 <= strs[i].length <= 100"),
            List.of("Map|HashMap|dict", "sort|Arrays.sort|sorted"),
            List.of("Uses a HashMap keyed on the sorted form (or character count) of each word",
                    "Groups words with the same sorted key")),

        // Java/Spring (difficulty 3)
        new Concept(3, Set.of("java", "spring", "springboot", "jpa", "hibernate", "algorithm", "algorithms", "search", "sorting"), Set.of(), "binarySearch", "Binary Search",
            "searches a sorted integer array for target and returns its index, or -1 if not found. Must run in O(log n).",
            "nums, target", "public static int binarySearch(int[] nums, int target)", "",
            List.of(new Example("nums = [-1,0,3,5,9,12], target = 9", "4"),
                    new Example("nums = [-1,0,3,5,9,12], target = 2", "-1")),
            List.of("1 <= nums.length <= 10^4", "nums is sorted in ascending order"),
            List.of("while|for", "mid|middle|lo+hi|low+high", "-1"),
            List.of("Uses a loop to halve the search space each iteration",
                    "Computes a midpoint and compares it to the target",
                    "Returns -1 when the target is not found")),

        new Concept(3, Set.of("java", "spring", "springboot", "oop", "object", "oriented", "design"), Set.of(), "longestCommonPrefix", "Longest Common Prefix",
            "returns the longest common prefix shared by every string in strs, or an empty string if there is none.",
            "strs", "public static String longestCommonPrefix(String[] strs)", "",
            List.of(new Example("strs = [\"flower\",\"flow\",\"flight\"]", "\"fl\""),
                    new Example("strs = [\"dog\",\"racecar\",\"car\"]", "\"\"")),
            List.of("1 <= strs.length <= 200", "0 <= strs[i].length <= 200"),
            List.of("for|while", "charAt|[0]|startswith|startsWith"),
            List.of("Compares characters across all the strings",
                    "Trims the candidate prefix down whenever a mismatch is found")),

        // Python/ML (difficulty 3)
        new Concept(3, Set.of("python", "pandas", "numpy", "data", "ml", "machine", "learning", "ai", "nlp", "algorithm", "algorithms"), Set.of(), "slidingWindowMax", "Sliding Window Maximum Sum",
            "given an integer array and window size k, returns an array of the maximum sum for each window of k consecutive elements.",
            "nums, k", "public static int[] slidingWindowMax(int[] nums, int k)", "",
            List.of(new Example("nums = [1,3,-1,-3,5,3,6,7], k = 3", "[3,3,5,5,6,7]"),
                    new Example("nums = [1,2,3,4,5], k = 2", "[3,5,7,9]")),
            List.of("1 <= k <= nums.length <= 10^5"),
            List.of("for|while", "sum|window|deque|Deque"),
            List.of("Maintains a running window sum or a monotonic deque",
                    "Slides the window by subtracting the outgoing element and adding the incoming one")),

        // JS/Frontend (difficulty 3)
        new Concept(3, Set.of("javascript", "js", "typescript", "react", "reactjs", "frontend", "vue", "angular", "node", "nodejs"), Set.of(), "debounce", "Implement Debounce",
            "returns a debounced version of fn that delays invoking fn until after wait milliseconds have elapsed since the last call. " +
            "Implement the logic using basic language constructs (simulate with a counter or flag, not real timers).",
            "fn, wait", "public static Runnable debounce(Runnable fn, int wait)", "",
            List.of(new Example("Called 5 times quickly, wait=200ms", "fn runs only once after the last call"),
                    new Example("Called once, wait=100ms", "fn runs once after 100ms")),
            List.of("0 <= wait <= 10^4"),
            List.of("timer|setTimeout|cancel|flag|lastCall|delay"),
            List.of("Cancels any pending scheduled invocation on each new call",
                    "Schedules fn to fire only after the wait period with no further calls")),

        // C/Embedded (difficulty 3)
        new Concept(3, Set.of("c", "embedded", "firmware", "microcontroller", "arduino", "cpp", "c++"), Set.of(), "countBits", "Count Set Bits",
            "returns the number of 1-bits (set bits) in the binary representation of a non-negative integer n.",
            "n", "public static int countBits(int n)", "",
            List.of(new Example("n = 11  (binary 1011)", "3"), new Example("n = 0", "0"), new Example("n = 7", "3")),
            List.of("0 <= n <= 2^31 - 1"),
            List.of("&|>>|>>>|bitCount|while|for"),
            List.of("Uses bitwise AND with 1 and right-shifting (or Integer.bitCount) to count set bits")),

        // ══════════════════════════════════════════════════
        // DIFFICULTY 4 — Medium-Hard
        // ══════════════════════════════════════════════════

        // General fallbacks
        new Concept(4, Set.of(), Set.of(), "isPalindrome", "Valid Palindrome",
            "checks whether a string reads the same forwards and backwards, ignoring case and non-alphanumeric characters.",
            "s", "public static boolean isPalindrome(String s)", "",
            List.of(new Example("s = \"A man, a plan, a canal: Panama\"", "true"), new Example("s = \"race a car\"", "false")),
            List.of("1 <= s.length <= 2*10^5"),
            List.of("for|while|toLowerCase|lower|replaceAll|isLetterOrDigit", "charAt|pointer"),
            List.of("Strips/ignores non-alphanumeric characters and normalises case",
                    "Uses two pointers or compares to the filtered reverse")),

        new Concept(4, Set.of(), Set.of(), "longestSubstringNoDup", "Longest Substring Without Repeating Characters",
            "returns the length of the longest substring of s that contains no repeating characters.",
            "s", "public static int longestSubstringNoDup(String s)", "",
            List.of(new Example("s = \"abcabcbb\"", "3 (\"abc\")"), new Example("s = \"bbbbb\"", "1"), new Example("s = \"pwwkew\"", "3")),
            List.of("0 <= s.length <= 5*10^4"),
            List.of("for|while|Map|Set|window|left|right"),
            List.of("Uses a sliding-window with a HashSet/Map to track the current window's characters",
                    "Shrinks the window from the left when a duplicate is found")),

        // Algorithms (difficulty 4)
        new Concept(4, Set.of("algorithm", "algorithms", "data", "structures", "array", "arrays"), Set.of(), "findDuplicate", "Find the Duplicate Number",
            "finds and returns the one duplicate number in nums (n+1 integers each in [1,n]) without modifying the array.",
            "nums", "public static int findDuplicate(int[] nums)", "",
            List.of(new Example("nums = [1,3,4,2,2]", "2"), new Example("nums = [3,1,3,4,2]", "3")),
            List.of("2 <= nums.length <= 10^5", "Only one duplicate number exists"),
            List.of("for|while", "slow|fast|seen|set|sort|cycle"),
            List.of("Iterates through the array (pointer walk, sorting, or a set)",
                    "Detects the repeated element via cycle detection or a seen-set")),

        new Concept(4, Set.of("algorithm", "algorithms", "sorting", "data", "structures"), Set.of(), "mergeIntervals", "Merge Intervals",
            "merges all overlapping [start, end] intervals and returns the merged list.",
            "intervals", "public static int[][] mergeIntervals(int[][] intervals)", "",
            List.of(new Example("intervals = [[1,3],[2,6],[8,10]]", "[[1,6],[8,10]]"),
                    new Example("intervals = [[1,4],[4,5]]", "[[1,5]]")),
            List.of("1 <= intervals.length <= 10^4", "intervals[i].length == 2"),
            List.of("sort|Arrays.sort", "for|while"),
            List.of("Sorts the intervals by start value first", "Walks the sorted list, extending or closing intervals")),

        // Java/Spring (difficulty 4)
        new Concept(4, Set.of("java", "spring", "springboot", "jpa", "hibernate", "oop", "collections"), Set.of(), "reverseList", "Reverse Linked List",
            "reverses a singly linked list in-place and returns the new head. Each node has an `int val` and a `next` pointer.",
            "head", "public static ListNode reverseList(ListNode head)",
            "    static class ListNode {\n        int val;\n        ListNode next;\n        ListNode(int val) { this.val = val; }\n    }\n\n",
            List.of(new Example("head = [1,2,3,4,5]", "[5,4,3,2,1]"),
                    new Example("head = []", "[]")),
            List.of("0 <= number of nodes <= 5000"),
            List.of("while|for|reverseList|reverse", "next", "null"),
            List.of("Iterates or recurses through the list nodes",
                    "Rewires the next pointer of each node backwards",
                    "Returns null / handles empty list correctly")),

        new Concept(4, Set.of("java", "spring", "springboot", "algorithm", "algorithms", "sorting"), Set.of(), "topKFrequent", "Top K Frequent Elements",
            "returns the k most frequently occurring elements in nums (any order is fine).",
            "nums, k", "public static int[] topKFrequent(int[] nums, int k)", "",
            List.of(new Example("nums = [1,1,1,2,2,3], k = 2", "[1,2]"),
                    new Example("nums = [1], k = 1", "[1]")),
            List.of("1 <= nums.length <= 10^5", "k is always valid"),
            List.of("Map|HashMap", "sort|PriorityQueue|bucket"),
            List.of("Counts frequency with a HashMap",
                    "Selects the top k entries (heap, sort, or bucket sort)")),

        // Python/ML (difficulty 4)
        new Concept(4, Set.of("python", "data", "ml", "machine", "learning", "ai", "pandas", "numpy", "algorithm", "algorithms"), Set.of(), "matrixTranspose", "Transpose a Matrix",
            "returns the transpose of an m×n matrix (swap rows and columns).",
            "matrix", "public static int[][] matrixTranspose(int[][] matrix)", "",
            List.of(new Example("matrix = [[1,2,3],[4,5,6]]", "[[1,4],[2,5],[3,6]]"),
                    new Example("matrix = [[1]]", "[[1]]")),
            List.of("1 <= m, n <= 1000", "m * n <= 10^5"),
            List.of("for", "matrix[j][i]|[i][j]|zip"),
            List.of("Creates a new n×m array and assigns matrix[i][j] to result[j][i]")),

        // JS/Frontend (difficulty 4)
        new Concept(4, Set.of("javascript", "js", "typescript", "react", "reactjs", "frontend", "vue", "angular"), Set.of(), "memoize", "Memoize a Function",
            "wraps a single-argument function fn with memoization so that repeated calls with the same argument return the cached result.",
            "fn", "public static <T,R> java.util.function.Function<T,R> memoize(java.util.function.Function<T,R> fn)", "",
            List.of(new Example("memoize(expensiveCalc)(5) called twice", "Second call returns cached result instantly")),
            List.of("fn is a pure function", "Arguments are valid HashMap keys"),
            List.of("Map|HashMap|cache|containsKey|computeIfAbsent"),
            List.of("Uses a HashMap to store previously computed results",
                    "Returns the cached value on cache hits")),

        // C/Embedded (difficulty 4)
        new Concept(4, Set.of("c", "embedded", "firmware", "microcontroller", "arduino", "cpp", "c++"), Set.of(), "powerOfTwo", "Power of Two",
            "returns true if n is a power of two, using a bit-manipulation approach (no loops).",
            "n", "public static boolean powerOfTwo(int n)", "",
            List.of(new Example("n = 16", "true"), new Example("n = 6", "false"), new Example("n = 1", "true")),
            List.of("0 <= n <= 2^31 - 1"),
            List.of("&|n-1|>|=="),
            List.of("Checks n > 0 && (n & (n-1)) == 0 — exactly one bit is set")),

        // ══════════════════════════════════════════════════
        // DIFFICULTY 5 — Hard
        // ══════════════════════════════════════════════════

        // General fallbacks
        new Concept(5, Set.of(), Set.of(), "twoSum", "Two Sum",
            "returns the indices of the two numbers in nums that add up to target, in O(n) using a hash map.",
            "nums, target", "public static int[] twoSum(int[] nums, int target)", "",
            List.of(new Example("nums = [2,7,11,15], target = 9", "[0, 1]"), new Example("nums = [3,2,4], target = 6", "[1, 2]")),
            List.of("2 <= nums.length <= 10^4", "Exactly one valid answer exists"),
            List.of("for", "Map|dict|{}"),
            List.of("Iterates over the array once", "Uses a hash map to look up the complement in O(1)")),

        new Concept(5, Set.of(), Set.of(), "longestPalindromicSubstring", "Longest Palindromic Substring",
            "returns the longest palindromic substring in s.",
            "s", "public static String longestPalindromicSubstring(String s)", "",
            List.of(new Example("s = \"babad\"", "\"bab\" or \"aba\""), new Example("s = \"cbbd\"", "\"bb\"")),
            List.of("1 <= s.length <= 1000"),
            List.of("for|while|expand|dp|manacher", "max|longest"),
            List.of("Expands around each centre (or uses DP/Manacher's) to find palindromes",
                    "Tracks the start and length of the longest one found")),

        // DSA (difficulty 5)
        new Concept(5, Set.of("data", "structures", "algorithm", "algorithms", "hashmap", "hashing", "collections"), Set.of(), "twoSumMap", "Two Sum (HashMap)",
            "returns the indices of the two numbers in nums that add up to target, in O(n) using a hash map.",
            "nums, target", "public static int[] twoSumMap(int[] nums, int target)", "",
            List.of(new Example("nums = [2,7,11,15], target = 9", "[0, 1]"), new Example("nums = [3,2,4], target = 6", "[1, 2]")),
            List.of("2 <= nums.length <= 10^4", "Exactly one valid answer exists"),
            List.of("for", "Map|dict|{}"),
            List.of("Iterates over the array once", "Uses a hash map/dictionary to look up complements in O(1)")),

        new Concept(5, Set.of("tree", "trees", "graph", "graphs", "data", "structures"), Set.of(), "maxDepth", "Maximum Depth of Binary Tree",
            "returns the maximum depth of a binary tree, where each node has `value`, `left` and `right` fields.",
            "root", "public static int maxDepth(TreeNode root)", TREE_NODE,
            List.of(new Example("root = [3,9,20,null,null,15,7]", "3"), new Example("root = []", "0")),
            List.of("0 <= number of nodes <= 10^4"),
            List.of("left", "right", "max"),
            List.of("Recurses into the left subtree", "Recurses into the right subtree", "Combines depths with max")),

        // Java (difficulty 5)
        new Concept(5, Set.of("java", "spring", "springboot", "algorithm", "algorithms", "dynamic", "dp", "data", "structures", "jpa", "hibernate"), Set.of(),
            "maxSubArray", "Maximum Subarray (Kadane's Algorithm)",
            "finds the contiguous subarray with the largest sum and returns that sum. Aim for O(n) using Kadane's algorithm.",
            "nums", "public static int maxSubArray(int[] nums)", "",
            List.of(new Example("nums = [-2,1,-3,4,-1,2,1,-5,4]", "6 (subarray [4,-1,2,1])"),
                    new Example("nums = [1]", "1"), new Example("nums = [5,4,-1,7,8]", "23")),
            List.of("1 <= nums.length <= 10^5", "-10^4 <= nums[i] <= 10^4"),
            List.of("for|while", "max|Math.max|maxSum|currentSum|curr"),
            List.of("Iterates over the array once",
                    "Tracks both the current running sum and the global maximum")),

        new Concept(5, Set.of("java", "spring", "springboot", "oop", "design", "collections"), Set.of(), "lruCache", "LRU Cache",
            "implements an LRU cache with get(key) and put(key, value) both in O(1). " +
            "Evict the least recently used entry when capacity is exceeded. Use a class with both methods.",
            "capacity", "// Design: class LRUCache { int get(int key); void put(int key, int value); }",
            "    static class LRUCache {\n        // implement here\n    }\n\n",
            List.of(new Example("put(1,1), put(2,2), get(1), put(3,3) (capacity=2)", "get(2) returns -1 (evicted)")),
            List.of("1 <= capacity <= 3000", "0 <= key, value <= 10^4"),
            List.of("LinkedHashMap|HashMap|Map|removeEldest|put|get", "doubly|linked|list|node"),
            List.of("Maintains O(1) access using a HashMap",
                    "Maintains insertion/access order using a doubly-linked list or LinkedHashMap")),

        // Python/ML (difficulty 5)
        new Concept(5, Set.of("python", "data", "ml", "machine", "learning", "ai", "pandas", "numpy", "algorithm", "algorithms", "nlp"), Set.of(), "coinChange", "Coin Change (DP)",
            "given a list of coin denominations and an amount, returns the fewest coins needed to make up that amount, or -1 if impossible.",
            "coins, amount", "public static int coinChange(int[] coins, int amount)", "",
            List.of(new Example("coins = [1,5,11], amount = 15", "3 (5+5+5)"),
                    new Example("coins = [2], amount = 3", "-1")),
            List.of("1 <= coins.length <= 12", "0 <= amount <= 10^4"),
            List.of("for|while|dp|int[]|memo|min|Math.min"),
            List.of("Builds a DP table dp[i] = min coins to make amount i",
                    "For each amount, tries every coin and takes the minimum")),

        // JS/Frontend (difficulty 5)
        new Concept(5, Set.of("javascript", "js", "typescript", "react", "reactjs", "frontend", "vue", "angular", "node", "nodejs"), Set.of(), "deepEqual", "Deep Equality Check",
            "returns true if two values a and b are deeply equal (handles nested objects and arrays).",
            "a, b", "public static boolean deepEqual(Object a, Object b)", "",
            List.of(new Example("a = {x:1,y:{z:2}}, b = {x:1,y:{z:2}}", "true"),
                    new Example("a = [1,[2,3]], b = [1,[2,4]]", "false")),
            List.of("Values may be primitives, arrays, or plain objects"),
            List.of("instanceof|typeof|getClass|equals|recursion|recursive", "for|while|keys|entrySet"),
            List.of("Handles base cases (null, primitive types) first",
                    "Recursively compares nested properties/elements")),

        // C/Embedded (difficulty 5)
        new Concept(5, Set.of("c", "embedded", "firmware", "microcontroller", "arduino", "cpp", "c++"), Set.of(), "atoi", "Implement atoi (String to Integer)",
            "converts a string to a 32-bit signed integer, handling leading whitespace, optional sign, and clamping to [-2^31, 2^31-1].",
            "s", "public static int myAtoi(String s)", "",
            List.of(new Example("s = \"42\"", "42"), new Example("s = \"  -42\"", "-42"),
                    new Example("s = \"4193 with words\"", "4193"), new Example("s = \"99999999999\"", "2147483647")),
            List.of("0 <= s.length <= 200"),
            List.of("for|while|trim|Character.isDigit|parseInt|sign|overflow|MAX_VALUE|MIN_VALUE"),
            List.of("Strips leading whitespace and reads an optional sign",
                    "Parses digit characters and detects integer overflow"))
    );



    public Problem generateProblem(int difficulty, String jobRole, List<String> rankedResumeSkills) {
        return generateProblem(difficulty, jobRole, rankedResumeSkills, Set.of());
    }

    /**
     * Same as above but skips any concept whose title appears in excludedTitles.
     * If all concepts at this difficulty are excluded, relaxes to adjacent difficulty
     * levels so the candidate always gets a fresh problem.
     */
    public Problem generateProblem(int difficulty, String jobRole,
                                   List<String> rankedResumeSkills, Set<String> excludedTitles) {
        String language = detectLanguage(jobRole, rankedResumeSkills);
        int level = clampDifficulty(difficulty);

        // Try current difficulty first, then widen range if all exhausted
        for (int radius = 0; radius <= 4; radius++) {
            List<Concept> options = new ArrayList<>();
            for (Concept c : CATALOG) {
                int d = c.difficulty();
                boolean inRange = (radius == 0) ? d == level
                        : d == level - radius || d == level + radius;
                if (inRange && c.supports(language)
                        && !excludedTitles.contains(c.title())) {
                    options.add(c);
                }
            }
            if (!options.isEmpty()) {
                return render(pickBestMatch(options, rankedResumeSkills, jobRole), language, jobRole);
            }
        }
        // Absolute fallback: pick any random question from the catalog
        Concept fallback = CATALOG.get(random.nextInt(CATALOG.size()));
        return render(fallback, language, jobRole);
    }

    /** Kept for backward compatibility with any caller not passing resume skills. */
    public Problem generateProblem(int difficulty, String jobRole) {
        return generateProblem(difficulty, jobRole, List.of(), Set.of());
    }

    /**
     * Language the candidate should be coding in. The job role wins ("java
     * developer" always gets Java); otherwise the first language found in
     * the resume's ranked skills; otherwise JavaScript.
     */
    String detectLanguage(String jobRole, List<String> rankedResumeSkills) {
        String fromRole = languageOf(tokens(jobRole));
        if (fromRole != null) return fromRole;
        for (String skill : rankedResumeSkills) {
            String lang = languageOf(List.of(skill.toLowerCase(Locale.ROOT)));
            if (lang != null) return lang;
        }
        return JAVASCRIPT;
    }

    private String languageOf(List<String> words) {
        // Check exact words so "java" is never confused with "javascript".
        for (String w : words) {
            switch (w) {
                case "java", "spring", "springboot", "hibernate", "jpa" -> { return JAVA; }
                case "python", "django", "flask", "pandas", "numpy" -> { return PYTHON; }
                case "javascript", "js", "typescript", "react", "reactjs", "node", "nodejs", "angular", "vue", "frontend" -> { return JAVASCRIPT; }
                case "c", "embedded", "firmware", "microcontroller", "arduino" -> { return C; }
                default -> { }
            }
        }
        return null;
    }

    private List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String t : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!t.isBlank()) out.add(t);
        }
        return out;
    }

    private Concept pickBestMatch(List<Concept> options, List<String> rankedResumeSkills, String jobRole) {
        // rankedResumeSkills is already ordered by relevance to the role (Eq. 1);
        // role words come last so they only break ties the resume can't.
        List<String> walk = new ArrayList<>();
        for (String s : rankedResumeSkills) walk.add(s.toLowerCase(Locale.ROOT));
        walk.addAll(tokens(jobRole));

        // Shuffle first so equal-priority concepts don't always resolve to the
        // same one — prevents the same question from appearing every session.
        List<Concept> shuffled = new ArrayList<>(options);
        Collections.shuffle(shuffled, random);

        for (String skill : walk) {
            for (Concept c : shuffled) {
                if (c.keywords().contains(skill)) return c;
            }
        }
        // Among fallback (keyword-free) concepts, pick one at random
        List<Concept> fallbacks = shuffled.stream().filter(Concept::isFallback).toList();
        if (!fallbacks.isEmpty()) return fallbacks.get(random.nextInt(fallbacks.size()));
        return shuffled.get(0);
    }

    private Problem render(Concept c, String language, String jobRole) {
        String snake = c.name().replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        String statement;
        String starter;

        switch (language) {
            case JAVA -> {
                statement = "Implement the method `" + c.javaSig() + "` in a `Solution` class. It " + c.description();
                starter = "import java.util.*;\n\n"
                        + "public class Solution {\n"
                        + c.javaExtras()
                        + "    " + c.javaSig() + " {\n"
                        + "        // your code here\n"
                        + "    }\n"
                        + "}\n";
            }
            case PYTHON -> {
                String sig = "def " + snake + "(" + c.params() + "):";
                statement = "Write a Python function `" + sig + "` that " + c.description();
                starter = sig + "\n    # your code here\n    pass\n";
            }
            case C -> {
                // Build a simple C function signature from the Java signature
                String cReturnType = c.javaSig().startsWith("public static int[]") ? "void"
                        : c.javaSig().startsWith("public static int")    ? "int"
                        : c.javaSig().startsWith("public static boolean") ? "int"   // C uses int for bool
                        : c.javaSig().startsWith("public static String")  ? "char*"
                        : "int";
                String cSig = cReturnType + " solution(" + cParamList(c.params()) + ")";
                statement = "Write a C function `" + cSig + "` that " + c.description() +
                        " (In C, use int instead of boolean; return 1 for true, 0 for false.)";
                starter = "#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n\n"
                        + cSig + " {\n"
                        + "    /* your code here */\n"
                        + "    return 0;\n"
                        + "}\n\n"
                        + "int main() {\n"
                        + "    /* test your solution here */\n"
                        + "    return 0;\n"
                        + "}\n";
            }
            default -> {
                String sig = "function " + c.name() + "(" + c.params() + ")";
                statement = "Write a JavaScript function `" + sig + "` that " + c.description();
                starter = sig + " {\n  // your code here\n}\n";
            }
        }

        // Scored only against what the candidate wrote (starter lines are
        // stripped first, see CodeEvaluationService.stripStarter): "return"
        // must be written by the candidate (the starter deliberately has
        // none), and the concept's evidence tokens check the body does the work.
        List<String> tokens = new ArrayList<>();
        tokens.add("return");
        tokens.addAll(c.evidence());

        List<String> labels = new ArrayList<>();
        labels.add("Includes a return statement");
        labels.addAll(c.evidenceLabels());

        if (jobRole != null && !jobRole.isBlank()) {
            statement += " (Role context: " + jobRole + ")";
        }
        return new Problem(c.title(), statement, c.examples(), c.constraints(), tokens, labels, language, starter);
    }

    /** Converts generic param names (arr, s, n, nums, target) to C-typed declarations. */
    private String cParamList(String params) {
        if (params == null || params.isBlank()) return "void";
        String[] parts = params.split(",");
        List<String> typed = new ArrayList<>();
        for (String p : parts) {
            String name = p.trim();
            String decl = switch (name) {
                case "arr", "nums", "prices", "intervals" -> "int* " + name + ", int size";
                case "s", "t" -> "char* " + name;
                case "n", "target" -> "int " + name;
                case "root" -> "struct TreeNode* root";
                default -> "int " + name;
            };
            typed.add(decl);
        }
        return String.join(", ", typed);
    }

    private int clampDifficulty(int d) {
        return Math.max(1, Math.min(5, d));
    }
}
