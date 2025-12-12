package nurgling.widgets.nsettings;

import haven.*;
import haven.res.ui.tt.q.quality.Quality;
import nurgling.NConfig;
import nurgling.NStyle;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;
import nurgling.conf.ItemQualityOverlaySettings.Corner;
import nurgling.conf.ItemQualityOverlaySettings.QualityThreshold;
import nurgling.widgets.NColorWidget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings panel for customizing item quality overlay display.
 * Features controls for corner position, font, size, colors, thresholds and a large preview.
 */
public class ItemOverlaySettings extends Panel {
    
    private static final List<String> FONT_FAMILIES = Arrays.asList(
            "Inter", "Roboto", "Sans", "Serif", "Fractur"
    );
    
    // Current settings being edited
    private ItemQualityOverlaySettings currentSettings;
    
    // UI Controls
    private Dropbox<Corner> cornerSelector;
    private Dropbox<String> fontSelector;
    private HSlider fontSizeSlider;
    private Label fontSizeLabel;
    private NColorWidget contentColorWidget;
    private CheckBox showBackgroundCheckbox;
    private NColorWidget backgroundColorWidget;
    private CheckBox showDecimalCheckbox;
    private CheckBox showOutlineCheckbox;
    private NColorWidget outlineColorWidget;
    private HSlider outlineWidthSlider;
    private Label outlineWidthLabel;
    private CheckBox useThresholdsCheckbox;
    private NColorWidget defaultColorWidget;
    
    // Thresholds list
    private ThresholdsList thresholdsList;
    private List<ThresholdItem> thresholdItems = new ArrayList<>();
    
    // Preview widget
    private PreviewWidget preview;
    
