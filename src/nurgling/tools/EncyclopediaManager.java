package nurgling.tools;

import nurgling.NConfig;
import nurgling.tools.markdown.MarkdownDocument;
import nurgling.tools.markdown.MarkdownElement;
import nurgling.tools.markdown.MarkdownParser;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EncyclopediaManager {
    private static EncyclopediaManager instance;
    private final Map<String, MarkdownDocument> documents = new HashMap<>();
    private final Map<String, String> fileToPath = new HashMap<>();
    private final MarkdownParser parser = new MarkdownParser();
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
        
        String encyclopediaPath = (String) NConfig.get(NConfig.Key.encyclopediaPath);
        if (encyclopediaPath == null || encyclopediaPath.isEmpty()) {
            encyclopediaPath = "src/nurgling/docs";
        }
        
        File docsDir = new File(encyclopediaPath);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("Encyclopedia directory not found: " + encyclopediaPath);
            return;
        }
        
        loadDocumentsFromDirectory(docsDir, "");
        loaded = true;
        
        System.out.println("Loaded " + documents.size() + " encyclopedia documents");
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
                try {
                    MarkdownDocument document = parser.parseFile(file);
                    String documentKey = relativePath.isEmpty() ? 
                        file.getName() : relativePath + "/" + file.getName();
                    
                    documents.put(documentKey, document);
                    fileToPath.put(file.getName(), documentKey);
                    
                } catch (IOException e) {
                    System.err.println("Failed to parse markdown file: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public MarkdownDocument getDocument(String key) {
        if (!loaded) loadDocuments();
        return documents.get(key);
    }
    
    public MarkdownDocument getDocumentByFileName(String fileName) {
        if (!loaded) loadDocuments();
        String path = fileToPath.get(fileName);
        return path != null ? documents.get(path) : null;
    }
    
    public Set<String> getAllDocumentKeys() {
        if (!loaded) loadDocuments();
        return new HashSet<>(documents.keySet());
    }
    
    public List<String> getDocumentKeysInDirectory(String directory) {
        if (!loaded) loadDocuments();
        
        List<String> result = new ArrayList<>();
        String searchPrefix = directory.isEmpty() ? "" : directory + "/";
        
        for (String key : documents.keySet()) {
            if (directory.isEmpty()) {
                // Root directory - only files without subdirectories
                if (!key.contains("/")) {
                    result.add(key);
                }
            } else if (key.startsWith(searchPrefix)) {
                // Check if it's directly in this directory (not in subdirectory)
                String remainder = key.substring(searchPrefix.length());
                if (!remainder.contains("/")) {
                    result.add(key);
                }
            }
        }
        
        return result;
    }
    
    public List<String> getSubdirectories(String directory) {
        if (!loaded) loadDocuments();
        
        Set<String> subdirs = new HashSet<>();
        String searchPrefix = directory.isEmpty() ? "" : directory + "/";
        
        for (String key : documents.keySet()) {
            if (directory.isEmpty()) {
                // Root directory - find first-level subdirectories
                if (key.contains("/")) {
                    String firstPart = key.substring(0, key.indexOf("/"));
                    subdirs.add(firstPart);
                }
            } else if (key.startsWith(searchPrefix)) {
                String remainder = key.substring(searchPrefix.length());
                if (remainder.contains("/")) {
                    String firstSubdir = remainder.substring(0, remainder.indexOf("/"));
                    subdirs.add(searchPrefix + firstSubdir);
                }
            }
        }
        
        return new ArrayList<>(subdirs);
    }
    
    public List<MarkdownDocument> searchDocuments(String query) {
        if (!loaded) loadDocuments();
        
        List<MarkdownDocument> results = new ArrayList<>();
        String lowercaseQuery = query.toLowerCase();
        
        for (MarkdownDocument document : documents.values()) {
            // Search in title
            if (document.getTitle().toLowerCase().contains(lowercaseQuery)) {
                results.add(document);
                continue;
            }
            
            // Search in content
            boolean found = false;
            for (MarkdownElement element : document.elements) {
                if (element.getContent().toLowerCase().contains(lowercaseQuery)) {
                    results.add(document);
                    found = true;
                    break;
                }
            }
        }
        
        return results;
    }
    
    public void reload() {
        documents.clear();
        fileToPath.clear();
        loaded = false;
        loadDocuments();
    }
}