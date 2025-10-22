package nurgling.tools;

import haven.UI;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class MarkdownToImageRenderer {
    
    public static BufferedImage renderMarkdownToImage(String markdown, int maxWidth, String documentKey) {
        // Step 1: Determine maximum content width (account for margins)
        int margin = 15;
        int contentWidth = maxWidth - (margin * 2);
        
        // Step 2: Parse markdown into elements
        java.util.List<MarkdownElement> elements = parseMarkdownToElements(markdown, documentKey);
        
        // Step 3: Compile document layout (fit elements to lines)
        java.util.List<DocumentLine> documentLines = compileDocumentLayout(elements, contentWidth);
        
        // Step 4: Calculate total height needed
        int lineHeight = 20;
        int totalHeight = calculateTotalHeight(documentLines, lineHeight, contentWidth, documentKey) + margin * 2;
        
        // Step 5: Render the final image
        return renderDocumentLines(documentLines, maxWidth, totalHeight, margin, lineHeight);
    }
    
    private static java.util.List<MarkdownElement> parseMarkdownToElements(String markdown, String documentKey) {
        java.util.List<MarkdownElement> elements = new java.util.ArrayList<>();
        String[] lines = markdown.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) {
                elements.add(new MarkdownElement(ElementType.PARAGRAPH_BREAK, "", null));
                continue;
            }
            
            // Parse different element types
            if (line.startsWith("#")) {
                int headerLevel = 0;
                for (char c : line.toCharArray()) {
                    if (c == '#') headerLevel++;
                    else break;
                }
                String headerText = line.replaceFirst("^#+\\s*", "");
                elements.add(new MarkdownElement(ElementType.HEADER, headerText, headerLevel));
                
            } else if (line.matches("^!\\[.*\\]\\(.*\\)$")) {
                java.util.regex.Pattern imagePattern = java.util.regex.Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
                java.util.regex.Matcher imageMatcher = imagePattern.matcher(line);
                if (imageMatcher.find()) {
                    String imagePath = imageMatcher.group(2);
                    elements.add(new MarkdownElement(ElementType.IMAGE, imagePath, documentKey));
                }
                
            } else if (line.matches("^\\d+\\.\\s.*")) {
                String listText = line.replaceFirst("^\\d+\\.\\s*", "");
                // Extract number for numbered list
                java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("^(\\d+)\\.");
                java.util.regex.Matcher numberMatcher = numberPattern.matcher(line);
                Integer listNumber = numberMatcher.find() ? Integer.parseInt(numberMatcher.group(1)) : 1;
                elements.add(new MarkdownElement(ElementType.NUMBERED_LIST_ITEM, listText, listNumber));
                
            } else if (line.matches("^[-*]\\s.*")) {
                String listText = line.replaceFirst("^[-*]\\s*", "");
                elements.add(new MarkdownElement(ElementType.BULLET_LIST_ITEM, listText, null));
                
            } else {
                // Regular paragraph - parse inline formatting
                java.util.List<TextSpan> spans = parseInlineFormatting(line);
                elements.add(new MarkdownElement(ElementType.PARAGRAPH, spans, null));
            }
        }
        
        return elements;
    }
    
    private static java.util.List<TextSpan> parseInlineFormatting(String text) {
        java.util.List<TextSpan> spans = new java.util.ArrayList<>();
        String remaining = text;
        
        while (!remaining.isEmpty()) {
            int nextFormatPos = remaining.length();
            String formatType = "";
            
            // Find earliest formatting
            int boldPos = remaining.indexOf("**");
            if (boldPos >= 0 && boldPos < nextFormatPos) {
                nextFormatPos = boldPos;
                formatType = "bold";
            }
            
            int italicPos = remaining.indexOf("*");
            if (italicPos >= 0 && italicPos < nextFormatPos) {
                // Make sure it's not part of **
                boolean partOfBold = false;
                if (italicPos > 0 && remaining.charAt(italicPos - 1) == '*') partOfBold = true;
                if (italicPos < remaining.length() - 1 && remaining.charAt(italicPos + 1) == '*') partOfBold = true;
                
                if (!partOfBold) {
                    nextFormatPos = italicPos;
                    formatType = "italic";
                }
            }
            
            int codePos = remaining.indexOf("`");
            if (codePos >= 0 && codePos < nextFormatPos) {
                nextFormatPos = codePos;
                formatType = "code";
            }
            
            // Add plain text before formatting
            if (nextFormatPos > 0) {
                spans.add(new TextSpan(remaining.substring(0, nextFormatPos), TextStyle.REGULAR));
            }
            
            if (nextFormatPos == remaining.length()) {
                break; // No more formatting
            }
            
            // Process formatting
            remaining = remaining.substring(nextFormatPos);
            
            if (formatType.equals("bold")) {
                int endBold = remaining.indexOf("**", 2);
                if (endBold > 0) {
                    String boldText = remaining.substring(2, endBold);
                    spans.add(new TextSpan(boldText, TextStyle.BOLD));
                    remaining = remaining.substring(endBold + 2);
                } else {
                    spans.add(new TextSpan("**", TextStyle.REGULAR));
                    remaining = remaining.substring(2);
                }
            } else if (formatType.equals("italic")) {
                int endItalic = remaining.indexOf("*", 1);
                if (endItalic > 0) {
                    String italicText = remaining.substring(1, endItalic);
                    spans.add(new TextSpan(italicText, TextStyle.ITALIC));
                    remaining = remaining.substring(endItalic + 1);
                } else {
                    spans.add(new TextSpan("*", TextStyle.REGULAR));
                    remaining = remaining.substring(1);
                }
            } else if (formatType.equals("code")) {
                int endCode = remaining.indexOf("`", 1);
                if (endCode > 0) {
                    String codeText = remaining.substring(1, endCode);
                    spans.add(new TextSpan(codeText, TextStyle.CODE));
                    remaining = remaining.substring(endCode + 1);
                } else {
                    spans.add(new TextSpan("`", TextStyle.REGULAR));
                    remaining = remaining.substring(1);
                }
            }
        }
        
        return spans;
    }
    
    private static int calculateTotalHeight(java.util.List<DocumentLine> lines, int lineHeight, int contentWidth, String documentKey) {
        int totalHeight = 0;
        
        for (DocumentLine line : lines) {
            if (line.element != null && line.element.type == ElementType.IMAGE) {
                // Calculate image height (with scaling if needed)
                String imagePath = line.element.textContent;
                BufferedImage img = loadImage(imagePath, documentKey);
                if (img != null) {
                    int imageHeight = img.getHeight();
                    if (img.getWidth() > contentWidth) {
                        double scale = (double) contentWidth / img.getWidth();
                        imageHeight = (int) (imageHeight * scale);
                    }
                    totalHeight += imageHeight + 15; // Add spacing
                } else {
                    totalHeight += lineHeight; // Fallback for missing images
                }
            } else if (line.spans.size() > 0 && line.spans.get(0).style == TextStyle.HEADER) {
                // Headers need more space
                totalHeight += lineHeight + 15; // Extra spacing for headers
            } else {
                totalHeight += lineHeight;
            }
        }
        
        // Add extra padding to prevent cutting off
        return totalHeight + 50;
    }
    
    private static java.util.List<DocumentLine> compileDocumentLayout(java.util.List<MarkdownElement> elements, int maxWidth) {
        java.util.List<DocumentLine> lines = new java.util.ArrayList<>();
        
        // Create graphics for measuring text
        BufferedImage tempImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImage.createGraphics();
        
        for (MarkdownElement element : elements) {
            if (element.type == ElementType.PARAGRAPH_BREAK) {
                lines.add(new DocumentLine()); // Empty line
                
            } else if (element.type == ElementType.HEADER) {
                lines.add(new DocumentLine()); // Space before header
                DocumentLine headerLine = new DocumentLine(element);
                // Convert header text to a span with appropriate styling
                headerLine.spans.add(new TextSpan(element.textContent, TextStyle.HEADER));
                lines.add(headerLine);
                
            } else if (element.type == ElementType.IMAGE) {
                lines.add(new DocumentLine(element));
                
            } else if (element.type == ElementType.BULLET_LIST_ITEM || element.type == ElementType.NUMBERED_LIST_ITEM) {
                // Convert list item text to spans and fit to lines
                java.util.List<TextSpan> spans = parseInlineFormatting(element.textContent);
                fitSpansToLines(spans, maxWidth - 40, g2d, lines, element); // Pass element for marker info
                
            } else if (element.type == ElementType.PARAGRAPH) {
                @SuppressWarnings("unchecked")
                java.util.List<TextSpan> spans = (java.util.List<TextSpan>) element.content;
                fitSpansToLines(spans, maxWidth, g2d, lines, null);
            }
        }
        
        g2d.dispose();
        return lines;
    }
    
    private static void fitSpansToLines(java.util.List<TextSpan> spans, int maxWidth, Graphics2D g2d, 
                                      java.util.List<DocumentLine> lines, MarkdownElement listElement) {
        DocumentLine currentLine = new DocumentLine();
        boolean isListItem = (listElement != null && (listElement.type == ElementType.BULLET_LIST_ITEM || listElement.type == ElementType.NUMBERED_LIST_ITEM));
        
        if (isListItem) {
            currentLine.isListItem = true;
            currentLine.listElement = listElement; // Store list element for rendering
        }
        
        for (TextSpan span : spans) {
            String[] words = span.text.split("\\s+");
            
            for (String word : words) {
                TextSpan wordSpan = new TextSpan(word, span.style);
                
                // Test if adding this word would exceed line width
                int lineWidth = calculateLineWidth(currentLine, g2d);
                int wordWidth = calculateSpanWidth(wordSpan, g2d);
                
                if (lineWidth + wordWidth <= maxWidth || currentLine.spans.isEmpty()) {
                    // Word fits on current line
                    if (!currentLine.spans.isEmpty()) {
                        currentLine.spans.add(new TextSpan(" ", TextStyle.REGULAR)); // Add space
                    }
                    currentLine.spans.add(wordSpan);
                } else {
                    // Start new line
                    lines.add(currentLine);
                    currentLine = new DocumentLine();
                    // Only the first line of a list item gets the bullet
                    currentLine.isListItem = false;
                    if (isListItem) {
                        currentLine.isListContinuation = true; // Mark as continuation
                        currentLine.listElement = listElement; // Preserve list element for continuation
                    }
                    currentLine.spans.add(wordSpan);
                }
            }
        }
        
        // Add the last line if it has content
        if (!currentLine.spans.isEmpty()) {
            lines.add(currentLine);
        }
    }
    
    private static int calculateLineWidth(DocumentLine line, Graphics2D g2d) {
        int width = 0;
        for (TextSpan span : line.spans) {
            width += calculateSpanWidth(span, g2d);
        }
        return width;
    }
    
    private static int calculateSpanWidth(TextSpan span, Graphics2D g2d) {
        Font font = getFontForStyle(span.style);
        g2d.setFont(font);
        return g2d.getFontMetrics().stringWidth(span.text);
    }
    
    private static Font getFontForStyle(TextStyle style) {
        switch (style) {
            case BOLD: return new Font("Arial", Font.BOLD, UI.scale(12));
            case ITALIC: return new Font("Arial", Font.ITALIC, UI.scale(12));
            case CODE: return new Font("Fira code", Font.PLAIN, UI.scale(11));
            case HEADER: return new Font("Arial", Font.BOLD, UI.scale(18));
            default: return new Font("Arial", Font.PLAIN, UI.scale(12));
        }
    }
    
    private static BufferedImage renderDocumentLines(java.util.List<DocumentLine> lines, int width, int height, int margin, int lineHeight) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background
        g2d.setColor(new java.awt.Color(30, 30, 30, 180));
        g2d.fillRect(0, 0, width, height);
        
        int y = margin + lineHeight;
        
        for (DocumentLine line : lines) {
            if (line.element != null && line.element.type == ElementType.IMAGE) {
                // Render image
                String imagePath = line.element.textContent;
                BufferedImage img = loadImage(imagePath, (String) line.element.content);
                if (img != null) {
                    // Scale image if too wide
                    int imageWidth = img.getWidth();
                    int imageHeight = img.getHeight();
                    int maxImageWidth = width - margin * 2;
                    
                    if (imageWidth > maxImageWidth) {
                        double scale = (double) maxImageWidth / imageWidth;
                        imageWidth = maxImageWidth;
                        imageHeight = (int) (imageHeight * scale);
                        
                        // Create scaled image
                        BufferedImage scaledImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D scaleG2d = scaledImage.createGraphics();
                        scaleG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        scaleG2d.drawImage(img, 0, 0, imageWidth, imageHeight, null);
                        scaleG2d.dispose();
                        img = scaledImage;
                    }
                    
                    g2d.drawImage(img, margin, y - lineHeight, null);
                    y += img.getHeight() + 15; // Add spacing after image
                }
            } else {
                // Render text spans
                int x = margin;
                
                // Check if this line should be indented (either has bullet or is continuation of list)
                boolean needsIndent = line.isListItem;
                if (!needsIndent && !line.spans.isEmpty()) {
                    // Check if previous line was a list item (this might be a continuation)
                    // For now, we'll handle this in the layout phase
                }
                
                if (line.isListItem && line.listElement != null) {
                    g2d.setColor(java.awt.Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.PLAIN, UI.scale(12)));
                    
                    if (line.listElement.type == ElementType.BULLET_LIST_ITEM) {
                        g2d.drawString("•", x, y);
                    } else if (line.listElement.type == ElementType.NUMBERED_LIST_ITEM) {
                        Integer listNumber = (Integer) line.listElement.content;
                        g2d.drawString(listNumber + ".", x, y);
                    }
                    x += 20; // Indent for list content
                } else if (line.isListContinuation) {
                    // Indent continuation lines but no bullet
                    x += 20;
                }
                
                for (TextSpan span : line.spans) {
                    Font font = getFontForStyle(span.style);
                    g2d.setFont(font);
                    g2d.setColor(getColorForStyle(span.style));
                    g2d.drawString(span.text, x, y);
                    x += g2d.getFontMetrics().stringWidth(span.text);
                }
            }
            y += lineHeight;
        }
        
        g2d.dispose();
        return image;
    }
    
    private static java.awt.Color getColorForStyle(TextStyle style) {
        switch (style) {
            case BOLD: return java.awt.Color.WHITE;
            case ITALIC: return new java.awt.Color(200, 200, 200);
            case CODE: return new java.awt.Color(150, 255, 150);
            case HEADER: return new java.awt.Color(255, 255, 128);
            default: return java.awt.Color.WHITE;
        }
    }
    
    // Data classes for the new approach
    private static class MarkdownElement {
        ElementType type;
        String textContent;
        Object content; // Can be Integer for header level, String for documentKey, List<TextSpan> for paragraph
        
        MarkdownElement(ElementType type, String textContent, Object content) {
            this.type = type;
            this.textContent = textContent;
            this.content = content;
        }
        
        MarkdownElement(ElementType type, Object content, Object extra) {
            this.type = type;
            this.content = content;
            // For paragraphs where content is List<TextSpan>
        }
    }
    
    private static class DocumentLine {
        java.util.List<TextSpan> spans = new java.util.ArrayList<>();
        MarkdownElement element; // For non-text elements like images
        boolean isListItem = false;
        boolean isListContinuation = false; // For wrapped list content
        MarkdownElement listElement; // For storing list element info (bullet vs numbered)
        
        DocumentLine() {}
        
        DocumentLine(MarkdownElement element) {
            this.element = element;
        }
    }
    
    private static class TextSpan {
        String text;
        TextStyle style;
        
        TextSpan(String text, TextStyle style) {
            this.text = text;
            this.style = style;
        }
    }
    
    private enum ElementType {
        PARAGRAPH, HEADER, IMAGE, BULLET_LIST_ITEM, NUMBERED_LIST_ITEM, PARAGRAPH_BREAK
    }
    
    private enum TextStyle {
        REGULAR, BOLD, ITALIC, CODE, HEADER
    }

    private static BufferedImage loadImage(String imagePath, String documentKey) {
        try {
            String resourcePath;

            // Construct resource path based on document location
            if (documentKey != null && documentKey.contains("/")) {
                // Extract directory from document key
                String docDir = documentKey.substring(0, documentKey.lastIndexOf("/"));
                resourcePath = resolveRelativePath("/nurgling/docs/" + docDir, imagePath);
            } else {
                // Default to images directory
                resourcePath = "/nurgling/docs/images/" + imagePath;
            }

            // Try to load as resource
            InputStream imageStream = MarkdownToImageRenderer.class.getResourceAsStream(resourcePath);
            if (imageStream != null) {
                try (InputStream stream = imageStream) {
                    return ImageIO.read(stream);
                }
            } else {
                // Fallback 1: try with uppercase extension for current path
                String upperCasePathAttempt1 = tryUppercaseExtension(resourcePath);
                if (upperCasePathAttempt1 != null) {
                    imageStream = MarkdownToImageRenderer.class.getResourceAsStream(upperCasePathAttempt1);
                    if (imageStream != null) {
                        try (InputStream stream = imageStream) {
                            return ImageIO.read(stream);
                        }
                    }
                }

                // Fallback 2: try loading from images directory
                resourcePath = "/nurgling/docs/images/" + imagePath;
                imageStream = MarkdownToImageRenderer.class.getResourceAsStream(resourcePath);
                if (imageStream != null) {
                    try (InputStream stream = imageStream) {
                        return ImageIO.read(stream);
                    }
                } else {
                    // Fallback 3: try loading from root docs directory
                    resourcePath = "/nurgling/docs/" + imagePath;
                    imageStream = MarkdownToImageRenderer.class.getResourceAsStream(resourcePath);
                    if (imageStream != null) {
                        try (InputStream stream = imageStream) {
                            return ImageIO.read(stream);
                        }
                    } else {
                        // Fallback 4: try with uppercase extension (Windows case issue)
                        String upperCasePath = tryUppercaseExtension(resourcePath);
                        if (upperCasePath != null) {
                            imageStream = MarkdownToImageRenderer.class.getResourceAsStream(upperCasePath);
                            if (imageStream != null) {
                                try (InputStream stream = imageStream) {
                                    return ImageIO.read(stream);
                                }
                            }
                        }
                        System.err.println("Image not found in resources: " + imagePath + " (tried multiple paths including /nurgling/docs/images/)");
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load image: " + imagePath + " - " + e.getMessage());
            return null;
        }
    }
    
    private static String tryUppercaseExtension(String path) {
        // Handle Windows case sensitivity issues by trying uppercase extensions
        if (path.endsWith(".png")) {
            return path.substring(0, path.length() - 4) + ".PNG";
        } else if (path.endsWith(".jpg")) {
            return path.substring(0, path.length() - 4) + ".JPG";
        } else if (path.endsWith(".jpeg")) {
            return path.substring(0, path.length() - 5) + ".JPEG";
        } else if (path.endsWith(".gif")) {
            return path.substring(0, path.length() - 4) + ".GIF";
        }
        return null;
    }

    private static String resolveRelativePath(String basePath, String relativePath) {
        // Handle relative paths like ../images/routes_main_ui.png
        if (relativePath.startsWith("./")) {
            relativePath = relativePath.substring(2);
        }
        
        String[] baseParts = basePath.split("/");
        String[] relativeParts = relativePath.split("/");
        
        java.util.List<String> resultParts = new java.util.ArrayList<>();
        
        // Add base path parts
        for (String part : baseParts) {
            if (!part.isEmpty()) {
                resultParts.add(part);
            }
        }
        
        // Process relative path parts
        for (String part : relativeParts) {
            if (part.equals("..")) {
                // Go up one directory
                if (!resultParts.isEmpty()) {
                    resultParts.remove(resultParts.size() - 1);
                }
            } else if (!part.equals(".") && !part.isEmpty()) {
                // Add this part
                resultParts.add(part);
            }
        }
        
        // Rebuild path
        StringBuilder result = new StringBuilder();
        for (String part : resultParts) {
            result.append("/").append(part);
        }
        
        return result.toString();
    }
}