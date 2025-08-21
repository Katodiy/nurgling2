package nurgling.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class EncyclopediaManager {
    private final Map<String, File> documentFiles = new HashMap<>();
    private final Map<String, String> documentTitles = new HashMap<>();
    
    public EncyclopediaManager() {
        loadDocuments();
    }
    
    private void loadDocuments() {
        String encyclopediaPath = "src/nurgling/docs";
        File docsDir = new File(encyclopediaPath);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("Encyclopedia directory not found: " + encyclopediaPath);
            return;
        }
        
        loadDocumentsFromDirectory(docsDir, "");
    }
    
    private void loadDocumentsFromDirectory(File directory, String relativePath) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            String relativePathToFiles = relativePath.isEmpty() ?
                    file.getName() : relativePath + "/" + file.getName();
            if (file.isDirectory()) {
                loadDocumentsFromDirectory(file, relativePathToFiles);
            } else if (file.getName().toLowerCase().endsWith(".md")) {
                String title = extractTitleFromFile(file);
                documentFiles.put(relativePathToFiles, file);
                documentTitles.put(relativePathToFiles, title);
            }
        }
    }
    
    public File getDocumentFile(String key) {
        return documentFiles.get(key);
    }
    
    public Set<String> getAllDocumentKeys() {
        return new HashSet<>(documentFiles.keySet());
    }
    
    public String getDocumentTitle(String key) {
        return documentTitles.getOrDefault(key, key);
    }
    
    private String extractTitleFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.startsWith("#")) {
                // Remove markdown header markers and trim
                return firstLine.replaceFirst("^#+\\s*", "").trim();
            } else if (firstLine != null && !firstLine.trim().isEmpty()) {
                // Use first non-empty line if no header
                return firstLine.trim();
            } else {
                // Fallback to filename without extension
                String name = file.getName();
                return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
            }
        } catch (Exception e) {
            // Fallback to filename without extension
            String name = file.getName();
            return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
        }
    }
}