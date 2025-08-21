package nurgling.tools;

import haven.UI;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class MarkdownToImageRenderer {
    
    public static BufferedImage renderMarkdownToImage(String markdown, int maxWidth) {
        // Define fonts and colors
        Font regularFont = new Font("Arial", Font.PLAIN, UI.scale(12));
        Font boldFont = new Font("Arial", Font.BOLD, UI.scale(12));
        Font italicFont = new Font("Arial", Font.ITALIC, UI.scale(12));
        Font codeFont = new Font("Courier New", Font.PLAIN, UI.scale(11));
        
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
        
        int estimatedHeight = lines.length * 25 + 100; // Rough estimate
        
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
        int headerHeight = 30;
        
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
                    renderFormattedLine(g2d, listLine, margin + 20, y, maxWidth - 40, regularFont, boldFont, italicFont, codeFont, textColor, boldColor, italicColor, linkColor, codeColor);
                    y += lineHeight;
                    i++;
                }
                
                y += 8; // Space after list
                continue;
            }
            
            // Regular paragraph
            if (!line.isEmpty()) {
                y += 8;
                renderFormattedLine(g2d, line, margin, y, maxWidth - 30, regularFont, boldFont, italicFont, codeFont, textColor, boldColor, italicColor, linkColor, codeColor);
                y += lineHeight + 5;
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
        if (x + textWidth > maxWidth) {
            return x;
        }
        
        g2d.drawString(text, x, y);
        return x + textWidth;
    }
}