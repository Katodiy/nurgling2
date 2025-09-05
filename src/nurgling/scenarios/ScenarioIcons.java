package nurgling.scenarios;

import haven.UI;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;

public class ScenarioIcons {
    
    public static BufferedImage loadScenarioIcon(Scenario scenario, String state) {
        // Always generate text-based icons
        return generateTextIcon(scenario, state);
    }
    
    public static BufferedImage loadScenarioIconUp(Scenario scenario) {
        return loadScenarioIcon(scenario, "u");
    }
    
    public static BufferedImage loadScenarioIconDown(Scenario scenario) {
        return loadScenarioIcon(scenario, "d");
    }
    
    public static BufferedImage loadScenarioIconHover(Scenario scenario) {
        return loadScenarioIcon(scenario, "h");
    }
    
    public static BufferedImage getIcon(Scenario scenario) {
        return loadScenarioIconUp(scenario);
    }
    
    public static BufferedImage getDefaultIcon() {
        // Always generate a generic text icon
        return generateTextIcon(null, "u");
    }
    
    private static BufferedImage generateTextIcon(Scenario scenario, String state) {
        String text = scenario != null ? getScenarioInitials(scenario.getName()) : "S";
        
        // Create a 32x32 icon with text (same size as bot icons)
        int size = UI.scale(32);
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background color based on state
        Color bgColor = getStateColor(state);
        Color textColor = Color.WHITE;
        
        // Draw background (scaled for 32x32)
        g2d.setColor(bgColor);
        g2d.fillRoundRect(2, 2, size - 4, size - 4, 6, 6);
        
        // Draw border (scaled for 32x32)
        g2d.setColor(textColor);
        g2d.drawRoundRect(2, 2, size - 4, size - 4, 6, 6);
        
        // Draw text (scaled font size for 32x32)
        Font font = new Font("SansSerif", Font.BOLD, UI.scale(12));
        g2d.setFont(font);
        g2d.setColor(textColor);
        
        // Center the text
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int textHeight = g2d.getFontMetrics().getAscent();
        int x = (size - textWidth) / 2;
        int y = (size + textHeight) / 2 - 2; // Slightly above center for better visual balance
        
        g2d.drawString(text, x, y);
        g2d.dispose();
        
        return icon;
    }
    
    private static String getScenarioInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "S";
        }
        
        String[] words = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        // Get first letter of each word, up to 3 letters max
        for (int i = 0; i < Math.min(3, words.length); i++) {
            if (!words[i].isEmpty()) {
                initials.append(words[i].charAt(0));
            }
        }
        
        return initials.toString().toUpperCase();
    }
    
    private static Color getStateColor(String state) {
        switch (state) {
            case "d": return new Color(100, 100, 150); // Darker blue for pressed
            case "h": return new Color(150, 150, 200); // Lighter blue for hover
            case "u":
            default: return new Color(120, 120, 170); // Normal blue
        }
    }
}