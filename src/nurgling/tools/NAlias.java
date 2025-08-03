package nurgling.tools;

import java.util.*;

public class NAlias {
    /**
     * Keys for alias matching.
     */
    public ArrayList<String> keys;
    
    /**
     * Exceptions to exclude from matching.
     */
    public ArrayList<String> exceptions;
    
    /**
     * Lowercase version of keys for performance.
     */
    private Set<String> lowercaseKeys;
    
    /**
     * Lowercase version of exceptions for performance.
     */
    private Set<String> lowercaseExceptions;
    
    /**
     * Cache for string matching results.
     */
    private static final Map<String, Map<NAlias, Boolean>> MATCH_CACHE = new HashMap<>();
    
    /**
     * Maximum size of match cache.
     */
    private static final int MAX_CACHE_SIZE = 1000;

    public NAlias() {
        keys = new ArrayList<String> ();
        exceptions = new ArrayList<String> ();
        buildCaches();
    }

    public NAlias(String... args) {
        keys = new ArrayList<String> (Arrays.asList(args));
        exceptions = new ArrayList<String> ();
        buildCaches();
    }

    public NAlias(String name ) {
        keys = new ArrayList<String> ( Collections.singletonList ( name ) );
        exceptions = new ArrayList<String> ();
        buildCaches();
    }

    public NAlias(ArrayList<String> keys ) {
        this.keys = new ArrayList<String> ();
        exceptions = new ArrayList<String> ();
        this.keys.addAll ( keys );
        buildCaches();
    }

    public NAlias(
            ArrayList<String> keys,
            ArrayList<String> exceptions
    ) {
        this.keys = keys;
        this.exceptions = exceptions;
        buildCaches();
    }

    public NAlias(
            List<String> keys,
            List<String> exceptions
    ) {
        this.keys = new ArrayList<> ();
        this.keys.addAll ( keys );
        this.exceptions = new ArrayList<> ();
        this.exceptions.addAll ( exceptions );
        buildCaches();
    }

    /**
     * Retrieves the first key as default.
     *
     * @return the first key in the list.
     */
    public String getDefault () {
        return keys.get ( 0 );
    }
    
    /**
     * Builds cached lowercase versions of keys and exceptions for fast lookups
     */
    public void buildCaches() {
        lowercaseKeys = new HashSet<>();
        lowercaseExceptions = new HashSet<>();
        
        if (keys != null) {
            for (String key : keys) {
                if (key != null) {
                    lowercaseKeys.add(key.toLowerCase());
                }
            }
        }
        
        if (exceptions != null) {
            for (String exception : exceptions) {
                if (exception != null) {
                    lowercaseExceptions.add(exception.toLowerCase());
                }
            }
        }
    }
    
    /**
     * Fast matching using cached lowercase strings for better performance.
     *
     * @param name the string to match against.
     * @return true if name matches this alias, false otherwise.
     */
    public boolean matches(String name) {
        if (name == null) return false;
        
        String lowerName = name.toLowerCase();
        
        // Check cache first
        Map<NAlias, Boolean> nameCache = MATCH_CACHE.get(lowerName);
        if (nameCache != null) {
            Boolean cachedResult = nameCache.get(this);
            if (cachedResult != null) {
                return cachedResult;
            }
        }
        
        boolean result = matchesInternal(lowerName);
        
        // Cache the result
        if (MATCH_CACHE.size() < MAX_CACHE_SIZE) {
            MATCH_CACHE.computeIfAbsent(lowerName, k -> new HashMap<>()).put(this, result);
        }
        
        return result;
    }
    
    private boolean matchesInternal(String lowerName) {
        // Check if any key matches
        if (!lowercaseKeys.isEmpty()) {
            boolean keyMatched = false;
            for (String key : lowercaseKeys) {
                if (lowerName.contains(key)) {
                    keyMatched = true;
                    break;
                }
            }
            if (!keyMatched) {
                return false;
            }
        }
        
        // Check exceptions
        for (String exception : lowercaseExceptions) {
            if (lowerName.contains(exception)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get cached lowercase keys for external use.
     *
     * @return unmodifiable set of lowercase keys.
     */
    public Set<String> getLowercaseKeys() {
        return Collections.unmodifiableSet(lowercaseKeys);
    }
    
    /**
     * Get cached lowercase exceptions for external use.
     *
     * @return unmodifiable set of lowercase exceptions.
     */
    public Set<String> getLowercaseExceptions() {
        return Collections.unmodifiableSet(lowercaseExceptions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NAlias nAlias = (NAlias) o;
        return keys.equals(nAlias.keys) && exceptions.equals(nAlias.exceptions);
    }

    @Override
    public int hashCode() {
        int result = keys.hashCode();
        result = 31 * result + exceptions.hashCode();
        return result;
    }
}