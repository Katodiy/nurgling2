package nurgling.tools;

/**
 * Positional fuzzy scoring for short UI strings.
 *
 * <p>{@link haven.Fuzzy} already answers "does this match", but it ranks by Jaccard similarity over
 * character <em>sets</em>, which is blind to both order and position: against the query "vision" it
 * scores "Vision noise" and "Envision" identically. Picking a setting out of a list of three hundred
 * needs the opposite bias, so this scorer rewards, in order, an exact hit, a substring hit, a hit
 * that starts on a word boundary, and a hit that starts early.
 *
 * <p>All inputs are expected to be lower-case already. Callers cache the folded haystacks and fold
 * the query once per keystroke rather than per comparison.
 */
public class NFuzzy {
    /** Returned when the needle is not present in the haystack at all. */
    public static final double NO_MATCH = Double.NEGATIVE_INFINITY;

    private static final double EQUAL = 1000;
    private static final double SUBSTRING = 400;
    private static final double SUBSEQUENCE = 100;
    private static final double WORD_START = 150;

    /** Lower-case a string for indexing or querying, tolerating null. */
    public static String fold(String s) {
        return((s == null) ? "" : s.toLowerCase());
    }

    /**
     * Score one already-folded needle against one already-folded haystack.
     *
     * @return A score where higher is better, or {@link #NO_MATCH}
     */
    public static double score(String hay, String needle) {
        if(needle.isEmpty())
            return(0);
        if(hay.isEmpty())
            return(NO_MATCH);
        if(hay.equals(needle))
            return(EQUAL);

        int at = hay.indexOf(needle);
        if(at >= 0) {
            double s = SUBSTRING;
            if((at == 0) || boundary(hay.charAt(at - 1)))
                s += WORD_START;
            /* Earlier is more likely to be what was meant, and a haystack that is mostly
             * the needle is more specific than one where it is a passing mention. */
            s -= at * 2.0;
            s -= (hay.length() - needle.length()) * 0.5;
            return(s);
        }
        return(subsequence(hay, needle));
    }

    /** True if the needle is present at all, fuzzily. */
    public static boolean matches(String hay, String needle) {
        return(score(hay, needle) != NO_MATCH);
    }

    /* Greedy left-to-right subsequence match: every needle character must appear in order.
     * Runs that stay contiguous, or that land on word starts, are treated as far better
     * matches than characters scattered across the string. */
    private static double subsequence(String hay, String needle) {
        double s = SUBSEQUENCE;
        int from = 0, prev = -2, first = -1;
        for(int i = 0; i < needle.length(); i++) {
            int at = hay.indexOf(needle.charAt(i), from);
            if(at < 0)
                return(NO_MATCH);
            if(first < 0)
                first = at;
            if(at == prev + 1)
                s += 8;
            else if((at == 0) || boundary(hay.charAt(at - 1)))
                s += 5;
            prev = at;
            from = at + 1;
        }
        s -= first * 1.5;
        s -= (hay.length() - needle.length()) * 0.3;
        return(s);
    }

    /* Spaces, dots and underscores all separate words here, so "night_vision" and
     * "Night vision" break the same way. */
    private static boolean boundary(char c) {
        return(!Character.isLetterOrDigit(c));
    }
}
