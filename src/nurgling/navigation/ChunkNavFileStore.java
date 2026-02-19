package nurgling.navigation;

import nurgling.profiles.ProfileManager;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Manages file storage for ChunkNav data.
 * Stores each chunk as a separate binary file in a directory.
 */
public class ChunkNavFileStore {

    private static final String CHUNK_EXTENSION = ".chunk";

    private final String genus;
    private final Path chunkDirectory;

    public ChunkNavFileStore(String genus) {
        this.genus = genus;
        ProfileManager pm = new ProfileManager(genus);
        this.chunkDirectory = pm.getConfigPath(ChunkNavConfig.STORAGE_DIRNAME);
    }

    /**
     * Get the directory where chunk files are stored.
     */
    public Path getChunkDirectory() {
        return chunkDirectory;
    }

    /**
     * Get the path to a specific chunk file.
     */
    public Path getChunkFile(long gridId) {
        return chunkDirectory.resolve(gridId + CHUNK_EXTENSION);
    }

    /**
     * Ensure the chunk directory exists.
     */
    public void ensureDirectoryExists() throws IOException {
        Files.createDirectories(chunkDirectory);
    }

    /**
     * Save a single chunk to its binary file.
     * Uses atomic write pattern (write to temp, then rename).
     */
    public void saveChunk(ChunkNavData chunk) throws IOException {
        ensureDirectoryExists();

        Path chunkFile = getChunkFile(chunk.gridId);
        Path tempFile = chunkFile.resolveSibling(chunk.gridId + ".tmp");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
            ChunkNavBinaryFormat.writeChunk(chunk, out);
        }

        // Atomic rename
        try {
            Files.move(tempFile, chunkFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tempFile, chunkFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Load a single chunk from its binary file.
     * Returns null if file doesn't exist or is corrupted.
     */
    public ChunkNavData loadChunk(long gridId) {
        Path chunkFile = getChunkFile(gridId);
        if (!Files.exists(chunkFile)) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(chunkFile)))) {
            return ChunkNavBinaryFormat.readChunk(in);
        } catch (IOException e) {
            System.err.println("ChunkNav: Failed to load chunk " + gridId + ": " + e.getMessage());
            // Delete corrupted file
            deleteCorruptedFile(chunkFile, gridId);
            return null;
        }
    }

    /**
     * Load all chunks from the directory.
     * Returns a list of successfully loaded chunks.
     * Corrupted files are deleted.
     */
    public List<ChunkNavData> loadAllChunks() {
        List<ChunkNavData> chunks = new ArrayList<>();

        if (!Files.exists(chunkDirectory)) {
            return chunks;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunkDirectory, "*" + CHUNK_EXTENSION)) {
            for (Path file : stream) {
                try (DataInputStream in = new DataInputStream(
                        new BufferedInputStream(Files.newInputStream(file)))) {
                    ChunkNavData chunk = ChunkNavBinaryFormat.readChunk(in);
                    chunks.add(chunk);
                } catch (IOException e) {
                    String filename = file.getFileName().toString();
                    System.err.println("ChunkNav: Failed to load " + filename + ": " + e.getMessage());
                    // Extract gridId from filename and delete
                    try {
                        String gridIdStr = filename.replace(CHUNK_EXTENSION, "");
                        long gridId = Long.parseLong(gridIdStr);
                        deleteCorruptedFile(file, gridId);
                    } catch (NumberFormatException nfe) {
                        // Can't parse filename, just delete it
                        try {
                            Files.delete(file);
                            System.out.println("ChunkNav: Deleted corrupted file: " + filename);
                        } catch (IOException deleteError) {
                            System.err.println("ChunkNav: Failed to delete corrupted file: " + filename);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ChunkNav: Failed to list chunk directory: " + e.getMessage());
        }

        return chunks;
    }

    /**
     * Delete a chunk file.
     */
    public void deleteChunkFile(long gridId) {
        Path chunkFile = getChunkFile(gridId);
        try {
            Files.deleteIfExists(chunkFile);
        } catch (IOException e) {
            System.err.println("ChunkNav: Failed to delete chunk " + gridId + ": " + e.getMessage());
        }
    }

    /**
     * Delete all chunk files from disk.
     * @return The number of files deleted
     */
    public int deleteAllChunkFiles() {
        if (!Files.exists(chunkDirectory)) {
            return 0;
        }

        int deleted = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunkDirectory, "*" + CHUNK_EXTENSION)) {
            for (Path file : stream) {
                try {
                    Files.delete(file);
                    deleted++;
                } catch (IOException e) {
                    System.err.println("ChunkNav: Failed to delete file " + file + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("ChunkNav: Failed to list chunk directory for deletion: " + e.getMessage());
        }
        return deleted;
    }

    /**
     * Delete a corrupted file and log.
     */
    private void deleteCorruptedFile(Path file, long gridId) {
        try {
            Files.delete(file);
            System.out.println("ChunkNav: Deleted corrupted chunk file: " + gridId);
        } catch (IOException e) {
            System.err.println("ChunkNav: Failed to delete corrupted file " + gridId + ": " + e.getMessage());
        }
    }

    /**
     * Get count of chunk files in directory.
     */
    public int getChunkCount() {
        if (!Files.exists(chunkDirectory)) {
            return 0;
        }

        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunkDirectory, "*" + CHUNK_EXTENSION)) {
            for (Path ignored : stream) {
                count++;
            }
        } catch (IOException e) {
            return 0;
        }
        return count;
    }

    /**
     * Check if the old JSON file exists (for migration).
     */
    public Path getOldJsonFilePath() {
        ProfileManager pm = new ProfileManager(genus);
        return pm.getConfigPath(ChunkNavConfig.STORAGE_FILENAME);
    }

    /**
     * Check if migration from JSON is needed.
     */
    public boolean needsMigration() {
        Path oldJson = getOldJsonFilePath();
        return Files.exists(oldJson) && getChunkCount() == 0;
    }

    /**
     * Delete the old JSON file after successful migration.
     */
    public void deleteOldJsonFile() {
        Path oldJson = getOldJsonFilePath();
        try {
            Files.deleteIfExists(oldJson);
            System.out.println("ChunkNav: Deleted old JSON file after migration");
        } catch (IOException e) {
            System.err.println("ChunkNav: Failed to delete old JSON file: " + e.getMessage());
        }
    }

    /**
     * Clean up any orphaned temp files.
     */
    public void cleanupTempFiles() {
        if (!Files.exists(chunkDirectory)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunkDirectory, "*.tmp")) {
            for (Path file : stream) {
                try {
                    Files.delete(file);
                    System.out.println("ChunkNav: Cleaned up orphaned temp file: " + file.getFileName());
                } catch (IOException e) {
                    // Ignore
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Check if the instance migration (neighbor wipe) needs to run.
     * This is a one-time migration when upgrading to V2 binary format with instanceId support.
     */
    public boolean needsInstanceMigration() {
        return !Files.exists(chunkDirectory.resolve(".instance_migrated"));
    }

    /**
     * Mark the instance migration as complete.
     */
    public void markInstanceMigrationDone() {
        try {
            Files.createDirectories(chunkDirectory);
            Files.createFile(chunkDirectory.resolve(".instance_migrated"));
        } catch (IOException e) {
            // Ignore - migration will re-run next time
        }
    }
}
