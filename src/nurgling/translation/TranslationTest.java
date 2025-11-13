package nurgling.translation;

/**
 * Simple test class to demonstrate translation functionality
 * Can be called from anywhere in the game to test translations
 */
public class TranslationTest {

    /**
     * Test method to demonstrate translation functionality
     * Call this from anywhere to see if translations are working
     */
    public static void testTranslations() {
        TranslationManager tm = TranslationManager.getInstance();

        System.out.println("=== Translation System Test ===");

        // Test static UI translations
        System.out.println("Static UI Test:");
        System.out.println("  Back = " + tm.translateStatic("Back"));
        System.out.println("  Video settings = " + tm.translateStatic("Video settings"));
        System.out.println("  Close = " + tm.translateStatic("Close"));
        System.out.println("  Master audio volume = " + tm.translateStatic("Master audio volume"));

        // Test dynamic item translations
        System.out.println("Dynamic Item Test:");
        System.out.println("  Chicken Egg = " + tm.translateItem("Chicken Egg"));
        System.out.println("  Poppy Flower = " + tm.translateItem("Poppy Flower"));
        System.out.println("  Straw = " + tm.translateItem("Straw"));

        // Test skill translations
        System.out.println("Skill Test:");
        System.out.println("  Carpentry = " + tm.translateSkill("Carpentry"));
        System.out.println("  Farming = " + tm.translateSkill("Farming"));

        // Test auto-detection
        System.out.println("Auto-detect Test:");
        System.out.println("  Apple (item) = " + tm.translate("Apple", TranslationType.AUTO_DETECT));
        System.out.println("  button.cancel (UI) = " + tm.translate("button.cancel", TranslationType.AUTO_DETECT));

        // Print stats
        System.out.println("Translation Stats: " + tm.getStats());
        System.out.println("Current Language: " + tm.getCurrentLanguage());
        System.out.println("Available Languages: " + tm.getAvailableLanguages());
        System.out.println("=== Test Complete ===");
    }
}