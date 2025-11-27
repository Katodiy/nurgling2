package nurgling.profiles;

import haven.HashDirCache;
import haven.ResCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * Manages world-specific configuration profiles based on genus identifier.
 * Provides path resolution, migration, and backwards compatibility for
 * per-world configuration storage.
 */
public class ProfileManager {
    private final String genus;
    private final Path baseConfigPath;
    private final Path profilePath;

    // World 16 genus identifier - the legacy world that should inherit existing configurations
    private static final String WORLD_16_GENUS = "c646473983afec09";

    // Configuration files that should be world-specific
    private static final List<String> PROFILE_CONFIG_FILES = Arrays.asList(
        "areas.nurgling.json",
        "routes.nurgling.json",
        "explored.nurgling.json",
        "fish_locations.nurgling.json",
        "fog.nurgling.json",
        "resource_timers.nurgling.json",
        "tree_locations.nurgling.json",
        "cheese_orders.nurgling.json"
    );

    public ProfileManager(String genus) {
        this.genus = genus;
        this.baseConfigPath = getBaseConfigPath();
        this.profilePath = baseConfigPath.resolve("profiles").resolve(genus);
    }

    /**
     * Gets the base configuration path (Haven and Hearth app data directory)
     */
    private Path getBaseConfigPath() {
        try {
            String basePath = ((HashDirCache) ResCache.global).base + "\\..\\";
            return Paths.get(basePath).normalize();
        } catch (Exception e) {
            // Fallback to standard AppData location
            String appdata = System.getenv("APPDATA");
            if (appdata == null) {
                appdata = System.getProperty("user.home");
            }
            return Paths.get(appdata, "Haven and Hearth");
        }
    }

    /**
     * Gets the full path for a configuration file in this profile
     */
    public Path getConfigPath(String filename) {
        return profilePath.resolve(filename);
    }

    /**
     * Gets the string path for a configuration file in this profile
     */
    public String getConfigPathString(String filename) {
        return getConfigPath(filename).toString();
    }

    /**
     * Gets the genus identifier for this profile
     */
    public String getGenus() {
        return genus;
    }

    /**
     * Ensures the profile directory exists and performs migration if needed
     */
    public void ensureProfileExists() {
        try {
            // Create profile directory if it doesn't exist
            if (!Files.exists(profilePath)) {
                Files.createDirectories(profilePath);
                System.out.println("Created profile directory for world: " + genus);

                // Only migrate existing configurations for World 16 (legacy world)
                // For all other worlds, start with fresh profiles
                if (WORLD_16_GENUS.equals(genus)) {
                    System.out.println("Migrating existing configurations for legacy world: " + genus);
                    migrateExistingConfigs();
                } else {
                    System.out.println("Starting with fresh profile for new world: " + genus);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to create profile directory for " + genus + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migrates existing configuration files from the base directory to this profile.
     * This is only called for World 16 (the legacy world) to preserve existing user
     * configurations. New worlds start with fresh profiles.
     */
    private void migrateExistingConfigs() {
        System.out.println("Migrating existing configurations to legacy profile: " + genus);

        for (String filename : PROFILE_CONFIG_FILES) {
            migrateConfigFile(filename);
        }

        // Migrate .dat files (except searchcmd.dat)
        migrateDatFiles();

        System.out.println("Legacy migration completed for profile: " + genus);
    }

    /**
     * Migrates a single configuration file
     */
    private void migrateConfigFile(String filename) {
        try {
            Path sourcePath = baseConfigPath.resolve(filename);
            Path targetPath = getConfigPath(filename);

            if (Files.exists(sourcePath) && !Files.exists(targetPath)) {
                Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                System.out.println("  Migrated: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Failed to migrate " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Migrates .dat files (excluding searchcmd.dat which should remain global)
     */
    private void migrateDatFiles() {
        try {
            if (Files.exists(baseConfigPath)) {
                Files.list(baseConfigPath)
                    .filter(path -> path.toString().endsWith(".dat"))
                    .filter(path -> !path.getFileName().toString().equals("searchcmd.dat"))
                    .forEach(this::migrateDatFile);
            }
        } catch (IOException e) {
            System.err.println("Failed to migrate .dat files: " + e.getMessage());
        }
    }

    /**
     * Migrates a single .dat file
     */
    private void migrateDatFile(Path datFile) {
        try {
            String filename = datFile.getFileName().toString();
            Path targetPath = getConfigPath(filename);

            if (!Files.exists(targetPath)) {
                Files.copy(datFile, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                System.out.println("  Migrated: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Failed to migrate " + datFile.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * Checks if the profile directory exists
     */
    public boolean exists() {
        return Files.exists(profilePath);
    }

    @Override
    public String toString() {
        return "ProfileManager{genus='" + genus + "', profilePath=" + profilePath + "}";
    }
}