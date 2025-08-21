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
    
    // New alternative rendering approach
    public static BufferedImage renderMarkdownToImageV2(String markdown, int maxWidth, String documentKey) {
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
                
            } else if (line.matches("^\\d+\\.\\s.*") || line.matches("^[-*]\\s.*")) {
                String listText = line.replaceFirst("^(\\d+\\.|[-*])\\s*", "");
                elements.add(new MarkdownElement(ElementType.LIST_ITEM, listText, null));
                
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
                
            } else if (element.type == ElementType.LIST_ITEM) {
                // Convert list item text to spans and fit to lines
                java.util.List<TextSpan> spans = parseInlineFormatting(element.textContent);
                fitSpansToLines(spans, maxWidth - 40, g2d, lines, true); // Indent list items
                
            } else if (element.type == ElementType.PARAGRAPH) {
                @SuppressWarnings("unchecked")
                java.util.List<TextSpan> spans = (java.util.List<TextSpan>) element.content;
                fitSpansToLines(spans, maxWidth, g2d, lines, false);
            }
        }
        
        g2d.dispose();
        return lines;
    }
    
    private static void fitSpansToLines(java.util.List<TextSpan> spans, int maxWidth, Graphics2D g2d, 
                                      java.util.List<DocumentLine> lines, boolean isListItem) {
        DocumentLine currentLine = new DocumentLine();
        boolean isFirstLineOfList = isListItem;
        
        if (isFirstLineOfList) {
            currentLine.isListItem = true;
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
                
                if (line.isListItem) {
                    g2d.setColor(java.awt.Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.PLAIN, UI.scale(12)));
                    g2d.drawString("•", x, y);
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
        PARAGRAPH, HEADER, IMAGE, LIST_ITEM, PARAGRAPH_BREAK
    }
    
    private enum TextStyle {
        REGULAR, BOLD, ITALIC, CODE, HEADER
    }

    public static BufferedImage renderMarkdownToImage(String markdown, int maxWidth, String documentKey) {
        // Define fonts and colors
        Font regularFont = new Font("Arial", Font.PLAIN, UI.scale(12));
        Font boldFont = new Font("Arial", Font.BOLD, UI.scale(12));
        Font italicFont = new Font("Arial", Font.ITALIC, UI.scale(12));
        Font codeFont = new Font("Fira code", Font.PLAIN, UI.scale(11));
        
        // Header fonts for different levels
        Font h1Font = new Font("Arial", Font.BOLD, UI.scale(20)); // # Main Header
        Font h2Font = new Font("Arial", Font.BOLD, UI.scale(18)); // ## Sub Header  
        Font h3Font = new Font("Arial", Font.BOLD, UI.scale(16)); // ### Smaller Header
        Font h4Font = new Font("Arial", Font.BOLD, UI.scale(14)); // #### 
        Font h5Font = new Font("Arial", Font.BOLD, UI.scale(13)); // #####
        Font h6Font = new Font("Arial", Font.BOLD, UI.scale(12)); // ######
        
        Color bgColor = new Color(30, 30, 30, 180);
        Color textColor = Color.WHITE;
        Color headerColor = new Color(255, 255, 128);
        Color boldColor = Color.WHITE;
        Color italicColor = new Color(200, 200, 200);
        Color linkColor = new Color(100, 200, 255);
        Color codeColor = new Color(150, 255, 150);
        
        // Split into lines and estimate height
        String[] lines = markdown.split("\n");
        
        // Start with base estimate for text lines
        int estimatedHeight = lines.length * 30 + 100; // Rough estimate
        
        // Add estimated height for images
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.matches("^!\\[.*\\]\\(.*\\)$")) {
                java.util.regex.Pattern imagePattern = java.util.regex.Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
                java.util.regex.Matcher imageMatcher = imagePattern.matcher(trimmedLine);
                if (imageMatcher.find()) {
                    String imagePath = imageMatcher.group(2);
                    BufferedImage imageToCheck = loadImage(imagePath, documentKey);
                    if (imageToCheck != null) {
                        int imageHeight = imageToCheck.getHeight();
                        // Scale height if image would be scaled down
                        int maxImageWidth = maxWidth - 30; // Account for margins
                        if (imageToCheck.getWidth() > maxImageWidth) {
                            double scale = (double) maxImageWidth / imageToCheck.getWidth();
                            imageHeight = (int) (imageHeight * scale);
                        }
                        estimatedHeight += imageHeight + 25; // Add image height plus spacing
                    } else {
                        estimatedHeight += 25; // Fallback for missing image
                    }
                }
            }
        }
        // Create image
        BufferedImage image = new BufferedImage(maxWidth, estimatedHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, maxWidth, estimatedHeight);
        
        // Group lines and render by sections
        int y = 25;
        int margin = 15;
        int lineHeight = 20;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) {
                y += 10; // Empty line spacing
                continue;
            }
            
            // Check for headers
            if (line.startsWith("#")) {
                y += 15; // Space before header
                
                // Determine header level and get appropriate font
                int headerLevel = 0;
                for (char c : line.toCharArray()) {
                    if (c == '#') headerLevel++;
                    else break;
                }
                
                String headerText = line.replaceFirst("^#+\\s*", "");
                Font headerFont;
                int headerSpacing;
                
                switch (headerLevel) {
                    case 1: headerFont = h1Font; headerSpacing = 20; break;
                    case 2: headerFont = h2Font; headerSpacing = 18; break;
                    case 3: headerFont = h3Font; headerSpacing = 16; break;
                    case 4: headerFont = h4Font; headerSpacing = 14; break;
                    case 5: headerFont = h5Font; headerSpacing = 12; break;
                    case 6: 
                    default: headerFont = h6Font; headerSpacing = 20; break;
                }
                
                g2d.setFont(headerFont);
                g2d.setColor(headerColor);
                g2d.drawString(headerText, margin, y);
                y += headerSpacing;
                continue;
            }
            
            // Check for images: ![alt text](image.png)
            if (line.matches("^!\\[.*\\]\\(.*\\)$")) {
                y += 10; // Space before image
                
                java.util.regex.Pattern imagePattern = java.util.regex.Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
                java.util.regex.Matcher imageMatcher = imagePattern.matcher(line);
                if (imageMatcher.find()) {
                    String altText = imageMatcher.group(1);
                    String imagePath = imageMatcher.group(2);
                    
                    BufferedImage imageToRender = loadImage(imagePath, documentKey);
                    if (imageToRender != null) {
                        // Scale image if too wide
                        int imageWidth = imageToRender.getWidth();
                        int imageHeight = imageToRender.getHeight();
                        int maxImageWidth = maxWidth - margin * 2;
                        
                        if (imageWidth > maxImageWidth) {
                            double scale = (double) maxImageWidth / imageWidth;
                            imageWidth = maxImageWidth;
                            imageHeight = (int) (imageHeight * scale);
                            
                            // Create scaled image
                            BufferedImage scaledImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D scaleG2d = scaledImage.createGraphics();
                            scaleG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            scaleG2d.drawImage(imageToRender, 0, 0, imageWidth, imageHeight, null);
                            scaleG2d.dispose();
                            imageToRender = scaledImage;
                        }
                        
                        // Draw the image
                        g2d.drawImage(imageToRender, margin, y, null);
                        y += imageHeight + 15; // Space after image
                    } else {
                        // Fallback: draw alt text if image couldn't be loaded
                        g2d.setFont(regularFont);
                        g2d.setColor(textColor);
                        g2d.drawString("[Image: " + altText + "]", margin, y);
                        y += lineHeight + 5;
                    }
                }
                continue;
            }
            
            // Check for fenced code blocks: ```
            if (line.startsWith("```")) {
                y += 10; // Space before code block
                
                // Extract language if specified (optional)
                String language = line.length() > 3 ? line.substring(3).trim() : "";
                
                // Collect all lines until closing ```
                StringBuilder codeContent = new StringBuilder();
                i++; // Move to next line after opening ```
                
                while (i < lines.length) {
                    String codeLine = lines[i];
                    if (codeLine.trim().equals("```")) {
                        break; // Found closing ```
                    }
                    if (codeContent.length() > 0) {
                        codeContent.append("\n");
                    }
                    codeContent.append(codeLine);
                    i++;
                }
                
                // Render the code block
                y = renderCodeBlock(g2d, codeContent.toString(), margin, y, maxWidth - margin * 2, codeFont, codeColor, bgColor);
                y += 15; // Space after code block
                continue;
            }
            
            // Check for list items - group consecutive items
            boolean isNumberedList = line.matches("^\\d+\\.\\s.*");
            boolean isBulletList = line.matches("^[-*]\\s.*");
            
            if (isNumberedList || isBulletList) {
                y += 8; // Space before list
                
                // Process all consecutive list items
                while (i < lines.length) {
                    String listLine = lines[i].trim();
                    boolean isListItem = listLine.matches("^\\d+\\.\\s.*") || listLine.matches("^[-*]\\s.*");
                    
                    if (!isListItem) {
                        i--; // Back up one since we'll increment at end of outer loop
                        break;
                    }
                    
                    y += 3; // Tighter spacing between list items
                    y = renderFormattedLineWithWrapping(g2d, listLine, margin + 20, y, maxWidth - 40, lineHeight, regularFont, boldFont, italicFont, codeFont, textColor, boldColor, italicColor, linkColor, codeColor);
                    i++;
                }
                
                // Ensure we don't go past the array bounds
                if (i >= lines.length) {
                    break;
                }
                
                y += 8; // Space after list
                continue;
            }
            
            // Regular paragraph
            if (!line.isEmpty()) {
                y += 8;
                y = renderFormattedLineWithWrapping(g2d, line, margin, y, maxWidth - 30, lineHeight, regularFont, boldFont, italicFont, codeFont, textColor, boldColor, italicColor, linkColor, codeColor);
                y += 5;
            }
        }
        
        g2d.dispose();
        
        // Crop to actual content height
        if (y < estimatedHeight - 50) {
            BufferedImage croppedImage = new BufferedImage(maxWidth, y + 20, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cropG2d = croppedImage.createGraphics();
            cropG2d.drawImage(image, 0, 0, null);
            cropG2d.dispose();
            return croppedImage;
        }
        
        return image;
    }
    
    private static int renderFormattedLineWithWrapping(Graphics2D g2d, String line, int x, int y, int maxWidth, int lineHeight,
                                              Font regularFont, Font boldFont, Font italicFont, Font codeFont,
                                              Color textColor, Color boldColor, Color italicColor, Color linkColor, Color codeColor) {
        // Split long lines into words for wrapping
        String[] words = line.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        int currentY = y;
        
        g2d.setFont(regularFont);
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            
            // Check if this line would be too wide (simplified check - doesn't account for formatting)
            int testWidth = g2d.getFontMetrics().stringWidth(testLine);
            
            if (testWidth <= maxWidth || currentLine.length() == 0) {
                // Word fits on current line
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                // Current line is full, render it and start a new line
                if (currentLine.length() > 0) {
                    renderFormattedLine(g2d, currentLine.toString(), x, currentY, maxWidth, regularFont, boldFont, italicFont, codeFont, textColor, boldColor, italicColor, linkColor, codeColor);
                    currentY += lineHeight;
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        // Render the last line
        if (currentLine.length() > 0) {
            renderFormattedLine(g2d, currentLine.toString(), x, currentY, maxWidth, regularFont, boldFont, italicFont, codeFont, textColor, boldColor, italicColor, linkColor, codeColor);
            currentY += lineHeight;
        }
        
        return currentY;
    }
    
    private static void renderFormattedLine(Graphics2D g2d, String line, int x, int y, int maxWidth, 
                                   Font regularFont, Font boldFont, Font italicFont, Font codeFont,
                                   Color textColor, Color boldColor, Color italicColor, Color linkColor, Color codeColor) {
        int currentX = x;
        String remaining = line;
        
        // Process formatting in order: links, code, bold, italic
        while (!remaining.isEmpty()) {
            int nextFormatPos = remaining.length();
            String formatType = "";
            
            // Find the earliest formatting
            int linkPos = remaining.indexOf("[");
            if (linkPos >= 0 && linkPos < nextFormatPos) {
                nextFormatPos = linkPos;
                formatType = "link";
            }
            
            int codePos = remaining.indexOf("`");
            if (codePos >= 0 && codePos < nextFormatPos) {
                nextFormatPos = codePos;
                formatType = "code";
            }
            
            // Check italic BEFORE bold to handle *text* correctly
            int italicPos = remaining.indexOf("*");
            if (italicPos >= 0 && italicPos < nextFormatPos) {
                // Check if this * is part of **
                boolean partOfBold = false;
                if (italicPos > 0 && remaining.charAt(italicPos - 1) == '*') {
                    partOfBold = true; // Previous char is *, so this is end of **
                }
                if (italicPos < remaining.length() - 1 && remaining.charAt(italicPos + 1) == '*') {
                    partOfBold = true; // Next char is *, so this is start of **
                }
                
                if (!partOfBold) {
                    nextFormatPos = italicPos;
                    formatType = "italic";
                }
            }
            
            int boldPos = remaining.indexOf("**");
            if (boldPos >= 0 && boldPos < nextFormatPos) {
                nextFormatPos = boldPos;
                formatType = "bold";
            }
            
            // Render text before formatting (preserve exact spacing)
            if (nextFormatPos > 0) {
                String beforeText = remaining.substring(0, nextFormatPos);
                currentX = renderText(g2d, beforeText, currentX, y, maxWidth, regularFont, textColor);
            }
            
            if (nextFormatPos == remaining.length()) {
                break; // No more formatting
            }
            
            // Process the formatting
            remaining = remaining.substring(nextFormatPos);
            
            if (formatType.equals("link")) {
                java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
                java.util.regex.Matcher linkMatcher = linkPattern.matcher(remaining);
                if (linkMatcher.find()) {
                    String linkText = linkMatcher.group(1);
                    currentX = renderText(g2d, linkText, currentX, y, maxWidth, regularFont, linkColor);
                    remaining = remaining.substring(linkMatcher.end());
                } else {
                    currentX = renderText(g2d, "[", currentX, y, maxWidth, regularFont, textColor);
                    remaining = remaining.substring(1);
                }
            } else if (formatType.equals("code")) {
                int endCode = remaining.indexOf("`", 1);
                if (endCode > 0) {
                    String codeText = remaining.substring(1, endCode);
                    currentX = renderText(g2d, codeText, currentX, y, maxWidth, codeFont, codeColor);
                    remaining = remaining.substring(endCode + 1);
                } else {
                    currentX = renderText(g2d, "`", currentX, y, maxWidth, regularFont, textColor);
                    remaining = remaining.substring(1);
                }
            } else if (formatType.equals("bold")) {
                int endBold = remaining.indexOf("**", 2);
                if (endBold > 0) {
                    String boldText = remaining.substring(2, endBold);
                    currentX = renderText(g2d, boldText, currentX, y, maxWidth, boldFont, boldColor);
                    remaining = remaining.substring(endBold + 2);
                } else {
                    currentX = renderText(g2d, "**", currentX, y, maxWidth, regularFont, textColor);
                    remaining = remaining.substring(2);
                }
            } else if (formatType.equals("italic")) {
                int endItalic = remaining.indexOf("*", 1);
                // Make sure the closing * is not part of **
                while (endItalic > 0 && endItalic < remaining.length() - 1 && remaining.charAt(endItalic + 1) == '*') {
                    endItalic = remaining.indexOf("*", endItalic + 1);
                }
                
                if (endItalic > 0) {
                    String italicText = remaining.substring(1, endItalic);
                    currentX = renderText(g2d, italicText, currentX, y, maxWidth, italicFont, italicColor);
                    remaining = remaining.substring(endItalic + 1);
                } else {
                    currentX = renderText(g2d, "*", currentX, y, maxWidth, regularFont, textColor);
                    remaining = remaining.substring(1);
                }
            }
        }
    }
    
    private static int renderText(Graphics2D g2d, String text, int x, int y, int maxWidth, Font font, Color color) {
        g2d.setFont(font);
        g2d.setColor(color);
        
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        if (x + textWidth <= maxWidth) {
            // Text fits on current line
            g2d.drawString(text, x, y);
            return x + textWidth;
        } else {
            // Text is too long, just render what we can (simplified approach)
            // For now, just render the text at the current position - wrapping would need more complex changes
            g2d.drawString(text, x, y);
            return x + textWidth;
        }
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
                // Fallback 1: try loading from images directory
                resourcePath = "/nurgling/docs/images/" + imagePath;
                imageStream = MarkdownToImageRenderer.class.getResourceAsStream(resourcePath);
                if (imageStream != null) {
                    try (InputStream stream = imageStream) {
                        return ImageIO.read(stream);
                    }
                } else {
                    // Fallback 2: try loading from root docs directory
                    resourcePath = "/nurgling/docs/" + imagePath;
                    imageStream = MarkdownToImageRenderer.class.getResourceAsStream(resourcePath);
                    if (imageStream != null) {
                        try (InputStream stream = imageStream) {
                            return ImageIO.read(stream);
                        }
                    } else {
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
    
    private static String resolveRelativePath(String basePath, String relativePath) {
        // Handle relative paths like ../images/img.png
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
    
    private static int renderCodeBlock(Graphics2D g2d, String codeContent, int x, int y, int blockWidth, Font codeFont, Color codeColor, Color backgroundColor) {
        // Set up colors for code block
        Color codeBlockBg = new Color(20, 20, 20, 200); // Darker background for code blocks
        Color borderColor = new Color(60, 60, 60, 180);
        
        // Split code into lines
        String[] codeLines = codeContent.split("\n");
        
        // Calculate dimensions
        g2d.setFont(codeFont);
        int lineHeight = g2d.getFontMetrics().getHeight();
        int padding = 10;
        int blockHeight = (codeLines.length * lineHeight) + (padding * 2);
        
        // Draw background rectangle
        g2d.setColor(codeBlockBg);
        g2d.fillRect(x, y, blockWidth, blockHeight);
        
        // Draw border
        g2d.setColor(borderColor);
        g2d.drawRect(x, y, blockWidth, blockHeight);
        
        // Draw code lines
        g2d.setColor(codeColor);
        g2d.setFont(codeFont);
        
        int lineY = y + padding + g2d.getFontMetrics().getAscent();
        for (String codeLine : codeLines) {
            g2d.drawString(codeLine, x + padding, lineY);
            lineY += lineHeight;
        }
        
        return y + blockHeight;
    }
}