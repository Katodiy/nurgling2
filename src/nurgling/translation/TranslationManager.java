package nurgling.translation;

import nurgling.NConfig;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
// Using simple JSON parsing instead of GSON to avoid dependencies

/**
 * Hybrid Translation Manager for Nurgling2
 * Handles both static UI translations and dynamic server content translations
 */
public class TranslationManager {
    private static final Logger logger = Logger.getLogger(TranslationManager.class.getName());
    private static TranslationManager instance;

    // Current language settings
    private String currentLanguage = "en";
    private String fallbackLanguage = "en";

    // Layer 1: Static UI translations (JSON style)
    private Map<String, String> staticTranslations;

    // Layer 2: Dynamic content translations (JSON dictionaries)
    private Map<String, String> itemTranslations;
    private Map<String, String> skillTranslations;

    // Performance optimization
    private final Map<String, String> translationCache = new ConcurrentHashMap<>();
    private boolean initialized = false;

    private TranslationManager() {}

    public static TranslationManager getInstance() {
        if (instance == null) {
            synchronized (TranslationManager.class) {
                if (instance == null) {
                    instance = new TranslationManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize translation system with user's language preference
     */
    public void initialize() {
        if (initialized) return;

        try {
            // Get language preference from NConfig
            Object langPref = NConfig.get(NConfig.Key.language);
            if (langPref instanceof String) {
                currentLanguage = (String) langPref;
            }

            logger.info("Initializing TranslationManager with language: " + currentLanguage);

            // Load translation files
            loadStaticTranslations();
            loadDynamicTranslations();

            initialized = true;
            logger.info("TranslationManager initialized successfully");

        } catch (Exception e) {
            logger.severe("Failed to initialize TranslationManager: " + e.getMessage());
            // Fall back to English
            currentLanguage = "en";
            loadFallbackTranslations();
            initialized = true;
        }
    }

    /**
     * Load static UI translations from JSON files
     */
    private void loadStaticTranslations() {
        // Load static UI translations using the same JSON method as dynamic content
        staticTranslations = loadJsonTranslations("static_ui");

        logger.info("Loaded static UI translations: " + staticTranslations.size() + " entries");
    }

    /**
     * Load dynamic content translations from JSON files
     */
    private void loadDynamicTranslations() {
        // Load item translations
        itemTranslations = loadJsonTranslations("items");

        // Load skill translations
        skillTranslations = loadJsonTranslations("skills");

        logger.info("Loaded dynamic translations - Items: " + itemTranslations.size() +
                   ", Skills: " + skillTranslations.size());
    }

    private Map<String, String> loadJsonTranslations(String category) {
        String jsonPath = "/resources/translations/dynamic/" + category + "_" + currentLanguage + ".json";
        InputStream is = getClass().getResourceAsStream(jsonPath);

        if (is == null && !currentLanguage.equals(fallbackLanguage)) {
            // Try fallback language
            jsonPath = "/resources/translations/dynamic/" + category + "_" + fallbackLanguage + ".json";
            is = getClass().getResourceAsStream(jsonPath);
        }

        if (is != null) {
            try (InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
                boolean preserveCase = category.startsWith("static_ui");
                Map<String, String> translations = parseSimpleJson(reader, preserveCase);
                logger.info("Loaded " + category + " translations from: " + jsonPath + " (" + translations.size() + " entries)");
                return translations != null ? translations : new HashMap<>();
            } catch (Exception e) {
                logger.warning("Failed to load " + category + " translations: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.warning("Could not find translation file: " + jsonPath);
        }

        return new HashMap<>();
    }

    /**
     * Simple JSON parser for basic string-to-string mappings
     * Avoids external dependencies
     */
    private Map<String, String> parseSimpleJson(InputStreamReader reader, boolean preserveCase) throws IOException {
        Map<String, String> result = new HashMap<>();
        StringBuilder content = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            content.append((char) ch);
        }

        String json = content.toString().trim();

        // Remove outer braces
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
        }

        // Split by commas, but be careful about commas inside strings
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            // Skip comments
            if (pair.startsWith("\"_")) continue;

            int colonIndex = pair.indexOf(':');
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();

                // Remove quotes
                key = removeQuotes(key);
                value = removeQuotes(value);

                if (!key.isEmpty() && !value.isEmpty()) {
                    String finalKey = preserveCase ? key : key.toLowerCase();
                    result.put(finalKey, value);
                }
            }
        }

        return result;
    }

    // Compatibility method for existing calls
    private Map<String, String> parseSimpleJson(InputStreamReader reader) throws IOException {
        return parseSimpleJson(reader, false);
    }

    private String removeQuotes(String str) {
        if (str.length() >= 2 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private void loadFallbackTranslations() {
        staticTranslations = new HashMap<>();
        itemTranslations = new HashMap<>();
        skillTranslations = new HashMap<>();
    }

    /**
     * Translate static UI text
     */
    public String translateStatic(String key) {
        if (!initialized) initialize();

        if (staticTranslations != null && staticTranslations.containsKey(key)) {
            return staticTranslations.get(key);
        }

        return key; // Return original if no translation found
    }

    /**
     * Translate static UI text with parameters
     */
    public String translateStatic(String key, Object... params) {
        String translated = translateStatic(key);
        if (params.length > 0) {
            try {
                return String.format(translated, params);
            } catch (Exception e) {
                logger.warning("Failed to format translated string: " + translated);
                return translated;
            }
        }
        return translated;
    }

    /**
     * Translate item name (dynamic content)
     */
    public String translateItem(String itemName) {
        if (!initialized) initialize();

        // Check cache first for performance
        String cacheKey = "item:" + itemName;
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Look up translation
        String translated = itemTranslations.getOrDefault(itemName.toLowerCase(), itemName);

        // Cache result
        translationCache.put(cacheKey, translated);

        return translated;
    }

    /**
     * Translate skill name (dynamic content)
     */
    public String translateSkill(String skillName) {
        if (!initialized) initialize();

        String cacheKey = "skill:" + skillName;
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String translated = skillTranslations.getOrDefault(skillName.toLowerCase(), skillName);
        translationCache.put(cacheKey, translated);

        return translated;
    }

    /**
     * Generic translation method with type detection
     */
    public String translate(String text, TranslationType type) {
        if (!initialized) initialize();

        switch(type) {
            case UI_STATIC:
                return translateStatic(text);
            case ITEM_DYNAMIC:
                return translateItem(text);
            case SKILL_DYNAMIC:
                return translateSkill(text);
            case AUTO_DETECT:
                return autoDetectAndTranslate(text);
            default:
                return text;
        }
    }

    private String autoDetectAndTranslate(String text) {
        // Simple heuristic: try item first, then static UI
        String itemResult = translateItem(text);
        if (!itemResult.equals(text)) {
            return itemResult;
        }

        return translateStatic(text);
    }

    /**
     * Change language at runtime
     */
    public void setLanguage(String languageCode) {
        if (!languageCode.equals(currentLanguage)) {
            currentLanguage = languageCode;

            // Save to config
            NConfig.set(NConfig.Key.language, languageCode);

            // Clear cache and reload
            translationCache.clear();
            initialized = false;
            initialize();

            logger.info("Language changed to: " + languageCode);
        }
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public Set<String> getAvailableLanguages() {
        // For now, return supported languages
        return Set.of("en", "ru");
    }

    /**
     * Clear translation cache (for memory management)
     */
    public void clearCache() {
        translationCache.clear();
    }

    /**
     * Get translation statistics for debugging
     */
    public TranslationStats getStats() {
        return new TranslationStats(
            staticTranslations != null ? staticTranslations.size() : 0,
            itemTranslations.size(),
            skillTranslations.size(),
            translationCache.size()
        );
    }

    public static class TranslationStats {
        public final int staticCount;
        public final int itemCount;
        public final int skillCount;
        public final int cacheSize;

        public TranslationStats(int staticCount, int itemCount, int skillCount, int cacheSize) {
            this.staticCount = staticCount;
            this.itemCount = itemCount;
            this.skillCount = skillCount;
            this.cacheSize = cacheSize;
        }

        @Override
        public String toString() {
            return String.format("Static: %d, Items: %d, Skills: %d, Cache: %d",
                               staticCount, itemCount, skillCount, cacheSize);
        }
    }
}