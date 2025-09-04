package nurgling.scenarios;

import haven.Resource;
import haven.GOut;
import haven.Coord;
import haven.Text;
import haven.Utils;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;

public class ScenarioIcons {
    private static final String SCENARIO_ICON_DIR = "nurgling/scenarios/icons/";
    private static final String[] FALLBACK_ICONS = {
        "nurgling/hud/buttons/play/u", // Play button icon
        "paginae/act/bash", // Action icon
        "baubles/custom", // Custom icon
        "ui/slp" // Simple icon
    };
    
    public static BufferedImage loadScenarioIcon(Scenario scenario, String state) {
        String iconPath = getScenarioIconPath(scenario);
        
        // Try custom scenario icon first
        try {
            return Resource.loadsimg(SCENARIO_ICON_DIR + iconPath + "/" + state);
        } catch (Exception e) {
            // Fallback to default scenario icon
            try {
                return Resource.loadsimg(SCENARIO_ICON_DIR + "default/" + state);
            } catch (Exception fallbackException) {
                // Generate a text-based icon as final fallback
                return generateTextIcon(scenario, state);
            }
        }
    }
    
    private static String getScenarioIconPath(Scenario scenario) {
        if (scenario == null || scenario.getName() == null) {
            return "default";
        }
        
        // Convert scenario name to valid icon path
        String iconName = scenario.getName().toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("\\s+", "");
        
        if (iconName.isEmpty()) {
            return "default";
        }
        
        return iconName;
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
        try {
            return Resource.loadsimg(SCENARIO_ICON_DIR + "default/u");
        } catch (Exception e) {
            // Try fallback icons from game assets
            for (String fallbackIcon : FALLBACK_ICONS) {
                try {
                    return Resource.loadsimg(fallbackIcon);
                } catch (Exception ignored) {}
            }
            // Generate a generic text icon
            return generateTextIcon(null, "u");
        }
    }
    
    private static BufferedImage generateTextIcon(Scenario scenario, String state) {
        String text = scenario != null ? getScenarioInitials(scenario.getName()) : "S";
        
        // Create a 16x16 icon with text
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background color based on state
        Color bgColor = getStateColor(state);
        Color textColor = Color.WHITE;
        
        // Draw background
        g2d.setColor(bgColor);
        g2d.fillRoundRect(1, 1, 14, 14, 3, 3);
        
        // Draw border
        g2d.setColor(textColor);
        g2d.drawRoundRect(1, 1, 14, 14, 3, 3);
        
        // Draw text
        Font font = new Font("SansSerif", Font.BOLD, 8);
        g2d.setFont(font);
        g2d.setColor(textColor);
        
        // Center the text
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (16 - textWidth) / 2;
        int y = 11; // Slightly below center for better visual balance
        
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