    public ItemOverlaySettings() {
        super("Item Quality Overlay");
        
        int margin = 10;
        int y = 35;
        int labelWidth = 110;  // Space for labels like "Content color:"
        int controlX = labelWidth + 5;  // Controls start after labels
        // 3:2 ratio - left column is 60%, right column is 40%
        int totalWidth = 560;
        int leftColumnWidth = (totalWidth * 3) / 5;  // 336
        int rightColumnX = leftColumnWidth + 10;  // Start of right column (unscaled)
        
        // === LEFT COLUMN ===
        
        // Corner position selector
        add(new Label("Corner:"), UI.scale(margin, y));
        cornerSelector = add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
            @Override
            protected Corner listitem(int i) { return Corner.values()[i]; }
            @Override
            protected int listitems() { return Corner.values().length; }
            @Override
            protected void drawitem(GOut g, Corner item, int i) {
                g.text(item.displayName, Coord.z);
            }
            @Override
            public void change(Corner item) {
                super.change(item);
                if (currentSettings != null) {
                    currentSettings.corner = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font family selector
        add(new Label("Font:"), UI.scale(margin, y));
        fontSelector = add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override
            protected int listitems() { return FONT_FAMILIES.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override
            public void change(String item) {
                super.change(item);
                if (currentSettings != null) {
                    currentSettings.fontFamily = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font size slider
        add(new Label("Size:"), UI.scale(margin, y));
        fontSizeSlider = add(new HSlider(UI.scale(80), 8, 20, 10) {
            @Override
            public void changed() {
                fontSizeLabel.settext(String.valueOf(val));
                if (currentSettings != null) {
                    currentSettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        fontSizeLabel = add(new Label("10"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show decimal checkbox
        showDecimalCheckbox = add(new CheckBox("Decimal (42.5)") {
            @Override
            public void changed(boolean val) {
                if (currentSettings != null) {
                    currentSettings.showDecimal = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Show outline checkbox
        showOutlineCheckbox = add(new CheckBox("Text outline") {
            @Override
            public void changed(boolean val) {
                if (currentSettings != null) {
                    currentSettings.showOutline = val;
                    outlineColorWidget.visible = val;
                    outlineWidthSlider.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Outline color & width
        add(new Label("Outline:"), UI.scale(margin + 15, y));
        outlineColorWidget = add(new NColorWidget(""), UI.scale(controlX, y - 8));
        outlineColorWidget.label.hide();
        outlineWidthSlider = add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                outlineWidthLabel.settext(String.valueOf(val));
                if (currentSettings != null) {
                    currentSettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        outlineWidthLabel = add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        showBackgroundCheckbox = add(new CheckBox("Background") {
            @Override
            public void changed(boolean val) {
                if (currentSettings != null) {
                    currentSettings.showBackground = val;
                    backgroundColorWidget.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Background color
        add(new Label("BG color:"), UI.scale(margin + 15, y + 8));
        backgroundColorWidget = add(new NColorWidget(""), UI.scale(controlX, y));
        backgroundColorWidget.label.hide();
        y += 30;
        
        // Content color
        add(new Label("Content color:"), UI.scale(margin, y + 8));
        contentColorWidget = add(new NColorWidget(""), UI.scale(controlX, y));
        contentColorWidget.label.hide();
        y += 35;
        
        // === RIGHT COLUMN (Quality Thresholds) ===
        int rightY = 35;
        
        add(new Label("● Quality Thresholds"), UI.scale(rightColumnX, rightY));
        rightY += 20;
        
        // Use thresholds checkbox
        useThresholdsCheckbox = add(new CheckBox("Use color thresholds") {
            @Override
            public void changed(boolean val) {
                if (currentSettings != null) {
                    currentSettings.useThresholds = val;
                    updatePreview();
                }
            }
        }, UI.scale(rightColumnX, rightY));
        rightY += 22;
        
        // Default color (when thresholds disabled)
        add(new Label("Default color:"), UI.scale(rightColumnX, rightY));
        defaultColorWidget = add(new NColorWidget(""), UI.scale(rightColumnX + 90, rightY - 8));
        defaultColorWidget.label.hide();
        rightY += 30;
        
        // Thresholds list
        add(new Label("Thresholds (Q >= value):"), UI.scale(rightColumnX, rightY));
        rightY += 18;
        
        // Thresholds scrollable list
        int listWidth = 210;
        thresholdsList = add(new ThresholdsList(UI.scale(new Coord(listWidth, 130))), UI.scale(rightColumnX, rightY));
        
        // Add button - aligned with right edge of list
        add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                if (currentSettings != null) {
                    // Add new threshold with default values
                    currentSettings.thresholds.add(new QualityThreshold(0, Color.WHITE));
                    currentSettings.sortThresholds();
                    rebuildThresholdsList();
                    updatePreview();
                }
            }
        }, UI.scale(rightColumnX + listWidth - 18, rightY - 20));
        
        rightY += 140;
        
        // === PREVIEW ===
        add(new Label("● Preview:"), UI.scale(margin, y));
        y += 18;
        preview = add(new PreviewWidget(), UI.scale(margin, y));
        
        // Load initial settings
        load();
    }
    
    private void rebuildThresholdsList() {
        thresholdItems.clear();
        if (currentSettings != null) {
            for (int i = 0; i < currentSettings.thresholds.size(); i++) {
                thresholdItems.add(new ThresholdItem(currentSettings.thresholds.get(i), i));
            }
        }
    }
    
    @Override
    public void load() {
        ItemQualityOverlaySettings settings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.itemQualityOverlay);
        if (settings == null) {
            settings = new ItemQualityOverlaySettings();
        }
        // Create a working copy
        currentSettings = settings.copy();
        
        // Update UI controls
        cornerSelector.sel = currentSettings.corner;
        fontSelector.sel = currentSettings.fontFamily;
        fontSizeSlider.val = currentSettings.fontSize;
        fontSizeLabel.settext(String.valueOf(currentSettings.fontSize));
        contentColorWidget.color = currentSettings.contentColor;
        showBackgroundCheckbox.a = currentSettings.showBackground;
        backgroundColorWidget.color = currentSettings.backgroundColor;
        backgroundColorWidget.visible = currentSettings.showBackground;
        showDecimalCheckbox.a = currentSettings.showDecimal;
        showOutlineCheckbox.a = currentSettings.showOutline;
        outlineColorWidget.color = currentSettings.outlineColor;
        outlineColorWidget.visible = currentSettings.showOutline;
        outlineWidthSlider.val = currentSettings.outlineWidth;
        outlineWidthLabel.settext(String.valueOf(currentSettings.outlineWidth));
        outlineWidthSlider.visible = currentSettings.showOutline;
        useThresholdsCheckbox.a = currentSettings.useThresholds;
        defaultColorWidget.color = currentSettings.defaultColor;
        
        rebuildThresholdsList();
        updatePreview();
    }
    
    @Override
    public void save() {
        if (currentSettings != null) {
            // Update colors from widgets
            currentSettings.contentColor = contentColorWidget.color;
            currentSettings.backgroundColor = backgroundColorWidget.color;
            currentSettings.outlineColor = outlineColorWidget.color;
            currentSettings.defaultColor = defaultColorWidget.color;
            
            // Update threshold colors from widgets
            for (ThresholdItem item : thresholdItems) {
                item.syncToSettings();
            }
            currentSettings.sortThresholds();
            
            NConfig.set(NConfig.Key.itemQualityOverlay, currentSettings);
            NConfig.needUpdate();
            
            // Force all quality overlays to refresh with new settings
            Quality.invalidateCache();
        }
    }
    
    private void updatePreview() {
        if (preview != null && currentSettings != null) {
            // Update colors from widgets
            currentSettings.contentColor = contentColorWidget.color;
            currentSettings.backgroundColor = backgroundColorWidget.color;
            currentSettings.outlineColor = outlineColorWidget.color;
            currentSettings.defaultColor = defaultColorWidget.color;
            
            // Sync threshold colors
            for (ThresholdItem item : thresholdItems) {
                item.syncToSettings();
            }
            
            preview.updateSettings(currentSettings);
        }
    }
    
    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (currentSettings != null) {
            boolean needsUpdate = false;
            if (!currentSettings.contentColor.equals(contentColorWidget.color)) {
                currentSettings.contentColor = contentColorWidget.color;
                needsUpdate = true;
            }
            if (!currentSettings.backgroundColor.equals(backgroundColorWidget.color)) {
                currentSettings.backgroundColor = backgroundColorWidget.color;
                needsUpdate = true;
            }
            if (!currentSettings.outlineColor.equals(outlineColorWidget.color)) {
                currentSettings.outlineColor = outlineColorWidget.color;
                needsUpdate = true;
            }
            if (!currentSettings.defaultColor.equals(defaultColorWidget.color)) {
                currentSettings.defaultColor = defaultColorWidget.color;
                needsUpdate = true;
            }
            // Check threshold colors
            for (ThresholdItem item : thresholdItems) {
                if (item.hasColorChanged()) {
                    item.syncToSettings();
                    needsUpdate = true;
                }
            }
            if (needsUpdate) {
                updatePreview();
            }
        }
    }
    
    /**
     * Widget representing a single threshold item in the list
     */
    private class ThresholdItem extends Widget {
        private QualityThreshold threshold;
        private int index;
        private TextEntry valueEntry;
        private NColorWidget colorWidget;
        private Color lastColor;
        private static final int ITEM_HEIGHT = 32;
        
        public ThresholdItem(QualityThreshold threshold, int index) {
            super(new Coord(UI.scale(180), UI.scale(ITEM_HEIGHT)));
            this.threshold = threshold;
            this.index = index;
            this.lastColor = threshold.color;
            
            int centerY = (UI.scale(ITEM_HEIGHT) - UI.scale(16)) / 2; // Center for text entry height
            int colorCenterY = (UI.scale(ITEM_HEIGHT) - Inventory.sqsz.y) / 2; // Center for color widget
            int btnCenterY = (UI.scale(ITEM_HEIGHT) - NStyle.removei[0].sz().y) / 2; // Center for button
            
            // Value entry - vertically centered
            valueEntry = add(new TextEntry(UI.scale(40), String.valueOf(threshold.threshold)) {
                @Override
                public boolean keydown(KeyDownEvent ev) {
                    boolean result = super.keydown(ev);
                    try {
                        threshold.threshold = Integer.parseInt(text());
                        currentSettings.sortThresholds();
                        updatePreview();
                    } catch (NumberFormatException ignored) {}
                    return result;
                }
            }, new Coord(0, centerY));
            
            // Color widget - vertically centered, after text entry
            int colorX = UI.scale(45);
            colorWidget = add(new NColorWidget(""), new Coord(colorX, colorCenterY));
            colorWidget.label.hide();
            colorWidget.color = threshold.color;
            
            // Remove button - vertically centered, to the right of color widget
            int removeX = colorX + colorWidget.sz.x + UI.scale(2);
            add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    if (currentSettings != null && currentSettings.thresholds.size() > 1) {
                        currentSettings.thresholds.remove(threshold);
                        rebuildThresholdsList();
                        updatePreview();
                    }
                }
            }, new Coord(removeX, btnCenterY));
        }
        
        public boolean hasColorChanged() {
            return !lastColor.equals(colorWidget.color);
        }
        
        public void syncToSettings() {
            threshold.color = colorWidget.color;
            lastColor = colorWidget.color;
            try {
                threshold.threshold = Integer.parseInt(valueEntry.text());
            } catch (NumberFormatException ignored) {}
        }
        
        @Override
        public void draw(GOut g) {
            super.draw(g);
        }
    }
    
    /**
     * List widget for thresholds
     */
    private class ThresholdsList extends SListBox<ThresholdItem, Widget> {
        public ThresholdsList(Coord sz) {
            super(sz, UI.scale(ThresholdItem.ITEM_HEIGHT + 2));
        }
        
        @Override
        protected List<ThresholdItem> items() {
            return thresholdItems;
        }
        
        @Override
        protected Widget makeitem(ThresholdItem item, int idx, Coord sz) {
            return new SListWidget.ItemWidget<ThresholdItem>(this, sz, item) {
                {
                    add(item);
                }
            };
        }
        
        @Override
        public void draw(GOut g) {
            g.chcolor(new Color(30, 35, 40, 180));
            g.frect(Coord.z, sz);
            g.chcolor();
            super.draw(g);
        }
    }
    
    /**
     * Preview widget showing mock inventory items with quality overlay
     */
    private class PreviewWidget extends Widget {
        private static final int ITEM_SIZE = 32;
        private TexI previewTex;
        private ItemQualityOverlaySettings settings;
        
        public PreviewWidget() {
            super(UI.scale(new Coord(540, 100)));
            settings = new ItemQualityOverlaySettings();
            rebuildPreview();
        }
        
        public void updateSettings(ItemQualityOverlaySettings newSettings) {
            this.settings = newSettings.copy();
            rebuildPreview();
        }
        
        private void rebuildPreview() {
            int scaledItemSize = UI.scale(ITEM_SIZE);
            int previewWidth = UI.scale(540);
            int previewHeight = UI.scale(100);
            
            BufferedImage img = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Draw background
            g.setColor(new Color(40, 40, 45));
            g.fillRect(0, 0, previewWidth, previewHeight);
            
            // Draw sample items with quality overlays
            int startX = UI.scale(20);
            int startY = UI.scale(25);
            int spacing = UI.scale(70);
            
            double[] qualities = {10.0, 35.0, 55.0, 80.0, 120.0, 200.0, 45.5};
            String[] labels = {"10", "35", "55", "80", "120", "200", "Cont"};
            boolean[] isContent = {false, false, false, false, false, false, true};
            
            for (int i = 0; i < qualities.length; i++) {
                int x = startX + i * spacing;
                int y = startY;
                
                // Draw inventory slot
                g.setColor(new Color(30, 30, 35));
                g.fillRect(x, y, scaledItemSize, scaledItemSize);
                g.setColor(new Color(70, 70, 80));
                g.drawRect(x, y, scaledItemSize, scaledItemSize);
                
                // Draw mock item
                Color itemColor = new Color(80 + i * 20, 60 + i * 15, 50 + i * 10);
                g.setColor(itemColor);
                int pad = UI.scale(4);
                g.fillRect(x + pad, y + pad, scaledItemSize - pad * 2, scaledItemSize - pad * 2);
                
                // Draw quality overlay
                drawQualityOverlay(g, x, y, scaledItemSize, qualities[i], isContent[i]);
                
                // Draw label
                g.setColor(Color.LIGHT_GRAY);
                g.setFont(new Font("SansSerif", Font.PLAIN, UI.scale(9)));
                FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(labels[i]);
                g.drawString(labels[i], x + (scaledItemSize - labelWidth) / 2, y + scaledItemSize + UI.scale(12));
            }
            
            g.dispose();
            previewTex = new TexI(img);
        }
        
        private void drawQualityOverlay(Graphics2D g, int itemX, int itemY, int itemSize, double quality, boolean isContent) {
            FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
            Font font;
            if (fontSettings != null) {
                font = fontSettings.getFont(settings.fontFamily);
                if (font == null) font = new Font("SansSerif", Font.BOLD, settings.fontSize);
                else font = font.deriveFont(Font.BOLD, (float) settings.fontSize);
            } else {
                font = new Font("SansSerif", Font.BOLD, settings.fontSize);
            }
            
            String text = settings.showDecimal ? String.format("%.1f", quality) : String.valueOf((int) Math.round(quality));
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            
            int pad = UI.scale(1);
            int x, y;
            switch (settings.corner) {
                case TOP_LEFT: x = itemX + pad; y = itemY + textHeight + pad; break;
                case TOP_RIGHT: x = itemX + itemSize - textWidth - pad; y = itemY + textHeight + pad; break;
                case BOTTOM_LEFT: x = itemX + pad; y = itemY + itemSize - pad; break;
                default: x = itemX + itemSize - textWidth - pad; y = itemY + itemSize - pad; break;
            }
            
            if (settings.showBackground) {
                g.setColor(settings.backgroundColor);
                g.fillRect(x - 1, y - textHeight, textWidth + 2, textHeight + 2);
            }
            
            Color textColor = isContent ? settings.contentColor : settings.getColorForQuality(quality);
            
            if (settings.showOutline) {
                g.setColor(settings.outlineColor);
                int w = settings.outlineWidth;
                for (int dx = -w; dx <= w; dx++) {
                    for (int dy = -w; dy <= w; dy++) {
                        if (dx != 0 || dy != 0) g.drawString(text, x + dx, y + dy);
                    }
                }
            }
            
            g.setColor(textColor);
            g.drawString(text, x, y);
        }
        
        @Override
        public void draw(GOut g) {
            if (previewTex != null) g.image(previewTex, Coord.z);
            g.chcolor(100, 100, 110, 255);
            g.rect(Coord.z, sz);
            g.chcolor();
        }
    }
}
