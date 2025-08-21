package nurgling.tools;

import java.io.File;
import java.util.*;

public class EncyclopediaManager {
    private final Map<String, File> documentFiles = new HashMap<>();
    
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

                documentFiles.put(relativePathToFiles, file);
            }
        }
    }
    
    public File getDocumentFile(String key) {
        return documentFiles.get(key);
    }
    
    public Set<String> getAllDocumentKeys() {
        return new HashSet<>(documentFiles.keySet());
    }
}