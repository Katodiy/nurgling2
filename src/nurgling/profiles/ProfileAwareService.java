package nurgling.profiles;

/**
 * Interface for services that need to be aware of world-specific profiles.
 * Services implementing this interface can handle configuration loading
 * and migration for different worlds based on genus.
 */
public interface ProfileAwareService {

    /**
     * Initializes the service for a specific profile (genus).
     * This should set up the service to use profile-specific configuration
     * and perform any necessary migration from global configs.
     *
     * @param genus The world identifier for this profile
     */
    void initializeForProfile(String genus);

    /**
     * Migrates existing global configuration to the profile.
     * This should only be called during first-time profile creation.
     */
    void migrateFromGlobal();

    /**
     * Gets the configuration file name that this service manages.
     * Used for automatic migration and path resolution.
     *
     * @return The configuration file name (e.g., "fish_locations.nurgling.json")
     */
    String getConfigFileName();

    /**
     * Gets the current genus (world identifier) for this service instance.
     * May return null if this is a global (non-profiled) instance.
     *
     * @return The genus identifier or null for global instances
     */
    String getGenus();

    /**
     * Checks if this service instance is profile-specific.
     *
     * @return true if this is a profile-specific instance, false if global
     */
    default boolean isProfiled() {
        return getGenus() != null && !getGenus().isEmpty();
    }

    /**
     * Loads the configuration from the appropriate location (profile or global).
     * This should be called after initialization to populate the service state.
     */
    void load();

    /**
     * Saves the current configuration to the appropriate location.
     */
    void save();

    /**
     * Gets the full path to the configuration file for this service.
     * Should return profile-specific path if profiled, global path otherwise.
     *
     * @return Full path to the configuration file
     */
    String getConfigPath();
}