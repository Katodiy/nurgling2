package nurgling.translation;

import nurgling.NConfig;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Simple localization utility for Nurgling2
 * Clean static API for translating UI strings
 */
public class L10n {
    private static final Logger logger = Logger.getLogger(L10n.class.getName());

    private static String currentLanguage = "en";
    private static String fallbackLanguage = "en";
    private static Map<String, String> translations = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Get translated text for a key
     */
    public static String get(String key) {
        if (!initialized) initialize();
        return translations.getOrDefault(key, key);
    }

    /**
     * Set the current language and reload translations
     */
    public static void setLanguage(String language) {
        if (!currentLanguage.equals(language)) {
            currentLanguage = language;
            initialized = false;
            translations.clear();
            initialize();

            // Save to config
            NConfig.set(NConfig.Key.language, language);
            logger.info("Language changed to: " + language);
        }
    }

    /**
     * Get current language
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Clear translation cache
     */
    public static void clearCache() {
        translations.clear();
        initialized = false;
    }

    /**
     * Initialize translation system
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Load language from config
            Object langObj = NConfig.get(NConfig.Key.language);
            if (langObj instanceof String) {
                String configLang = (String) langObj;
                if (!configLang.equals(currentLanguage)) {
                    currentLanguage = configLang;
                }
            }

            // Load all translation files into single map
            loadTranslations("static_ui");
            loadTranslations("items");
            loadTranslations("skills");

            initialized = true;
            logger.info("L10n initialized successfully for language: " + currentLanguage +
                       " (" + translations.size() + " translations loaded)");

        } catch (Exception e) {
            logger.severe("Failed to initialize L10n: " + e.getMessage());
            e.printStackTrace();
            initialized = true; // Prevent retry loops
        }
    }

    /**
     * Load translations from a JSON file
     */
    private static void loadTranslations(String category) {
        String jsonPath = "/resources/translations/dynamic/" + category + "_" + currentLanguage + ".json";
        InputStream is = L10n.class.getResourceAsStream(jsonPath);

        if (is == null && !currentLanguage.equals(fallbackLanguage)) {
            // Try fallback language
            jsonPath = "/resources/translations/dynamic/" + category + "_" + fallbackLanguage + ".json";
            is = L10n.class.getResourceAsStream(jsonPath);
        }

        if (is != null) {
            try (InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
                Map<String, String> categoryTranslations = parseJson(reader);
                translations.putAll(categoryTranslations);
                logger.info("Loaded " + category + " translations: " + categoryTranslations.size() + " entries");

                return;
            } catch (IOException e) {
                logger.warning("Failed to load translations from: " + jsonPath + " - " + e.getMessage());
            }
        }
    }

    /**
     * Simple JSON parser for translation files
     */
    private static Map<String, String> parseJson(InputStreamReader reader) throws IOException {
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

            // Find the separator colon - look for ": pattern (end of quoted key + separator colon)
            int quoteColonIndex = pair.indexOf("\":");
            if (quoteColonIndex > 0) {
                // Include the closing quote in the key, skip the separator colon for value
                String key = pair.substring(0, quoteColonIndex + 1).trim(); // Include the closing quote
                String value = pair.substring(quoteColonIndex + 2).trim();  // Skip ": separator

                // Remove quotes
                key = removeQuotes(key);
                value = removeQuotes(value);

                if (!key.isEmpty() && !value.isEmpty()) {
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Remove surrounding quotes from a string
     */
    private static String removeQuotes(String str) {
        if (str.length() >= 2 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
}