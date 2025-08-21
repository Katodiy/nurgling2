package nurgling.tools;

import java.io.File;
import java.util.*;

public class EncyclopediaManager {
    private static EncyclopediaManager instance;
    private final Map<String, File> documentFiles = new HashMap<>();
    private final Map<String, String> fileToPath = new HashMap<>();
    private boolean loaded = false;
    
    private EncyclopediaManager() {}
    
    public static EncyclopediaManager getInstance() {
        if (instance == null) {
            instance = new EncyclopediaManager();
        }
        return instance;
    }
    
    public void loadDocuments() {
        if (loaded) return;
        
        String encyclopediaPath = "src/nurgling/docs";
        File docsDir = new File(encyclopediaPath);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("Encyclopedia directory not found: " + encyclopediaPath);
            return;
        }
        
        loadDocumentsFromDirectory(docsDir, "");
        loaded = true;
        
        System.out.println("Loaded " + documentFiles.size() + " encyclopedia documents");
    }
    
    private void loadDocumentsFromDirectory(File directory, String relativePath) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                String newRelativePath = relativePath.isEmpty() ? 
                    file.getName() : relativePath + "/" + file.getName();
                loadDocumentsFromDirectory(file, newRelativePath);
            } else if (file.getName().toLowerCase().endsWith(".md")) {
                String documentKey = relativePath.isEmpty() ? 
                    file.getName() : relativePath + "/" + file.getName();
                
                documentFiles.put(documentKey, file);
                fileToPath.put(file.getName(), documentKey);
            }
        }
    }
    
    public File getDocumentFile(String key) {
        if (!loaded) loadDocuments();
        return documentFiles.get(key);
    }
    
    public Set<String> getAllDocumentKeys() {
        if (!loaded) loadDocuments();
        return new HashSet<>(documentFiles.keySet());
    }
}