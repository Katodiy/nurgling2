package nurgling.i18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import java.util.*;

import nurgling.NConfig;

/**
 * Localization system for Nurgling client.
 * 
 * Usage:
 *   - In tooltip resources: store key with @ prefix: "@bot.chopper.title"
 *   - In code: L10n.get("bot.chopper.title") or L10n.tr("@bot.chopper.title")
 *   - With parameters: L10n.get("craft.progress", current, total)
 * 
 * Users can create their own translations by placing messages_XX.properties
 * in the lang/ folder next to the client jar.
 */
public final class L10n {
    
    private static final String KEY_PREFIX = "@";
    private static final String BUNDLE_NAME = "messages";
    
    private static Properties messages = new Properties();
    private static Properties fallback = new Properties();
    private static Locale currentLocale = Locale.ENGLISH;
    private static Path langDir;
    private static boolean initialized = false;
    
    // Supported languages for UI selection (only languages with translation files)
    public static final String[][] SUPPORTED_LANGUAGES = {
        {"en", "English"},
        {"ru", "Русский"}
    };
    
    static {
        init();
    }
    
    private static void init() {
        if (initialized) return;
        
        try {
            // Try to find lang/ directory relative to jar location
            langDir = findLangDirectory();
            
            // Load fallback (English) from jar resources
            loadFromResources(fallback, "");
            
            // Copy messages to fallback as base
            messages.putAll(fallback);
            
            // Try to load user's locale
            setLocale(Locale.getDefault());
            
            initialized = true;
            System.out.println("[L10n] Initialized with locale: " + currentLocale);
        } catch (Exception e) {
            System.err.println("[L10n] Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Path findLangDirectory() {
        try {
            // Try relative to jar
            Path jarPath = Paths.get(L10n.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent();
            Path langPath = jarPath.resolve("lang");
            if (Files.exists(langPath)) {
                return langPath;
            }
            
            // Try current working directory
            langPath = Paths.get("lang");
            if (Files.exists(langPath)) {
                return langPath;
            }
            
            // Try to create it
            Files.createDirectories(jarPath.resolve("lang"));
            return jarPath.resolve("lang");
        } catch (Exception e) {
            // Fallback to current directory
            return Paths.get("lang");
        }
    }
    
    /**
     * Set the current locale and reload translations.
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        messages = new Properties();
        messages.putAll(fallback); // Start with fallback
        
        // Try to load from external file first (user translations)
        boolean loaded = loadFromFile(messages, locale);
        
        // If not found externally, try from jar resources
        if (!loaded) {
            loadFromResources(messages, "_" + locale.getLanguage());
        }
    }
    
    /**
     * Set locale by language code (e.g., "ru", "en", "de").
     */
    public static void setLanguage(String langCode) {
        setLocale(Locale.forLanguageTag(langCode));
    }
    
    private static boolean loadFromFile(Properties props, Locale locale) {
        if (langDir == null) return false;
        
        // Try: messages_ru_RU.properties в†’ messages_ru.properties
        String[] suffixes = {
            "_" + locale.getLanguage() + "_" + locale.getCountry(),
            "_" + locale.getLanguage()
        };
        
        for (String suffix : suffixes) {
            Path file = langDir.resolve(BUNDLE_NAME + suffix + ".properties");
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    props.load(reader);
                    System.out.println("[L10n] Loaded translations from: " + file);
                    return true;
                } catch (IOException e) {
                    System.err.println("[L10n] Failed to load " + file + ": " + e.getMessage());
                }
            }
        }
        return false;
    }
    
    private static void loadFromResources(Properties props, String suffix) {
        String resourcePath = "/lang/" + BUNDLE_NAME + suffix + ".properties";
        try (InputStream is = L10n.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                System.out.println("[L10n] Loaded translations from resources: " + resourcePath);
            }
        } catch (IOException e) {
            // Silently ignore - fallback will be used
        }
    }
    
    /**
     * Translate a key or pass-through text.
     * If text starts with "@", it's treated as a localization key.
     * Otherwise, the text is returned as-is (for server strings).
     * 
     * @param text The text or key to translate
     * @return Translated text or original if no translation found
     */
    public static String tr(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Check if this is a localization key
        if (text.startsWith(KEY_PREFIX)) {
            String key = text.substring(KEY_PREFIX.length());
            return getMessage(key);
        }
        
        // Not a key - return as-is (server tooltip, etc.)
        return text;
    }
    
