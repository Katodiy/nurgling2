package nurgling.tools.markdown;

import java.util.ArrayList;
import java.util.List;

public class MarkdownDocument {
    public final String title;
    public final String fileName;
    public final List<MarkdownElement> elements;
    
    public MarkdownDocument(String fileName) {
        this.fileName = fileName;
        this.title = extractTitleFromFileName(fileName);
        this.elements = new ArrayList<>();
    }
    
    public void addElement(MarkdownElement element) {
        elements.add(element);
    }
    
    public String getTitle() {
        // Try to find first header element for title
        for (MarkdownElement element : elements) {
            if (element instanceof MarkdownElement.Header) {
                return element.getContent();
            }
        }
        return title;
    }
    
    private String extractTitleFromFileName(String fileName) {
        if (fileName == null) return "Unknown";
        
        // Remove .md extension
        String name = fileName;
        if (name.endsWith(".md")) {
            name = name.substring(0, name.length() - 3);
        }
        
        // Replace underscores and hyphens with spaces
        name = name.replace("_", " ").replace("-", " ");
        
        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (words[i].length() > 0) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
}