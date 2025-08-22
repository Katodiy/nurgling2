package nurgling.tools;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class EncyclopediaManager {
    private final Map<String, String> documentPaths = new HashMap<>();
    private final Map<String, String> documentTitles = new HashMap<>();
    
    public EncyclopediaManager() {
        loadDocuments();
    }
    
    private void loadDocuments() {
        try {
            String resourcePath = "/nurgling/docs";
            URL resourceUrl = getClass().getResource(resourcePath);
            
            if (resourceUrl != null) {
                URI resourceUri = resourceUrl.toURI();
                Path docsPath;
                
                if (resourceUri.getScheme().equals("jar")) {
                    // Running from JAR - create filesystem for the JAR
                    FileSystem fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
                    docsPath = fileSystem.getPath(resourcePath);
                } else {
                    // Running from IDE/file system
                    docsPath = Paths.get(resourceUri);
                }
                
                loadDocumentsFromPath(docsPath, "");
            } else {
                System.err.println("Encyclopedia docs not found in resources");
            }
        } catch (Exception e) {
            System.err.println("Failed to load encyclopedia documents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadDocumentsFromPath(Path directory, String relativePath) {
        try {
            if (!Files.exists(directory)) return;
            
            try (Stream<Path> files = Files.list(directory)) {
                files.forEach(file -> {
                    String fileName = file.getFileName().toString();
                    String relativePathToFile = relativePath.isEmpty() ? 
                        fileName : relativePath + "/" + fileName;
                    
                    if (Files.isDirectory(file)) {
                        loadDocumentsFromPath(file, relativePathToFile);
                    } else if (fileName.toLowerCase().endsWith(".md")) {
                        String title = extractTitleFromResource(file);
                        documentPaths.put(relativePathToFile, "/nurgling/docs/" + relativePathToFile);
                        documentTitles.put(relativePathToFile, title);
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Error reading directory: " + directory + " - " + e.getMessage());
        }
    }
    
    public InputStream getDocumentStream(String key) {
        String resourcePath = documentPaths.get(key);
        if (resourcePath != null) {
            return getClass().getResourceAsStream(resourcePath);
        }
        return null;
    }
    
    public String getDocumentContent(String key) {
        try (InputStream stream = getDocumentStream(key)) {
            if (stream != null) {
                return new String(stream.readAllBytes());
            }
        } catch (IOException e) {
            System.err.println("Failed to read document: " + key + " - " + e.getMessage());
        }
        return null;
    }
    
    public Set<String> getAllDocumentKeys() {
        return new HashSet<>(documentPaths.keySet());
    }
    
    public String getDocumentTitle(String key) {
        return documentTitles.getOrDefault(key, key);
    }
    
    private String extractTitleFromResource(Path file) {
        try {
            String firstLine = Files.lines(file).findFirst().orElse("");
            if (firstLine.startsWith("#")) {
                // Remove markdown header markers and trim
                return firstLine.replaceFirst("^#+\\s*", "").trim();
            } else if (!firstLine.trim().isEmpty()) {
                // Use first non-empty line if no header
                return firstLine.trim();
            } else {
                // Fallback to filename without extension
                String name = file.getFileName().toString();
                return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
            }
        } catch (Exception e) {
            // Fallback to filename without extension
            String name = file.getFileName().toString();
            return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
        }
    }
}