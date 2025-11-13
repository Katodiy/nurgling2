package nurgling.translation;

import haven.*;
import java.awt.Color;

/**
 * Translation-aware text rendering wrapper
 * Integrates with the hybrid translation system to automatically translate text
 */
public class TranslatedText {

    /**
     * Render text with automatic translation detection
     */
    public static Text.Line render(String text, Color color) {
        // Try translation first
        String translated = TranslationManager.getInstance()
            .translate(text, TranslationType.AUTO_DETECT);

        // Use original Text.render with translated text
        return Text.render(translated, color);
    }

    /**
     * Render text with automatic translation detection (white color)
     */
    public static Text.Line render(String text) {
        return render(text, Color.WHITE);
    }

    /**
     * Render text with specific translation type
     */
    public static Text.Line render(String text, Color color, TranslationType type) {
        String translated = TranslationManager.getInstance()
            .translate(text, type);

        return Text.render(translated, color);
    }

    /**
     * Render formatted text with automatic translation
     */
    public static Text.Line renderf(Color color, String format, Object... args) {
        // Translate the format string first
        String translatedFormat = TranslationManager.getInstance()
            .translate(format, TranslationType.AUTO_DETECT);

        // Then format with arguments
        return Text.renderf(color, translatedFormat, args);
    }

    /**
     * Render static UI text (forces static translation lookup)
     */
    public static Text.Line renderStatic(String key, Color color) {
        String translated = TranslationManager.getInstance()
            .translateStatic(key);

        return Text.render(translated, color);
    }

    /**
     * Render static UI text with parameters
     */
    public static Text.Line renderStatic(String key, Color color, Object... params) {
        String translated = TranslationManager.getInstance()
            .translateStatic(key, params);

        return Text.render(translated, color);
    }

    /**
     * Render item name (forces item translation lookup)
     */
    public static Text.Line renderItem(String itemName, Color color) {
        String translated = TranslationManager.getInstance()
            .translateItem(itemName);

        return Text.render(translated, color);
    }

    /**
     * Render skill name (forces skill translation lookup)
     */
    public static Text.Line renderSkill(String skillName, Color color) {
        String translated = TranslationManager.getInstance()
            .translateSkill(skillName);

        return Text.render(translated, color);
    }
}