    /**
     * Get translation by key directly (without @ prefix).
     * 
     * @param key The translation key
     * @return Translated text or key in brackets if not found
     */
    public static String get(String key) {
        return getMessage(key);
    }
    
    /**
     * Get translation with parameter substitution.
     * Uses MessageFormat patterns: {0}, {1}, etc.
     * 
     * Example: get("craft.progress", 5, 10) with pattern "Crafting {0} of {1}"
     *          returns "Crafting 5 of 10"
     * 
     * @param key The translation key
     * @param args Values to substitute
     * @return Formatted translated text
     */
    public static String get(String key, Object... args) {
        String pattern = getMessage(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern; // Return unformatted if formatting fails
        }
    }
    
    private static String getMessage(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        
        String value = messages.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Not found - return key in brackets for debugging
        System.err.println("[L10n] Missing translation: " + key);
        return "[" + key + "]";
    }
    
    /**
     * Check if a translation exists for the given key.
     */
    public static boolean hasKey(String key) {
        return messages.containsKey(key);
    }
    
    /**
     * Get current locale.
     */
    public static Locale getLocale() {
        return currentLocale;
    }
    
    /**
     * Get current language code (e.g., "en", "ru").
     */
    public static String getLanguage() {
        return currentLocale.getLanguage();
    }
    
    /**
     * Reload translations from files.
     * Call this if user has modified translation files.
     */
    public static void reload() {
        messages = new Properties();
        messages.putAll(fallback);
        loadFromFile(messages, currentLocale);
        System.out.println("[L10n] Translations reloaded");
    }
    
    /**
     * Apply saved language from NConfig.
     * Call this after NConfig is loaded.
     */
    public static void applySavedLanguage() {
        try {
            Object langPref = NConfig.get(NConfig.Key.language);
            if (langPref != null && !langPref.toString().isEmpty()) {
                String savedLang = langPref.toString();
                if (!savedLang.equals(currentLocale.getLanguage())) {
                    setLanguage(savedLang);
                    System.out.println("[L10n] Applied saved language: " + savedLang);
                }
            }
        } catch (Exception e) {
            // NConfig not ready yet, ignore
        }
    }
    
    /**
     * Get the path to the lang directory for user reference.
     */
    public static Path getLangDirectory() {
        return langDir;
    }
    
    /**
     * Export default translations to a file for users to customize.
     * 
     * @param langCode Language code for the file name
     */
    public static void exportTemplate(String langCode) {
        if (langDir == null) return;
        
        Path file = langDir.resolve(BUNDLE_NAME + "_" + langCode + ".properties");
        try {
            // Don't overwrite existing files
            if (Files.exists(file)) {
                System.out.println("[L10n] File already exists: " + file);
                return;
            }
            
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("# Nurgling Client Translation Template\n");
                writer.write("# Language: " + langCode + "\n");
                writer.write("# \n");
                writer.write("# Copy this file and translate values (right side of =)\n");
                writer.write("# Keep the keys (left side of =) unchanged!\n");
                writer.write("# \n\n");
                
                // Write all fallback keys as template
                TreeMap<String, String> sorted = new TreeMap<>();
                for (String key : fallback.stringPropertyNames()) {
                    sorted.put(key, fallback.getProperty(key));
                }
                
                String lastSection = "";
                for (Map.Entry<String, String> entry : sorted.entrySet()) {
                    String key = entry.getKey();
                    String section = key.contains(".") ? key.substring(0, key.indexOf('.')) : "";
                    
                    if (!section.equals(lastSection)) {
                        writer.write("\n# === " + section.toUpperCase() + " ===\n");
                        lastSection = section;
                    }
                    
                    writer.write(key + "=" + entry.getValue() + "\n");
                }
            }
            System.out.println("[L10n] Template exported to: " + file);
        } catch (IOException e) {
            System.err.println("[L10n] Failed to export template: " + e.getMessage());
        }
    }
}

