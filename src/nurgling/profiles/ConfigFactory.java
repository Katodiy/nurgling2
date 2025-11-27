package nurgling.profiles;

import nurgling.NConfig;

/**
 * Factory for creating and managing configuration instances.
 * Provides centralized access to both global and profile-specific configurations.
 */
public class ConfigFactory {

    /**
     * Gets the appropriate NConfig instance for the given genus.
     * Returns global instance if genus is null or empty, otherwise returns profile-specific instance.
     */
    public static NConfig getConfig(String genus) {
        if (genus == null || genus.isEmpty()) {
            return NConfig.getGlobalInstance();
        }
        return NConfig.getProfileInstance(genus);
    }

    /**
     * Initializes a profile for the given genus.
     * This ensures the profile directory exists and migrates existing configs if needed.
     */
    public static void initializeProfile(String genus) {
        if (genus != null && !genus.isEmpty()) {
            // Getting the profile instance will automatically trigger initialization
            // through the ProfileManager in the NConfig constructor
            NConfig.getProfileInstance(genus);
            System.out.println("Initialized profile for world: " + genus);
        }
    }

    /**
     * Gets the global (non-profiled) configuration instance.
     */
    public static NConfig getGlobalConfig() {
        return NConfig.getGlobalInstance();
    }

    /**
     * Gets a profile-specific configuration instance.
     */
    public static NConfig getProfileConfig(String genus) {
        if (genus == null || genus.isEmpty()) {
            throw new IllegalArgumentException("Genus cannot be null or empty for profile config");
        }
        return NConfig.getProfileInstance(genus);
    }

    /**
     * Checks if a profile exists for the given genus.
     */
    public static boolean profileExists(String genus) {
        if (genus == null || genus.isEmpty()) {
            return false;
        }
        ProfileManager manager = new ProfileManager(genus);
        return manager.exists();
    }

    /**
     * Creates a new profile manager for the given genus (useful for utilities).
     */
    public static ProfileManager createProfileManager(String genus) {
        if (genus == null || genus.isEmpty()) {
            throw new IllegalArgumentException("Genus cannot be null or empty");
        }
        return new ProfileManager(genus);
    }
}