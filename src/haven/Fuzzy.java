package haven;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import haven.MenuSearch.Result;


public class Fuzzy {

    /**
     * Calculates the Jaccard similarity between two strings based on character sets.
     * 
     * @param str1 The first string.
     * @param str2 The second string.
     * @return A double value representing the Jaccard similarity.
     */
    public static double jaccardSimilarity(String str1, String str2) {
        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();

        for (char ch : str1.toCharArray()) {
            set1.add(ch);
        }
        for (char ch : str2.toCharArray()) {
            set2.add(ch);
        }

        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /**
     * Performs sequential fuzzy substring matching.
     * Checks if all characters of the needle appear in the haystack in the same order.
     * 
     * @param haystack The string to search within.
     * @param needle The substring to search for.
     * @return True if the haystack contains the needle in a fuzzy sequential manner, false otherwise.
     */
    public static boolean fuzzyContains(String haystack, String needle) {
        int hayIndex = 0;
        for (char ch : needle.toCharArray()) {
            hayIndex = haystack.indexOf(ch, hayIndex);
            if (hayIndex == -1) {
                return false;
            }
            hayIndex++; // Move past the found character for the next search
        }
        return true;
    }

    /**
     * Filters a list of results based on fuzzy matching with a given needle string.
     * Sorts the results by Jaccard similarity.
     * 
     * @param needle The string to search for in each result.
     * @param results The list of results to filter and sort.
     * @return A list of results that match the needle, sorted by Jaccard similarity.
     */
    public static List<Result> fuzzyFilterAndSort(String needle, List<Result> results) {
        List<Result> found = new ArrayList<>();
        String lowerNeedle = needle.toLowerCase();

        for (Result res : results) {
            String haystack = res.btn.name().toLowerCase();
            if (fuzzyContains(haystack, lowerNeedle)) {
                found.add(res);
            }
        }

        // Sort using Jaccard similarity (common letters regardless of relative positions)
        found.sort(Comparator.comparingDouble(res -> -jaccardSimilarity(lowerNeedle, res.btn.name().toLowerCase())));
        return found;
    }
}

