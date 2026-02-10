package nurgling.equipment;

import haven.UI;
import nurgling.widgets.CustomIcon;
import nurgling.widgets.CustomIconManager;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;

public class EquipmentPresetIcons {

    public static BufferedImage loadPresetIcon(EquipmentPreset preset, String state) {
        // Check for custom icon first
        if (preset != null && preset.getCustomIconId() != null) {
            CustomIcon customIcon = CustomIconManager.getInstance().getIcon(preset.getCustomIconId());
            if (customIcon != null) {
                int stateIndex = getStateIndex(state);
                return customIcon.getImage(stateIndex);
            }
        }
        return generateTextIcon(preset, state);
    }

    public static BufferedImage loadPresetIconUp(EquipmentPreset preset) {
        return loadPresetIcon(preset, "u");
    }

    public static BufferedImage loadPresetIconDown(EquipmentPreset preset) {
        return loadPresetIcon(preset, "d");
    }

    public static BufferedImage loadPresetIconHover(EquipmentPreset preset) {
        return loadPresetIcon(preset, "h");
    }

    private static int getStateIndex(String state) {
        switch (state) {
            case "d": return 1;
            case "h": return 2;
            case "u":
            default: return 0;
        }
    }

    private static BufferedImage generateTextIcon(EquipmentPreset preset, String state) {
        String text = preset != null ? getPresetInitials(preset.getName()) : "E";

        int size = UI.scale(32);
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Use green color scheme to differentiate from scenarios (blue)
        Color bgColor = getStateColor(state);
        Color textColor = Color.WHITE;

        g2d.setColor(bgColor);
        g2d.fillRoundRect(2, 2, size - 4, size - 4, 6, 6);

        g2d.setColor(textColor);
        g2d.drawRoundRect(2, 2, size - 4, size - 4, 6, 6);

        Font font = new Font("SansSerif", Font.BOLD, UI.scale(12));
        g2d.setFont(font);
        g2d.setColor(textColor);

        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int textHeight = g2d.getFontMetrics().getAscent();
        int x = (size - textWidth) / 2;
        int y = (size + textHeight) / 2 - 2;

        g2d.drawString(text, x, y);
        g2d.dispose();

        return icon;
    }

    private static String getPresetInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "E";
        }

        String[] words = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < Math.min(3, words.length); i++) {
            if (!words[i].isEmpty()) {
                initials.append(words[i].charAt(0));
            }
        }

        return initials.toString().toUpperCase();
    }

    private static Color getStateColor(String state) {
        switch (state) {
            case "d": return new Color(80, 130, 80);   // Darker green for pressed
            case "h": return new Color(120, 180, 120); // Lighter green for hover
            case "u":
            default: return new Color(100, 150, 100);  // Normal green
        }
    }
}
