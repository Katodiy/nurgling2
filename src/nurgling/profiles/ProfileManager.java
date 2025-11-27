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

    // Configuration files that should be world-specific
    private static final List<String> PROFILE_CONFIG_FILES = Arrays.asList(
        "areas.nurgling.json",
        "routes.nurgling.json",
        "explored.nurgling.json",
        "fish_locations.nurgling.json",
        "fog.nurgling.json",
        "resource_timers.nurgling.json",
        "tree_locations.nurgling.json",
        "cheese_orders.nurgling.json",
        "scenarios.nurgling.json"
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
     * Gets the profile directory path
     */
    public Path getProfilePath() {
        return profilePath;
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

                // Migrate existing configurations
                migrateExistingConfigs();
            }
        } catch (IOException e) {
            System.err.println("Failed to create profile directory for " + genus + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migrates existing configuration files from the base directory to this profile
     */
    private void migrateExistingConfigs() {
        System.out.println("Migrating existing configurations to profile: " + genus);

        for (String filename : PROFILE_CONFIG_FILES) {
            migrateConfigFile(filename);
        }

        // Migrate .dat files (except searchcmd.dat)
        migrateDatFiles();

        System.out.println("Migration completed for profile: " + genus);
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
     * Checks if a configuration file exists in this profile
     */
    public boolean hasConfig(String filename) {
        return Files.exists(getConfigPath(filename));
    }

    /**
     * Checks if the profile directory exists
     */
    public boolean exists() {
        return Files.exists(profilePath);
    }

    /**
     * Creates a backup of a configuration file before modification
     */
    public void backupConfig(String filename) {
        try {
            Path configPath = getConfigPath(filename);
            if (Files.exists(configPath)) {
                Path backupPath = configPath.resolveSibling(filename + ".backup");
                Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Failed to backup " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Gets the fallback (global) path for a configuration file
     */
    public String getFallbackPath(String filename) {
        return baseConfigPath.resolve(filename).toString();
    }

    @Override
    public String toString() {
        return "ProfileManager{genus='" + genus + "', profilePath=" + profilePath + "}";
    }
}