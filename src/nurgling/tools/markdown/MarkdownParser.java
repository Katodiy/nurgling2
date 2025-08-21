package nurgling.tools.markdown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    
    public MarkdownDocument parseFile(File file) throws IOException {
        MarkdownDocument document = new MarkdownDocument(file.getName());
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder paragraphBuffer = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Empty line - end current paragraph if any
                if (line.isEmpty()) {
                    if (paragraphBuffer.length() > 0) {
                        document.addElement(parseParagraph(paragraphBuffer.toString()));
                        paragraphBuffer.setLength(0);
                    }
                    continue;
                }
                
                // Check for headers
                Matcher headerMatcher = HEADER_PATTERN.matcher(line);
                if (headerMatcher.matches()) {
                    // End current paragraph if any
                    if (paragraphBuffer.length() > 0) {
                        document.addElement(parseParagraph(paragraphBuffer.toString()));
                        paragraphBuffer.setLength(0);
                    }
                    
                    int level = headerMatcher.group(1).length();
                    String text = headerMatcher.group(2);
                    document.addElement(new MarkdownElement.Header(level, text));
                    continue;
                }
                
                // Add to current paragraph
                if (paragraphBuffer.length() > 0) {
                    paragraphBuffer.append(" ");
                }
                paragraphBuffer.append(line);
            }
            
            // Add final paragraph if any
            if (paragraphBuffer.length() > 0) {
                document.addElement(parseParagraph(paragraphBuffer.toString()));
            }
        }
        
        return document;
    }
    
    private MarkdownElement.Paragraph parseParagraph(String text) {
        // For now, return as simple paragraph - inline parsing will be added later
        return new MarkdownElement.Paragraph(text);
    }
    
    public List<MarkdownElement> parseInlineElements(String text) {
        List<MarkdownElement> elements = new ArrayList<>();
        String remaining = text;
        
        while (!remaining.isEmpty()) {
            // Find the earliest match among all patterns
            int earliestIndex = remaining.length();
            String matchType = "";
            Matcher earliestMatcher = null;
            
            // Check for images first (they include links)
            Matcher imageMatcher = IMAGE_PATTERN.matcher(remaining);
            if (imageMatcher.find() && imageMatcher.start() < earliestIndex) {
                earliestIndex = imageMatcher.start();
                matchType = "image";
                earliestMatcher = imageMatcher;
            }
            
            // Check for links
            Matcher linkMatcher = LINK_PATTERN.matcher(remaining);
            if (linkMatcher.find() && linkMatcher.start() < earliestIndex) {
                earliestIndex = linkMatcher.start();
                matchType = "link";
                earliestMatcher = linkMatcher;
            }
            
            // Check for bold
            Matcher boldMatcher = BOLD_PATTERN.matcher(remaining);
            if (boldMatcher.find() && boldMatcher.start() < earliestIndex) {
                earliestIndex = boldMatcher.start();
                matchType = "bold";
                earliestMatcher = boldMatcher;
            }
            
            // Check for italic
            Matcher italicMatcher = ITALIC_PATTERN.matcher(remaining);
            if (italicMatcher.find() && italicMatcher.start() < earliestIndex) {
                earliestIndex = italicMatcher.start();
                matchType = "italic";
                earliestMatcher = italicMatcher;
            }
            
            // Add text before the match as plain text
            if (earliestIndex > 0) {
                String beforeText = remaining.substring(0, earliestIndex);
                if (!beforeText.trim().isEmpty()) {
                    elements.add(new MarkdownElement.Text(beforeText));
                }
            }
            
            // Process the match
            if (earliestMatcher != null) {
                switch (matchType) {
                    case "image":
                        elements.add(new MarkdownElement.Image(
                            earliestMatcher.group(1), 
                            earliestMatcher.group(2)
                        ));
                        break;
                    case "link":
                        elements.add(new MarkdownElement.Link(
                            earliestMatcher.group(1), 
                            earliestMatcher.group(2)
                        ));
                        break;
                    case "bold":
                        elements.add(new MarkdownElement.Bold(earliestMatcher.group(1)));
                        break;
                    case "italic":
                        elements.add(new MarkdownElement.Italic(earliestMatcher.group(1)));
                        break;
                }
                remaining = remaining.substring(earliestMatcher.end());
            } else {
                // No more matches, add remaining text
                if (!remaining.trim().isEmpty()) {
                    elements.add(new MarkdownElement.Text(remaining));
                }
                break;
            }
        }
        
        return elements;
    }
}