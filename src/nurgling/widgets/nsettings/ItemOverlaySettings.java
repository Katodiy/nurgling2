package nurgling.widgets.nsettings;

import haven.*;
import haven.res.ui.tt.q.quality.Quality;
import haven.res.ui.tt.stackn.Stack;
import nurgling.NConfig;
import nurgling.NStyle;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;
import nurgling.conf.ItemQualityOverlaySettings.Corner;
import nurgling.conf.ItemQualityOverlaySettings.QualityThreshold;
import nurgling.conf.ItemQualityOverlaySettings.TimeFormat;
import nurgling.i18n.L10n;
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
 * Features tabs for Item Quality and Stack Quality, with a shared preview.
 */
public class ItemOverlaySettings extends Panel {
    
    private static final List<String> FONT_FAMILIES = Arrays.asList(
            "Inter", "Roboto", "Sans", "Serif", "Fractur"
    );
    
    // Tab system
    private Button itemQualityTabBtn;
    private Button stackQualityTabBtn;
    private Button amountTabBtn;
    private Button studyInfoTabBtn;
    private Button progressTabBtn;
    private Button volumeTabBtn;
    private Widget itemQualityTab;
    private Widget stackQualityTab;
    private Widget amountTab;
    private Widget studyInfoTab;
    private Widget progressTab;
    private Widget volumeTab;
    private int activeTab = 0; // 0 = item, 1 = stack, 2 = amount, 3 = study, 4 = progress, 5 = volume
    
    // Current settings being edited
    private ItemQualityOverlaySettings currentSettings;
    private ItemQualityOverlaySettings currentStackSettings;
    private ItemQualityOverlaySettings currentAmountSettings;
    private ItemQualityOverlaySettings currentStudySettings;
    private ItemQualityOverlaySettings currentProgressSettings;
    private ItemQualityOverlaySettings currentVolumeSettings;
    
    // Item Quality UI Controls
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
    private ThresholdsList thresholdsList;
    private List<ThresholdItem> thresholdItems = new ArrayList<>();
    
    // Stack Quality UI Controls (no content color - stacks don't have content)
    private Dropbox<Corner> stackCornerSelector;
    private Dropbox<String> stackFontSelector;
    private HSlider stackFontSizeSlider;
    private Label stackFontSizeLabel;
    private CheckBox stackShowBackgroundCheckbox;
    private NColorWidget stackBackgroundColorWidget;
    private CheckBox stackShowDecimalCheckbox;
    private CheckBox stackShowOutlineCheckbox;
    private NColorWidget stackOutlineColorWidget;
    private HSlider stackOutlineWidthSlider;
    private Label stackOutlineWidthLabel;
    private CheckBox stackUseThresholdsCheckbox;
    private NColorWidget stackDefaultColorWidget;
    private ThresholdsList stackThresholdsList;
    private List<ThresholdItem> stackThresholdItems = new ArrayList<>();
    
    // Amount UI Controls (no content, no decimal, no thresholds - just simple number)
    private Dropbox<Corner> amountCornerSelector;
    private Dropbox<String> amountFontSelector;
    private HSlider amountFontSizeSlider;
    private Label amountFontSizeLabel;
    private CheckBox amountShowPrefixCheckbox;
    private CheckBox amountShowBackgroundCheckbox;
    private NColorWidget amountBackgroundColorWidget;
    private CheckBox amountShowOutlineCheckbox;
    private NColorWidget amountOutlineColorWidget;
    private HSlider amountOutlineWidthSlider;
    private Label amountOutlineWidthLabel;
    private NColorWidget amountColorWidget;
    
    // Study Info UI Controls
    private CheckBox studyHiddenCheckbox;
    private Dropbox<Corner> studyCornerSelector;
    private Dropbox<String> studyFontSelector;
    private HSlider studyFontSizeSlider;
    private Label studyFontSizeLabel;
    private CheckBox studyShowBackgroundCheckbox;
    private NColorWidget studyBackgroundColorWidget;
    private CheckBox studyShowOutlineCheckbox;
    private NColorWidget studyOutlineColorWidget;
    private HSlider studyOutlineWidthSlider;
    private Label studyOutlineWidthLabel;
    private NColorWidget studyColorWidget;
    private HSlider studyTimeRatioSlider;
    private Label studyTimeRatioLabel;
    private Dropbox<ItemQualityOverlaySettings.TimeFormat> studyTimeFormatSelector;
    private CheckBox studyCompactTooltipCheckbox;
    private CheckBox studyShowLphPerWeightCheckbox;
    
    // Progress UI Controls
    private CheckBox progressHiddenCheckbox;
    private Dropbox<Corner> progressCornerSelector;
    private Dropbox<String> progressFontSelector;
    private HSlider progressFontSizeSlider;
    private Label progressFontSizeLabel;
    private CheckBox progressShowBackgroundCheckbox;
    private NColorWidget progressBackgroundColorWidget;
    private CheckBox progressShowOutlineCheckbox;
    private NColorWidget progressOutlineColorWidget;
    private HSlider progressOutlineWidthSlider;
    private Label progressOutlineWidthLabel;
    private NColorWidget progressColorWidget;
    
    // Volume UI Controls
    private CheckBox volumeHiddenCheckbox;
    private Dropbox<Corner> volumeCornerSelector;
    private Dropbox<String> volumeFontSelector;
    private HSlider volumeFontSizeSlider;
    private Label volumeFontSizeLabel;
    private CheckBox volumeShowBackgroundCheckbox;
    private NColorWidget volumeBackgroundColorWidget;
    private CheckBox volumeShowOutlineCheckbox;
    private NColorWidget volumeOutlineColorWidget;
    private HSlider volumeOutlineWidthSlider;
    private Label volumeOutlineWidthLabel;
    private NColorWidget volumeColorWidget;
    
    // Preview widget (shared)
    private PreviewWidget preview;
    private CheckBox previewShowItemQ;
    private CheckBox previewShowStackQ;
    private CheckBox previewShowAmount;
    private CheckBox previewShowStudy;
    private CheckBox previewShowMeter;
    private CheckBox previewShowVol;
    
    public ItemOverlaySettings() {
        super(L10n.get("overlay.title"));
        
        int margin = 10;
        int tabY = 25;
        int tabWidth = 55;
        
        // === TAB BUTTONS ===
        itemQualityTabBtn = add(new Button(UI.scale(tabWidth), L10n.get("overlay.tab.item_q")) {
            @Override
            public void click() {
                switchTab(0);
            }
        }, UI.scale(0, tabY));
        
        stackQualityTabBtn = add(new Button(UI.scale(tabWidth), L10n.get("overlay.tab.stack_q")) {
            @Override
            public void click() {
                switchTab(1);
            }
        }, UI.scale(tabWidth + 2, tabY));
        
        amountTabBtn = add(new Button(UI.scale(tabWidth), L10n.get("overlay.tab.amount")) {
            @Override
            public void click() {
                switchTab(2);
            }
        }, UI.scale((tabWidth + 2) * 2, tabY));
        
        studyInfoTabBtn = add(new Button(UI.scale(tabWidth), L10n.get("overlay.tab.study")) {
            @Override
            public void click() {
                switchTab(3);
            }
        }, UI.scale((tabWidth + 2) * 3, tabY));
        
        progressTabBtn = add(new Button(UI.scale(tabWidth), L10n.get("overlay.tab.meter")) {
            @Override
            public void click() {
                switchTab(4);
            }
        }, UI.scale((tabWidth + 2) * 4, tabY));
        
        volumeTabBtn = add(new Button(UI.scale(tabWidth), L10n.get("overlay.tab.vol")) {
            @Override
            public void click() {
                switchTab(5);
            }
        }, UI.scale((tabWidth + 2) * 5, tabY));
        
        int contentY = tabY + 28 + 15;  // 15px gap after tabs
        
        // === ITEM QUALITY TAB ===
        itemQualityTab = add(new Widget(UI.scale(new Coord(560, 290))), UI.scale(margin, contentY));
        buildItemQualityTab(itemQualityTab);
        
        // === STACK QUALITY TAB ===
        stackQualityTab = add(new Widget(UI.scale(new Coord(560, 290))), UI.scale(margin, contentY));
        buildStackQualityTab(stackQualityTab);
        stackQualityTab.hide();
        
        // === AMOUNT TAB ===
        amountTab = add(new Widget(UI.scale(new Coord(560, 290))), UI.scale(margin, contentY));
        buildAmountTab(amountTab);
        amountTab.hide();
        
        // === STUDY INFO TAB ===
        studyInfoTab = add(new Widget(UI.scale(new Coord(560, 290))), UI.scale(margin, contentY));
        buildStudyInfoTab(studyInfoTab);
        studyInfoTab.hide();
        
        // === PROGRESS TAB ===
        progressTab = add(new Widget(UI.scale(new Coord(560, 290))), UI.scale(margin, contentY));
        buildProgressTab(progressTab);
        progressTab.hide();
        
        // === VOLUME TAB ===
        volumeTab = add(new Widget(UI.scale(new Coord(560, 290))), UI.scale(margin, contentY));
        buildVolumeTab(volumeTab);
        volumeTab.hide();
        
        // === PREVIEW (shared) ===
        int previewY = contentY + 290 + 15;  // 15px gap before preview (content height 290)
        add(new Label("● " + L10n.get("overlay.section.preview")), UI.scale(margin, previewY));
        
        // Preview visibility checkboxes
        int cbY = previewY + 2;
        int cbX = margin + 60;
        int cbSpacing = 70;
        
        previewShowItemQ = add(new CheckBox("ItemQ") {
            @Override public void changed(boolean val) { updatePreview(); }
        }, UI.scale(cbX, cbY));
        previewShowItemQ.a = true;
        
        previewShowStackQ = add(new CheckBox("StackQ") {
            @Override public void changed(boolean val) { updatePreview(); }
        }, UI.scale(cbX + cbSpacing, cbY));
        previewShowStackQ.a = false;
        
        previewShowAmount = add(new CheckBox("Amt") {
            @Override public void changed(boolean val) { updatePreview(); }
        }, UI.scale(cbX + cbSpacing * 2, cbY));
        previewShowAmount.a = true;
        
        previewShowStudy = add(new CheckBox("Study") {
            @Override public void changed(boolean val) { updatePreview(); }
        }, UI.scale(cbX + cbSpacing * 3, cbY));
        previewShowStudy.a = false;
        
        previewShowMeter = add(new CheckBox("Meter") {
            @Override public void changed(boolean val) { updatePreview(); }
        }, UI.scale(cbX + cbSpacing * 4, cbY));
        previewShowMeter.a = false;
        
        previewShowVol = add(new CheckBox("Vol") {
            @Override public void changed(boolean val) { updatePreview(); }
        }, UI.scale(cbX + cbSpacing * 5, cbY));
        previewShowVol.a = false;
        
        preview = add(new PreviewWidget(), UI.scale(margin, previewY + 18));
        
        // Load initial settings
        load();
    }
    
    private void switchTab(int tab) {
        activeTab = tab;
        itemQualityTab.visible = (tab == 0);
        stackQualityTab.visible = (tab == 1);
        amountTab.visible = (tab == 2);
        studyInfoTab.visible = (tab == 3);
        progressTab.visible = (tab == 4);
        volumeTab.visible = (tab == 5);
        updatePreview();
    }
    
    private void buildItemQualityTab(Widget parent) {
        int margin = 0;
        int y = 0;
        int labelWidth = 110;
        int controlX = labelWidth + 5;
        int totalWidth = 560;
        int leftColumnWidth = (totalWidth * 3) / 5;
        int rightColumnX = leftColumnWidth + 10;
        
        // === LEFT COLUMN ===
        
        // Corner position selector
        parent.add(new Label(L10n.get("overlay.corner")), UI.scale(margin, y));
        cornerSelector = parent.add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
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
        parent.add(new Label(L10n.get("overlay.font")), UI.scale(margin, y));
        fontSelector = parent.add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
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
        parent.add(new Label(L10n.get("overlay.size")), UI.scale(margin, y));
        fontSizeSlider = parent.add(new HSlider(UI.scale(80), 8, 20, 10) {
            @Override
            public void changed() {
                fontSizeLabel.settext(String.valueOf(val));
                if (currentSettings != null) {
                    currentSettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        fontSizeLabel = parent.add(new Label("10"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show decimal checkbox
        showDecimalCheckbox = parent.add(new CheckBox(L10n.get("overlay.decimal")) {
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
        showOutlineCheckbox = parent.add(new CheckBox(L10n.get("overlay.text_outline")) {
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
        parent.add(new Label(L10n.get("overlay.outline")), UI.scale(margin + 15, y));
        outlineColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y - 8));
        outlineColorWidget.label.hide();
        outlineWidthSlider = parent.add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                outlineWidthLabel.settext(String.valueOf(val));
                if (currentSettings != null) {
                    currentSettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        outlineWidthLabel = parent.add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        showBackgroundCheckbox = parent.add(new CheckBox(L10n.get("overlay.background")) {
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
        parent.add(new Label(L10n.get("overlay.bg_color")), UI.scale(margin + 15, y + 8));
        backgroundColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        backgroundColorWidget.label.hide();
        y += 30;
        
        // Content color
        parent.add(new Label(L10n.get("overlay.content_color")), UI.scale(margin, y + 8));
        contentColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        contentColorWidget.label.hide();
        
        // === RIGHT COLUMN (Quality Thresholds) ===
        int rightY = 0;
        
        parent.add(new Label("● " + L10n.get("overlay.section.thresholds")), UI.scale(rightColumnX, rightY));
        rightY += 20;
        
        // Use thresholds checkbox
        useThresholdsCheckbox = parent.add(new CheckBox(L10n.get("overlay.use_thresholds")) {
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
        parent.add(new Label(L10n.get("overlay.default_color")), UI.scale(rightColumnX, rightY));
        defaultColorWidget = parent.add(new NColorWidget(""), UI.scale(rightColumnX + 90, rightY - 8));
        defaultColorWidget.label.hide();
        rightY += 30;
        
        // Thresholds list
        parent.add(new Label(L10n.get("overlay.thresholds_list")), UI.scale(rightColumnX, rightY));
        rightY += 18;
        
        int listWidth = 210;
        thresholdsList = parent.add(new ThresholdsList(UI.scale(new Coord(listWidth, 130)), false), UI.scale(rightColumnX, rightY));
        
        // Add button
        parent.add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                if (currentSettings != null) {
                    currentSettings.thresholds.add(new QualityThreshold(0, Color.WHITE));
                    currentSettings.sortThresholds();
                    rebuildThresholdsList(false);
                    updatePreview();
                }
            }
        }, UI.scale(rightColumnX + listWidth - 18, rightY - 20));
    }
    
    private void buildStackQualityTab(Widget parent) {
        int margin = 0;
        int y = 0;
        int labelWidth = 110;
        int controlX = labelWidth + 5;
        int totalWidth = 560;
        int leftColumnWidth = (totalWidth * 3) / 5;
        int rightColumnX = leftColumnWidth + 10;
        
        // === LEFT COLUMN ===
        
        // Corner position selector
        parent.add(new Label(L10n.get("overlay.corner")), UI.scale(margin, y));
        stackCornerSelector = parent.add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
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
                if (currentStackSettings != null) {
                    currentStackSettings.corner = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font family selector
        parent.add(new Label(L10n.get("overlay.font")), UI.scale(margin, y));
        stackFontSelector = parent.add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override
            protected int listitems() { return FONT_FAMILIES.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override
            public void change(String item) {
                super.change(item);
                if (currentStackSettings != null) {
                    currentStackSettings.fontFamily = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font size slider
        parent.add(new Label(L10n.get("overlay.size")), UI.scale(margin, y));
        stackFontSizeSlider = parent.add(new HSlider(UI.scale(80), 8, 20, 10) {
            @Override
            public void changed() {
                stackFontSizeLabel.settext(String.valueOf(val));
                if (currentStackSettings != null) {
                    currentStackSettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        stackFontSizeLabel = parent.add(new Label("10"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show decimal checkbox
        stackShowDecimalCheckbox = parent.add(new CheckBox(L10n.get("overlay.decimal")) {
            @Override
            public void changed(boolean val) {
                if (currentStackSettings != null) {
                    currentStackSettings.showDecimal = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Show outline checkbox
        stackShowOutlineCheckbox = parent.add(new CheckBox(L10n.get("overlay.text_outline")) {
            @Override
            public void changed(boolean val) {
                if (currentStackSettings != null) {
                    currentStackSettings.showOutline = val;
                    stackOutlineColorWidget.visible = val;
                    stackOutlineWidthSlider.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Outline color & width
        parent.add(new Label(L10n.get("overlay.outline")), UI.scale(margin + 15, y));
        stackOutlineColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y - 8));
        stackOutlineColorWidget.label.hide();
        stackOutlineWidthSlider = parent.add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                stackOutlineWidthLabel.settext(String.valueOf(val));
                if (currentStackSettings != null) {
                    currentStackSettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        stackOutlineWidthLabel = parent.add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        stackShowBackgroundCheckbox = parent.add(new CheckBox(L10n.get("overlay.background")) {
            @Override
            public void changed(boolean val) {
                if (currentStackSettings != null) {
                    currentStackSettings.showBackground = val;
                    stackBackgroundColorWidget.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Background color
        parent.add(new Label(L10n.get("overlay.bg_color")), UI.scale(margin + 15, y + 8));
        stackBackgroundColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        stackBackgroundColorWidget.label.hide();
        
        // === RIGHT COLUMN (Quality Thresholds) ===
        int rightY = 0;
        
        parent.add(new Label("● " + L10n.get("overlay.section.thresholds")), UI.scale(rightColumnX, rightY));
        rightY += 20;
        
        // Use thresholds checkbox
        stackUseThresholdsCheckbox = parent.add(new CheckBox(L10n.get("overlay.use_thresholds")) {
            @Override
            public void changed(boolean val) {
                if (currentStackSettings != null) {
                    currentStackSettings.useThresholds = val;
                    updatePreview();
                }
            }
        }, UI.scale(rightColumnX, rightY));
        rightY += 22;
        
        // Default color
        parent.add(new Label(L10n.get("overlay.default_color")), UI.scale(rightColumnX, rightY));
        stackDefaultColorWidget = parent.add(new NColorWidget(""), UI.scale(rightColumnX + 90, rightY - 8));
        stackDefaultColorWidget.label.hide();
        rightY += 30;
        
        // Thresholds list
        parent.add(new Label(L10n.get("overlay.thresholds_list")), UI.scale(rightColumnX, rightY));
        rightY += 18;
        
        int listWidth = 210;
        stackThresholdsList = parent.add(new ThresholdsList(UI.scale(new Coord(listWidth, 130)), true), UI.scale(rightColumnX, rightY));
        
        // Add button
        parent.add(new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                if (currentStackSettings != null) {
                    currentStackSettings.thresholds.add(new QualityThreshold(0, Color.WHITE));
                    currentStackSettings.sortThresholds();
                    rebuildThresholdsList(true);
                    updatePreview();
                }
            }
        }, UI.scale(rightColumnX + listWidth - 18, rightY - 20));
    }
    
    private void buildAmountTab(Widget parent) {
        int margin = 0;
        int y = 0;
        int labelWidth = 110;
        int controlX = labelWidth + 5;
        
        // Corner position selector
        parent.add(new Label(L10n.get("overlay.corner")), UI.scale(margin, y));
        amountCornerSelector = parent.add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
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
                if (currentAmountSettings != null) {
                    currentAmountSettings.corner = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font family selector
        parent.add(new Label(L10n.get("overlay.font")), UI.scale(margin, y));
        amountFontSelector = parent.add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override
            protected int listitems() { return FONT_FAMILIES.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override
            public void change(String item) {
                super.change(item);
                if (currentAmountSettings != null) {
                    currentAmountSettings.fontFamily = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font size slider
        parent.add(new Label(L10n.get("overlay.size")), UI.scale(margin, y));
        amountFontSizeSlider = parent.add(new HSlider(UI.scale(80), 8, 20, 10) {
            @Override
            public void changed() {
                amountFontSizeLabel.settext(String.valueOf(val));
                if (currentAmountSettings != null) {
                    currentAmountSettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        amountFontSizeLabel = parent.add(new Label("10"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show prefix checkbox (×5 instead of 5)
        amountShowPrefixCheckbox = parent.add(new CheckBox(L10n.get("overlay.prefix")) {
            @Override
            public void changed(boolean val) {
                if (currentAmountSettings != null) {
                    currentAmountSettings.showAmountPrefix = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Show outline checkbox
        amountShowOutlineCheckbox = parent.add(new CheckBox(L10n.get("overlay.text_outline")) {
            @Override
            public void changed(boolean val) {
                if (currentAmountSettings != null) {
                    currentAmountSettings.showOutline = val;
                    amountOutlineColorWidget.visible = val;
                    amountOutlineWidthSlider.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Outline color & width
        parent.add(new Label(L10n.get("overlay.outline")), UI.scale(margin + 15, y));
        amountOutlineColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y - 8));
        amountOutlineColorWidget.label.hide();
        amountOutlineWidthSlider = parent.add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                amountOutlineWidthLabel.settext(String.valueOf(val));
                if (currentAmountSettings != null) {
                    currentAmountSettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        amountOutlineWidthLabel = parent.add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        amountShowBackgroundCheckbox = parent.add(new CheckBox(L10n.get("overlay.background")) {
            @Override
            public void changed(boolean val) {
                if (currentAmountSettings != null) {
                    currentAmountSettings.showBackground = val;
                    amountBackgroundColorWidget.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Background color
        parent.add(new Label(L10n.get("overlay.bg_color")), UI.scale(margin + 15, y + 8));
        amountBackgroundColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        amountBackgroundColorWidget.label.hide();
        y += 30;
        
        // Text color
        parent.add(new Label(L10n.get("overlay.text_color")), UI.scale(margin, y + 8));
        amountColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        amountColorWidget.label.hide();
    }
    
    private void buildStudyInfoTab(Widget parent) {
        int margin = 0;
        int y = 0;
        int labelWidth = 110;
        int controlX = labelWidth + 5;
        
        // Hide checkbox
        studyHiddenCheckbox = parent.add(new CheckBox(L10n.get("overlay.hide")) {
            @Override
            public void changed(boolean val) {
                if (currentStudySettings != null) {
                    currentStudySettings.hidden = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;

        // Compact tooltip checkbox
        studyCompactTooltipCheckbox = parent.add(new CheckBox(L10n.get("overlay.compact_tooltip")) {
            @Override
            public void changed(boolean val) {
                if (currentStudySettings != null) {
                    currentStudySettings.compactTooltip = val;
                    studyShowLphPerWeightCheckbox.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;

        // Show LP/H/Weight checkbox (only visible when compact tooltip is enabled)
        studyShowLphPerWeightCheckbox = parent.add(new CheckBox(L10n.get("overlay.show_lph_weight")) {
            @Override
            public void changed(boolean val) {
                if (currentStudySettings != null) {
                    currentStudySettings.showLphPerWeight = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin + 15, y));
        y += 22;

        // Corner position selector
        parent.add(new Label(L10n.get("overlay.corner")), UI.scale(margin, y));
        studyCornerSelector = parent.add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
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
                if (currentStudySettings != null) {
                    currentStudySettings.corner = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Time format selector
        parent.add(new Label(L10n.get("overlay.time_format")), UI.scale(margin, y));
        studyTimeFormatSelector = parent.add(new Dropbox<TimeFormat>(UI.scale(100), TimeFormat.values().length, UI.scale(16)) {
            @Override
            protected TimeFormat listitem(int i) { return TimeFormat.values()[i]; }
            @Override
            protected int listitems() { return TimeFormat.values().length; }
            @Override
            protected void drawitem(GOut g, TimeFormat item, int i) {
                g.text(item.displayName, Coord.z);
            }
            @Override
            public void change(TimeFormat item) {
                super.change(item);
                if (currentStudySettings != null) {
                    currentStudySettings.timeFormat = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Time ratio slider (2.0 - 5.0, default 3.29)
        parent.add(new Label(L10n.get("overlay.time_ratio")), UI.scale(margin, y));
        studyTimeRatioSlider = parent.add(new HSlider(UI.scale(360), 200, 500, 329) {
            @Override
            public void changed() {
                float ratio = val / 100.0f;
                studyTimeRatioLabel.settext(String.format("%.2f", ratio));
                if (currentStudySettings != null) {
                    currentStudySettings.studyTimeRatio = ratio;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        studyTimeRatioLabel = parent.add(new Label("3.29"), UI.scale(controlX + 370, y));
        y += 25;
        
        // Font family selector
        parent.add(new Label(L10n.get("overlay.font")), UI.scale(margin, y));
        studyFontSelector = parent.add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override
            protected int listitems() { return FONT_FAMILIES.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override
            public void change(String item) {
                super.change(item);
                if (currentStudySettings != null) {
                    currentStudySettings.fontFamily = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font size slider
        parent.add(new Label(L10n.get("overlay.size")), UI.scale(margin, y));
        studyFontSizeSlider = parent.add(new HSlider(UI.scale(80), 8, 20, 9) {
            @Override
            public void changed() {
                studyFontSizeLabel.settext(String.valueOf(val));
                if (currentStudySettings != null) {
                    currentStudySettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        studyFontSizeLabel = parent.add(new Label("9"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show outline checkbox
        studyShowOutlineCheckbox = parent.add(new CheckBox(L10n.get("overlay.text_outline")) {
            @Override
            public void changed(boolean val) {
                if (currentStudySettings != null) {
                    currentStudySettings.showOutline = val;
                    studyOutlineColorWidget.visible = val;
                    studyOutlineWidthSlider.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Outline color & width
        parent.add(new Label(L10n.get("overlay.outline")), UI.scale(margin + 15, y));
        studyOutlineColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y - 8));
        studyOutlineColorWidget.label.hide();
        studyOutlineWidthSlider = parent.add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                studyOutlineWidthLabel.settext(String.valueOf(val));
                if (currentStudySettings != null) {
                    currentStudySettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        studyOutlineWidthLabel = parent.add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        studyShowBackgroundCheckbox = parent.add(new CheckBox(L10n.get("overlay.background")) {
            @Override
            public void changed(boolean val) {
                if (currentStudySettings != null) {
                    currentStudySettings.showBackground = val;
                    studyBackgroundColorWidget.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Background color
        parent.add(new Label(L10n.get("overlay.bg_color")), UI.scale(margin + 15, y + 8));
        studyBackgroundColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        studyBackgroundColorWidget.label.hide();
        y += 30;
        
        // Text color
        parent.add(new Label(L10n.get("overlay.text_color")), UI.scale(margin, y + 8));
        studyColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        studyColorWidget.label.hide();
    }
    
    private void buildProgressTab(Widget parent) {
        int margin = 0;
        int y = 0;
        int labelWidth = 110;
        int controlX = labelWidth + 5;
        
        // Hide checkbox
        progressHiddenCheckbox = parent.add(new CheckBox(L10n.get("overlay.hide")) {
            @Override
            public void changed(boolean val) {
                if (currentProgressSettings != null) {
                    currentProgressSettings.hidden = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Corner position selector
        parent.add(new Label(L10n.get("overlay.corner")), UI.scale(margin, y));
        progressCornerSelector = parent.add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
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
                if (currentProgressSettings != null) {
                    currentProgressSettings.corner = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font family selector
        parent.add(new Label(L10n.get("overlay.font")), UI.scale(margin, y));
        progressFontSelector = parent.add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override
            protected int listitems() { return FONT_FAMILIES.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override
            public void change(String item) {
                super.change(item);
                if (currentProgressSettings != null) {
                    currentProgressSettings.fontFamily = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font size slider
        parent.add(new Label(L10n.get("overlay.size")), UI.scale(margin, y));
        progressFontSizeSlider = parent.add(new HSlider(UI.scale(80), 8, 20, 10) {
            @Override
            public void changed() {
                progressFontSizeLabel.settext(String.valueOf(val));
                if (currentProgressSettings != null) {
                    currentProgressSettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        progressFontSizeLabel = parent.add(new Label("10"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show outline checkbox
        progressShowOutlineCheckbox = parent.add(new CheckBox(L10n.get("overlay.text_outline")) {
            @Override
            public void changed(boolean val) {
                if (currentProgressSettings != null) {
                    currentProgressSettings.showOutline = val;
                    progressOutlineColorWidget.visible = val;
                    progressOutlineWidthSlider.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Outline color & width
        parent.add(new Label(L10n.get("overlay.outline")), UI.scale(margin + 15, y));
        progressOutlineColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y - 8));
        progressOutlineColorWidget.label.hide();
        progressOutlineWidthSlider = parent.add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                progressOutlineWidthLabel.settext(String.valueOf(val));
                if (currentProgressSettings != null) {
                    currentProgressSettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        progressOutlineWidthLabel = parent.add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        progressShowBackgroundCheckbox = parent.add(new CheckBox(L10n.get("overlay.background")) {
            @Override
            public void changed(boolean val) {
                if (currentProgressSettings != null) {
                    currentProgressSettings.showBackground = val;
                    progressBackgroundColorWidget.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Background color
        parent.add(new Label(L10n.get("overlay.bg_color")), UI.scale(margin + 15, y + 8));
        progressBackgroundColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        progressBackgroundColorWidget.label.hide();
        y += 30;
        
        // Text color
        parent.add(new Label(L10n.get("overlay.text_color")), UI.scale(margin, y + 8));
        progressColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        progressColorWidget.label.hide();
    }
    
    private void buildVolumeTab(Widget parent) {
        int margin = 0;
        int y = 0;
        int labelWidth = 110;
        int controlX = labelWidth + 5;
        
        // Hide checkbox
        volumeHiddenCheckbox = parent.add(new CheckBox(L10n.get("overlay.hide")) {
            @Override
            public void changed(boolean val) {
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.hidden = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Corner position selector
        parent.add(new Label(L10n.get("overlay.corner")), UI.scale(margin, y));
        volumeCornerSelector = parent.add(new Dropbox<Corner>(UI.scale(100), Corner.values().length, UI.scale(16)) {
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
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.corner = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font family selector
        parent.add(new Label(L10n.get("overlay.font")), UI.scale(margin, y));
        volumeFontSelector = parent.add(new Dropbox<String>(UI.scale(100), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override
            protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override
            protected int listitems() { return FONT_FAMILIES.size(); }
            @Override
            protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override
            public void change(String item) {
                super.change(item);
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.fontFamily = item;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y));
        y += 25;
        
        // Font size slider
        parent.add(new Label(L10n.get("overlay.size")), UI.scale(margin, y));
        volumeFontSizeSlider = parent.add(new HSlider(UI.scale(80), 8, 20, 10) {
            @Override
            public void changed() {
                volumeFontSizeLabel.settext(String.valueOf(val));
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.fontSize = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX, y + 2));
        volumeFontSizeLabel = parent.add(new Label("10"), UI.scale(controlX + 90, y));
        y += 25;
        
        // Show outline checkbox
        volumeShowOutlineCheckbox = parent.add(new CheckBox(L10n.get("overlay.text_outline")) {
            @Override
            public void changed(boolean val) {
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.showOutline = val;
                    volumeOutlineColorWidget.visible = val;
                    volumeOutlineWidthSlider.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Outline color & width
        parent.add(new Label(L10n.get("overlay.outline")), UI.scale(margin + 15, y));
        volumeOutlineColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y - 8));
        volumeOutlineColorWidget.label.hide();
        volumeOutlineWidthSlider = parent.add(new HSlider(UI.scale(50), 1, 3, 1) {
            @Override
            public void changed() {
                volumeOutlineWidthLabel.settext(String.valueOf(val));
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.outlineWidth = val;
                    updatePreview();
                }
            }
        }, UI.scale(controlX + 15, y + 2));
        volumeOutlineWidthLabel = parent.add(new Label("1"), UI.scale(controlX + 70, y));
        y += 28;
        
        // Show background checkbox
        volumeShowBackgroundCheckbox = parent.add(new CheckBox(L10n.get("overlay.background")) {
            @Override
            public void changed(boolean val) {
                if (currentVolumeSettings != null) {
                    currentVolumeSettings.showBackground = val;
                    volumeBackgroundColorWidget.visible = val;
                    updatePreview();
                }
            }
        }, UI.scale(margin, y));
        y += 22;
        
        // Background color
        parent.add(new Label(L10n.get("overlay.bg_color")), UI.scale(margin + 15, y + 8));
        volumeBackgroundColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        volumeBackgroundColorWidget.label.hide();
        y += 30;
        
        // Text color
        parent.add(new Label(L10n.get("overlay.text_color")), UI.scale(margin, y + 8));
        volumeColorWidget = parent.add(new NColorWidget(""), UI.scale(controlX, y));
        volumeColorWidget.label.hide();
    }
    
    private void rebuildThresholdsList(boolean isStack) {
        if (isStack) {
            stackThresholdItems.clear();
            if (currentStackSettings != null) {
                for (int i = 0; i < currentStackSettings.thresholds.size(); i++) {
                    stackThresholdItems.add(new ThresholdItem(currentStackSettings.thresholds.get(i), i, true));
                }
            }
        } else {
            thresholdItems.clear();
            if (currentSettings != null) {
                for (int i = 0; i < currentSettings.thresholds.size(); i++) {
                    thresholdItems.add(new ThresholdItem(currentSettings.thresholds.get(i), i, false));
                }
            }
        }
    }
    
    @Override
    public void load() {
        // Load Item Quality settings
        ItemQualityOverlaySettings settings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.itemQualityOverlay);
        if (settings == null) {
            settings = new ItemQualityOverlaySettings();
        }
        currentSettings = settings.copy();
        
        // Load Stack Quality settings
        ItemQualityOverlaySettings stackSettings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.stackQualityOverlay);
        if (stackSettings == null) {
            stackSettings = new ItemQualityOverlaySettings();
            stackSettings.corner = Corner.TOP_LEFT; // Default different corner for stacks
        }
        currentStackSettings = stackSettings.copy();
        
        // Update Item Quality UI controls
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
        
        // Update Stack Quality UI controls
        stackCornerSelector.sel = currentStackSettings.corner;
        stackFontSelector.sel = currentStackSettings.fontFamily;
        stackFontSizeSlider.val = currentStackSettings.fontSize;
        stackFontSizeLabel.settext(String.valueOf(currentStackSettings.fontSize));
        stackShowBackgroundCheckbox.a = currentStackSettings.showBackground;
        stackBackgroundColorWidget.color = currentStackSettings.backgroundColor;
        stackBackgroundColorWidget.visible = currentStackSettings.showBackground;
        stackShowDecimalCheckbox.a = currentStackSettings.showDecimal;
        stackShowOutlineCheckbox.a = currentStackSettings.showOutline;
        stackOutlineColorWidget.color = currentStackSettings.outlineColor;
        stackOutlineColorWidget.visible = currentStackSettings.showOutline;
        stackOutlineWidthSlider.val = currentStackSettings.outlineWidth;
        stackOutlineWidthLabel.settext(String.valueOf(currentStackSettings.outlineWidth));
        stackOutlineWidthSlider.visible = currentStackSettings.showOutline;
        stackUseThresholdsCheckbox.a = currentStackSettings.useThresholds;
        stackDefaultColorWidget.color = currentStackSettings.defaultColor;
        
        // Load Amount settings
        ItemQualityOverlaySettings amountSettings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.amountOverlay);
        if (amountSettings == null) {
            amountSettings = new ItemQualityOverlaySettings();
            amountSettings.corner = Corner.BOTTOM_RIGHT;
            amountSettings.useThresholds = false;
        }
        currentAmountSettings = amountSettings.copy();
        
        // Update Amount UI controls
        amountCornerSelector.sel = currentAmountSettings.corner;
        amountFontSelector.sel = currentAmountSettings.fontFamily;
        amountFontSizeSlider.val = currentAmountSettings.fontSize;
        amountFontSizeLabel.settext(String.valueOf(currentAmountSettings.fontSize));
        amountShowPrefixCheckbox.a = currentAmountSettings.showAmountPrefix;
        amountShowBackgroundCheckbox.a = currentAmountSettings.showBackground;
        amountBackgroundColorWidget.color = currentAmountSettings.backgroundColor;
        amountBackgroundColorWidget.visible = currentAmountSettings.showBackground;
        amountShowOutlineCheckbox.a = currentAmountSettings.showOutline;
        amountOutlineColorWidget.color = currentAmountSettings.outlineColor;
        amountOutlineColorWidget.visible = currentAmountSettings.showOutline;
        amountOutlineWidthSlider.val = currentAmountSettings.outlineWidth;
        amountOutlineWidthLabel.settext(String.valueOf(currentAmountSettings.outlineWidth));
        amountOutlineWidthSlider.visible = currentAmountSettings.showOutline;
        amountColorWidget.color = currentAmountSettings.defaultColor;
        
        // Load Study Info settings
        ItemQualityOverlaySettings studySettings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.studyInfoOverlay);
        if (studySettings == null) {
            studySettings = new ItemQualityOverlaySettings();
            studySettings.corner = Corner.BOTTOM_LEFT;
            studySettings.defaultColor = new Color(255, 255, 50);
        }
        currentStudySettings = studySettings.copy();
        
        // Update Study Info UI controls
        studyCornerSelector.sel = currentStudySettings.corner;
        studyTimeFormatSelector.sel = currentStudySettings.timeFormat;
        studyTimeRatioSlider.val = (int)(currentStudySettings.studyTimeRatio * 100);
        studyTimeRatioLabel.settext(String.format("%.2f", currentStudySettings.studyTimeRatio));
        studyFontSelector.sel = currentStudySettings.fontFamily;
        studyFontSizeSlider.val = currentStudySettings.fontSize;
        studyFontSizeLabel.settext(String.valueOf(currentStudySettings.fontSize));
        studyShowBackgroundCheckbox.a = currentStudySettings.showBackground;
        studyBackgroundColorWidget.color = currentStudySettings.backgroundColor;
        studyBackgroundColorWidget.visible = currentStudySettings.showBackground;
        studyShowOutlineCheckbox.a = currentStudySettings.showOutline;
        studyOutlineColorWidget.color = currentStudySettings.outlineColor;
        studyOutlineColorWidget.visible = currentStudySettings.showOutline;
        studyOutlineWidthSlider.val = currentStudySettings.outlineWidth;
        studyOutlineWidthLabel.settext(String.valueOf(currentStudySettings.outlineWidth));
        studyOutlineWidthSlider.visible = currentStudySettings.showOutline;
        studyColorWidget.color = currentStudySettings.defaultColor;
        studyHiddenCheckbox.a = currentStudySettings.hidden;
        studyCompactTooltipCheckbox.a = currentStudySettings.compactTooltip;
        studyShowLphPerWeightCheckbox.a = currentStudySettings.showLphPerWeight;
        studyShowLphPerWeightCheckbox.visible = currentStudySettings.compactTooltip;

        // Load Progress settings
        ItemQualityOverlaySettings progressSettings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.progressOverlay);
        if (progressSettings == null) {
            progressSettings = new ItemQualityOverlaySettings();
            progressSettings.corner = Corner.BOTTOM_LEFT;
            progressSettings.defaultColor = new Color(234, 164, 101);
            progressSettings.showBackground = true;
        }
        currentProgressSettings = progressSettings.copy();
        
        // Update Progress UI controls
        progressCornerSelector.sel = currentProgressSettings.corner;
        progressFontSelector.sel = currentProgressSettings.fontFamily;
        progressFontSizeSlider.val = currentProgressSettings.fontSize;
        progressFontSizeLabel.settext(String.valueOf(currentProgressSettings.fontSize));
        progressShowBackgroundCheckbox.a = currentProgressSettings.showBackground;
        progressBackgroundColorWidget.color = currentProgressSettings.backgroundColor;
        progressBackgroundColorWidget.visible = currentProgressSettings.showBackground;
        progressShowOutlineCheckbox.a = currentProgressSettings.showOutline;
        progressOutlineColorWidget.color = currentProgressSettings.outlineColor;
        progressOutlineColorWidget.visible = currentProgressSettings.showOutline;
        progressOutlineWidthSlider.val = currentProgressSettings.outlineWidth;
        progressOutlineWidthLabel.settext(String.valueOf(currentProgressSettings.outlineWidth));
        progressOutlineWidthSlider.visible = currentProgressSettings.showOutline;
        progressColorWidget.color = currentProgressSettings.defaultColor;
        progressHiddenCheckbox.a = currentProgressSettings.hidden;
        
        // Load Volume settings
        ItemQualityOverlaySettings volumeSettings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.volumeOverlay);
        if (volumeSettings == null) {
            volumeSettings = new ItemQualityOverlaySettings();
            volumeSettings.corner = Corner.TOP_LEFT;
            volumeSettings.defaultColor = new Color(65, 255, 115);
            volumeSettings.showBackground = true;
        }
        currentVolumeSettings = volumeSettings.copy();
        
        // Update Volume UI controls
        volumeCornerSelector.sel = currentVolumeSettings.corner;
        volumeFontSelector.sel = currentVolumeSettings.fontFamily;
        volumeFontSizeSlider.val = currentVolumeSettings.fontSize;
        volumeFontSizeLabel.settext(String.valueOf(currentVolumeSettings.fontSize));
        volumeShowBackgroundCheckbox.a = currentVolumeSettings.showBackground;
        volumeBackgroundColorWidget.color = currentVolumeSettings.backgroundColor;
        volumeBackgroundColorWidget.visible = currentVolumeSettings.showBackground;
        volumeShowOutlineCheckbox.a = currentVolumeSettings.showOutline;
        volumeOutlineColorWidget.color = currentVolumeSettings.outlineColor;
        volumeOutlineColorWidget.visible = currentVolumeSettings.showOutline;
        volumeOutlineWidthSlider.val = currentVolumeSettings.outlineWidth;
        volumeOutlineWidthLabel.settext(String.valueOf(currentVolumeSettings.outlineWidth));
        volumeOutlineWidthSlider.visible = currentVolumeSettings.showOutline;
        volumeColorWidget.color = currentVolumeSettings.defaultColor;
        volumeHiddenCheckbox.a = currentVolumeSettings.hidden;
        
        rebuildThresholdsList(false);
        rebuildThresholdsList(true);
        updatePreview();
    }
    
    @Override
    public void save() {
        // Save Item Quality settings
        if (currentSettings != null) {
            currentSettings.contentColor = contentColorWidget.color;
            currentSettings.backgroundColor = backgroundColorWidget.color;
            currentSettings.outlineColor = outlineColorWidget.color;
            currentSettings.defaultColor = defaultColorWidget.color;
            
            for (ThresholdItem item : thresholdItems) {
                item.syncToSettings();
            }
            currentSettings.sortThresholds();
            
            NConfig.set(NConfig.Key.itemQualityOverlay, currentSettings);
        }
        
        // Save Stack Quality settings
        if (currentStackSettings != null) {
            currentStackSettings.backgroundColor = stackBackgroundColorWidget.color;
            currentStackSettings.outlineColor = stackOutlineColorWidget.color;
            currentStackSettings.defaultColor = stackDefaultColorWidget.color;
            
            for (ThresholdItem item : stackThresholdItems) {
                item.syncToSettings();
            }
            currentStackSettings.sortThresholds();
            
            NConfig.set(NConfig.Key.stackQualityOverlay, currentStackSettings);
        }
        
        // Save Amount settings
        if (currentAmountSettings != null) {
            currentAmountSettings.backgroundColor = amountBackgroundColorWidget.color;
            currentAmountSettings.outlineColor = amountOutlineColorWidget.color;
            currentAmountSettings.defaultColor = amountColorWidget.color;
            
            NConfig.set(NConfig.Key.amountOverlay, currentAmountSettings);
        }
        
        // Save Study Info settings
        if (currentStudySettings != null) {
            currentStudySettings.backgroundColor = studyBackgroundColorWidget.color;
            currentStudySettings.outlineColor = studyOutlineColorWidget.color;
            currentStudySettings.defaultColor = studyColorWidget.color;
            
            NConfig.set(NConfig.Key.studyInfoOverlay, currentStudySettings);
        }
        
        // Save Progress settings
        if (currentProgressSettings != null) {
            currentProgressSettings.backgroundColor = progressBackgroundColorWidget.color;
            currentProgressSettings.outlineColor = progressOutlineColorWidget.color;
            currentProgressSettings.defaultColor = progressColorWidget.color;
            
            NConfig.set(NConfig.Key.progressOverlay, currentProgressSettings);
        }
        
        // Save Volume settings
        if (currentVolumeSettings != null) {
            currentVolumeSettings.backgroundColor = volumeBackgroundColorWidget.color;
            currentVolumeSettings.outlineColor = volumeOutlineColorWidget.color;
            currentVolumeSettings.defaultColor = volumeColorWidget.color;
            
            NConfig.set(NConfig.Key.volumeOverlay, currentVolumeSettings);
        }
        
        NConfig.needUpdate();
        Quality.invalidateCache();
        Stack.invalidateCache();
        haven.GItem.Amount.invalidateCache();
        nurgling.iteminfo.NCuriosity.invalidateCache();
        haven.res.ui.tt.drying.Drying.invalidateCache();
        haven.res.ui.tt.cn.CustomName.invalidateCache();
    }
    
    private void updatePreview() {
        if (preview != null) {
            // Sync Item Quality colors
            if (currentSettings != null) {
                currentSettings.contentColor = contentColorWidget.color;
                currentSettings.backgroundColor = backgroundColorWidget.color;
                currentSettings.outlineColor = outlineColorWidget.color;
                currentSettings.defaultColor = defaultColorWidget.color;
                for (ThresholdItem item : thresholdItems) {
                    item.syncToSettings();
                }
            }
            
            // Sync Stack Quality colors
            if (currentStackSettings != null) {
                currentStackSettings.backgroundColor = stackBackgroundColorWidget.color;
                currentStackSettings.outlineColor = stackOutlineColorWidget.color;
                currentStackSettings.defaultColor = stackDefaultColorWidget.color;
                for (ThresholdItem item : stackThresholdItems) {
                    item.syncToSettings();
                }
            }
            
            // Sync Amount colors
            if (currentAmountSettings != null) {
                currentAmountSettings.backgroundColor = amountBackgroundColorWidget.color;
                currentAmountSettings.outlineColor = amountOutlineColorWidget.color;
                currentAmountSettings.defaultColor = amountColorWidget.color;
            }
            
            // Sync Study Info colors
            if (currentStudySettings != null) {
                currentStudySettings.backgroundColor = studyBackgroundColorWidget.color;
                currentStudySettings.outlineColor = studyOutlineColorWidget.color;
                currentStudySettings.defaultColor = studyColorWidget.color;
            }
            
            // Sync Progress colors
            if (currentProgressSettings != null) {
                currentProgressSettings.backgroundColor = progressBackgroundColorWidget.color;
                currentProgressSettings.outlineColor = progressOutlineColorWidget.color;
                currentProgressSettings.defaultColor = progressColorWidget.color;
            }
            
            // Sync Volume colors
            if (currentVolumeSettings != null) {
                currentVolumeSettings.backgroundColor = volumeBackgroundColorWidget.color;
                currentVolumeSettings.outlineColor = volumeOutlineColorWidget.color;
                currentVolumeSettings.defaultColor = volumeColorWidget.color;
            }
            
            ItemQualityOverlaySettings previewSettings;
            if (activeTab == 0) previewSettings = currentSettings;
            else if (activeTab == 1) previewSettings = currentStackSettings;
            else if (activeTab == 2) previewSettings = currentAmountSettings;
            else if (activeTab == 3) previewSettings = currentStudySettings;
            else if (activeTab == 4) previewSettings = currentProgressSettings;
            else previewSettings = currentVolumeSettings;
            
            // Pass visibility flags from checkboxes
            boolean[] showFlags = new boolean[] {
                previewShowItemQ != null && previewShowItemQ.a,
                previewShowStackQ != null && previewShowStackQ.a,
                previewShowAmount != null && previewShowAmount.a,
                previewShowStudy != null && previewShowStudy.a,
                previewShowMeter != null && previewShowMeter.a,
                previewShowVol != null && previewShowVol.a
            };
            
            preview.updateSettings(previewSettings, activeTab, showFlags);
        }
    }
    
    @Override
    public void tick(double dt) {
        super.tick(dt);
        boolean needsUpdate = false;
        
        // Check Item Quality color changes
        if (currentSettings != null) {
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
            for (ThresholdItem item : thresholdItems) {
                if (item.hasColorChanged()) {
                    item.syncToSettings();
                    needsUpdate = true;
                }
            }
        }
        
        // Check Stack Quality color changes
        if (currentStackSettings != null) {
            if (!currentStackSettings.backgroundColor.equals(stackBackgroundColorWidget.color)) {
                currentStackSettings.backgroundColor = stackBackgroundColorWidget.color;
                needsUpdate = true;
            }
            if (!currentStackSettings.outlineColor.equals(stackOutlineColorWidget.color)) {
                currentStackSettings.outlineColor = stackOutlineColorWidget.color;
                needsUpdate = true;
            }
            if (!currentStackSettings.defaultColor.equals(stackDefaultColorWidget.color)) {
                currentStackSettings.defaultColor = stackDefaultColorWidget.color;
                needsUpdate = true;
            }
            for (ThresholdItem item : stackThresholdItems) {
                if (item.hasColorChanged()) {
                    item.syncToSettings();
                    needsUpdate = true;
                }
            }
        }
        
        // Check Amount color changes
        if (currentAmountSettings != null) {
            if (!currentAmountSettings.backgroundColor.equals(amountBackgroundColorWidget.color)) {
                currentAmountSettings.backgroundColor = amountBackgroundColorWidget.color;
                needsUpdate = true;
            }
            if (!currentAmountSettings.outlineColor.equals(amountOutlineColorWidget.color)) {
                currentAmountSettings.outlineColor = amountOutlineColorWidget.color;
                needsUpdate = true;
            }
            if (!currentAmountSettings.defaultColor.equals(amountColorWidget.color)) {
                currentAmountSettings.defaultColor = amountColorWidget.color;
                needsUpdate = true;
            }
        }
        
        // Check Study Info color changes
        if (currentStudySettings != null) {
            if (!currentStudySettings.backgroundColor.equals(studyBackgroundColorWidget.color)) {
                currentStudySettings.backgroundColor = studyBackgroundColorWidget.color;
                needsUpdate = true;
            }
            if (!currentStudySettings.outlineColor.equals(studyOutlineColorWidget.color)) {
                currentStudySettings.outlineColor = studyOutlineColorWidget.color;
                needsUpdate = true;
            }
            if (!currentStudySettings.defaultColor.equals(studyColorWidget.color)) {
                currentStudySettings.defaultColor = studyColorWidget.color;
                needsUpdate = true;
            }
        }
        
        // Check Progress color changes
        if (currentProgressSettings != null) {
            if (!currentProgressSettings.backgroundColor.equals(progressBackgroundColorWidget.color)) {
                currentProgressSettings.backgroundColor = progressBackgroundColorWidget.color;
                needsUpdate = true;
            }
            if (!currentProgressSettings.outlineColor.equals(progressOutlineColorWidget.color)) {
                currentProgressSettings.outlineColor = progressOutlineColorWidget.color;
                needsUpdate = true;
            }
            if (!currentProgressSettings.defaultColor.equals(progressColorWidget.color)) {
                currentProgressSettings.defaultColor = progressColorWidget.color;
                needsUpdate = true;
            }
        }
        
        // Check Volume color changes
        if (currentVolumeSettings != null) {
            if (!currentVolumeSettings.backgroundColor.equals(volumeBackgroundColorWidget.color)) {
                currentVolumeSettings.backgroundColor = volumeBackgroundColorWidget.color;
                needsUpdate = true;
            }
            if (!currentVolumeSettings.outlineColor.equals(volumeOutlineColorWidget.color)) {
                currentVolumeSettings.outlineColor = volumeOutlineColorWidget.color;
                needsUpdate = true;
            }
            if (!currentVolumeSettings.defaultColor.equals(volumeColorWidget.color)) {
                currentVolumeSettings.defaultColor = volumeColorWidget.color;
                needsUpdate = true;
            }
        }
        
        if (needsUpdate) {
            updatePreview();
        }
    }
    
    /**
     * Widget representing a single threshold item in the list
     */
    private class ThresholdItem extends Widget {
        private QualityThreshold threshold;
        private int index;
        private boolean isStack;
        private TextEntry valueEntry;
        private NColorWidget colorWidget;
        private Color lastColor;
        private static final int ITEM_HEIGHT = 32;
        
        public ThresholdItem(QualityThreshold threshold, int index, boolean isStack) {
            super(new Coord(UI.scale(180), UI.scale(ITEM_HEIGHT)));
            this.threshold = threshold;
            this.index = index;
            this.isStack = isStack;
            this.lastColor = threshold.color;
            
            int centerY = (UI.scale(ITEM_HEIGHT) - UI.scale(16)) / 2;
            int colorCenterY = (UI.scale(ITEM_HEIGHT) - Inventory.sqsz.y) / 2;
            int btnCenterY = (UI.scale(ITEM_HEIGHT) - NStyle.removei[0].sz().y) / 2;
            
            valueEntry = add(new TextEntry(UI.scale(40), String.valueOf(threshold.threshold)) {
                @Override
                public boolean keydown(KeyDownEvent ev) {
                    boolean result = super.keydown(ev);
                    try {
                        threshold.threshold = Integer.parseInt(text());
                        if (isStack) {
                            currentStackSettings.sortThresholds();
                        } else {
                            currentSettings.sortThresholds();
                        }
                        updatePreview();
                    } catch (NumberFormatException ignored) {}
                    return result;
                }
            }, new Coord(0, centerY));
            
            int colorX = UI.scale(45);
            colorWidget = add(new NColorWidget(""), new Coord(colorX, colorCenterY));
            colorWidget.label.hide();
            colorWidget.color = threshold.color;
            
            int removeX = colorX + colorWidget.sz.x + UI.scale(2);
            add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back) {
                @Override
                public void click() {
                    ItemQualityOverlaySettings settings = isStack ? currentStackSettings : currentSettings;
                    if (settings != null && settings.thresholds.size() > 1) {
                        settings.thresholds.remove(threshold);
                        rebuildThresholdsList(isStack);
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
        private boolean isStack;
        
        public ThresholdsList(Coord sz, boolean isStack) {
            super(sz, UI.scale(ThresholdItem.ITEM_HEIGHT + 2));
            this.isStack = isStack;
        }
        
        @Override
        protected List<ThresholdItem> items() {
            return isStack ? stackThresholdItems : thresholdItems;
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
        private int previewMode = 0; // 0 = item, 1 = stack, 2 = amount
        private BufferedImage itemIcon = null;
        
        // All overlay settings for multi-overlay preview
        private ItemQualityOverlaySettings itemQSettings;
        private ItemQualityOverlaySettings stackQSettings;
        private ItemQualityOverlaySettings amountSettings;
        private ItemQualityOverlaySettings studySettings;
        private ItemQualityOverlaySettings progressSettings;
        private ItemQualityOverlaySettings volumeSettings;
        
        // Visibility flags from checkboxes
        private boolean showItemQ = true;
        private boolean showStackQ = false;
        private boolean showAmount = true;
        private boolean showStudy = false;
        private boolean showMeter = false;
        private boolean showVol = false;
        
        public PreviewWidget() {
            super(UI.scale(new Coord(540, 100)));
            settings = new ItemQualityOverlaySettings();
            loadItemIcon();
            rebuildPreview();
        }
        
        private void loadItemIcon() {
            try {
                Resource res = Resource.remote().loadwait("gfx/invobjs/stoneaxe");
                if (res != null && res.layer(Resource.imgc) != null) {
                    itemIcon = res.layer(Resource.imgc).img;
                }
            } catch (Exception e) {
                // Failed to load, will use fallback
                itemIcon = null;
            }
        }
        
        public void updateSettings(ItemQualityOverlaySettings newSettings, int tabIndex, boolean[] showFlags) {
            this.settings = newSettings != null ? newSettings.copy() : new ItemQualityOverlaySettings();
            this.previewMode = tabIndex;
            
            // Store visibility flags
            if (showFlags != null && showFlags.length >= 6) {
                this.showItemQ = showFlags[0];
                this.showStackQ = showFlags[1];
                this.showAmount = showFlags[2];
                this.showStudy = showFlags[3];
                this.showMeter = showFlags[4];
                this.showVol = showFlags[5];
            }
            
            // Copy all current settings for multi-overlay preview
            this.itemQSettings = currentSettings != null ? currentSettings.copy() : null;
            this.stackQSettings = currentStackSettings != null ? currentStackSettings.copy() : null;
            this.amountSettings = currentAmountSettings != null ? currentAmountSettings.copy() : null;
            this.studySettings = currentStudySettings != null ? currentStudySettings.copy() : null;
            this.progressSettings = currentProgressSettings != null ? currentProgressSettings.copy() : null;
            this.volumeSettings = currentVolumeSettings != null ? currentVolumeSettings.copy() : null;
            
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
            int itemCount = 7;
            
            // Labels show values for the currently active tab
            String[] labels;
            if (previewMode == 5) {
                labels = new String[]{"0.50", "1.00", "2.50", "5.00", "10.00", "15.00", "25.00"};
            } else if (previewMode == 4) {
                labels = new String[]{"10%", "25%", "33%", "50%", "66%", "75%", "90%"};
            } else if (previewMode == 3) {
                labels = new String[]{"1h", "2h", "5h", "12h", "1d", "2d", "3d"};
            } else if (previewMode == 2) {
                labels = new String[]{"1", "5", "10", "25", "50", "99", "127"};
            } else {
                labels = new String[]{"10", "35", "55", "80", "120", "200", "Q"};
            }
            
            for (int i = 0; i < itemCount; i++) {
                int x = startX + i * spacing;
                int y = startY;
                
                // Draw inventory slot background
                g.setColor(new Color(30, 30, 35));
                g.fillRect(x, y, scaledItemSize, scaledItemSize);
                g.setColor(new Color(70, 70, 80));
                g.drawRect(x, y, scaledItemSize, scaledItemSize);
                
                // Draw item icon
                if (itemIcon != null) {
                    g.drawImage(itemIcon, x, y, scaledItemSize, scaledItemSize, null);
                } else {
                    // Fallback to colored rectangle
                    Color itemColor = new Color(80 + i * 20, 60 + i * 15, 50 + i * 10);
                    g.setColor(itemColor);
                    int pad = UI.scale(4);
                    g.fillRect(x + pad, y + pad, scaledItemSize - pad * 2, scaledItemSize - pad * 2);
                }
                
                // Draw overlays based on checkbox visibility
                // Sample values for each overlay type
                double qualityVal = 10 + i * 30;  // 10, 40, 70, 100, 130, 160, 190
                double amountVal = 1 + i * 3;     // 1, 4, 7, 10, 13, 16, 19
                double studyVal = 3600 * (i + 1); // 1h, 2h, 3h...
                double progressVal = 10 + i * 13; // 10%, 23%, 36%...
                double volumeVal = 0.5 + i * 0.7; // 0.5, 1.2, 1.9...
                
                // Draw Item Quality overlay (mode 0)
                if (showItemQ && itemQSettings != null) {
                    drawOverlayWithSettings(g, x, y, scaledItemSize, qualityVal, false, itemQSettings, 0);
                }
                
                // Draw Stack Quality overlay (mode 1)
                if (showStackQ && stackQSettings != null) {
                    drawOverlayWithSettings(g, x, y, scaledItemSize, qualityVal, false, stackQSettings, 1);
                }
                
                // Draw Amount overlay (mode 2)
                if (showAmount && amountSettings != null) {
                    drawOverlayWithSettings(g, x, y, scaledItemSize, amountVal, false, amountSettings, 2);
                }
                
                // Draw Study overlay (mode 3)
                if (showStudy && studySettings != null) {
                    drawOverlayWithSettings(g, x, y, scaledItemSize, studyVal, false, studySettings, 3);
                }
                
                // Draw Progress overlay (mode 4)
                if (showMeter && progressSettings != null) {
                    drawOverlayWithSettings(g, x, y, scaledItemSize, progressVal, false, progressSettings, 4);
                }
                
                // Draw Volume overlay (mode 5)
                if (showVol && volumeSettings != null) {
                    drawOverlayWithSettings(g, x, y, scaledItemSize, volumeVal, false, volumeSettings, 5);
                }
                
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
        
        
        private void drawOverlayWithSettings(Graphics2D g, int itemX, int itemY, int itemSize, 
                                              double value, boolean isContent, 
                                              ItemQualityOverlaySettings s, int mode) {
            FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
            Font font;
            if (fontSettings != null) {
                font = fontSettings.getFont(s.fontFamily);
                if (font == null) font = new Font("SansSerif", Font.BOLD, s.fontSize);
                else font = font.deriveFont(Font.BOLD, (float) s.fontSize);
            } else {
                font = new Font("SansSerif", Font.BOLD, s.fontSize);
            }
            
            String text;
            if (mode == 5) {
                // Volume mode - format as decimal
                text = String.format("%.2f", value);
            } else if (mode == 4) {
                // Progress mode - format as percentage
                text = (int) Math.round(value) + "%";
            } else if (mode == 3) {
                // Study info mode - format as time
                text = formatStudyTimeWithSettings((int) value, s);
            } else if (mode == 2) {
                // Amount mode
                text = s.showAmountPrefix ? ("×" + (int) Math.round(value)) : String.valueOf((int) Math.round(value));
            } else {
                // Quality mode (0 or 1)
                text = s.showDecimal ? String.format("%.1f", value) : String.valueOf((int) Math.round(value));
            }
            
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();
            
            int pad = UI.scale(1);
            int x, y;
            switch (s.corner) {
                case TOP_LEFT: x = itemX + pad; y = itemY + textHeight + pad; break;
                case TOP_RIGHT: x = itemX + itemSize - textWidth - pad; y = itemY + textHeight + pad; break;
                case BOTTOM_LEFT: x = itemX + pad; y = itemY + itemSize - pad; break;
                default: x = itemX + itemSize - textWidth - pad; y = itemY + itemSize - pad; break;
            }
            
            if (s.showBackground) {
                g.setColor(s.backgroundColor);
                g.fillRect(x - 1, y - textHeight, textWidth + 2, textHeight + 2);
            }
            
            Color textColor = isContent ? s.contentColor : s.getColorForQuality(value);
            
            if (s.showOutline) {
                g.setColor(s.outlineColor);
                int w = s.outlineWidth;
                for (int dx = -w; dx <= w; dx++) {
                    for (int dy = -w; dy <= w; dy++) {
                        if (dx != 0 || dy != 0) g.drawString(text, x + dx, y + dy);
                    }
                }
            }
            
            g.setColor(textColor);
            g.drawString(text, x, y);
        }
        
        private String formatStudyTimeWithSettings(int seconds, ItemQualityOverlaySettings s) {
            if (seconds <= 0) return "0s";
            
            TimeFormat format = s.timeFormat;
            switch (format) {
                case SECONDS:
                    return seconds + "s";
                case MINUTES:
                    return (seconds / 60) + "m";
                case HOURS:
                    return String.format("%.1fh", seconds / 3600.0);
                case DAYS:
                    return String.format("%.1fd", seconds / 86400.0);
                case AUTO:
                default:
                    int days = seconds / 86400;
                    int hours = (seconds % 86400) / 3600;
                    int mins = (seconds % 3600) / 60;
                    int secs = seconds % 60;
                    
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    if (days > 0 && count < 2) { sb.append(days).append("d"); count++; }
                    if (hours > 0 && count < 2) { sb.append(hours).append("h"); count++; }
                    if (mins > 0 && count < 2) { sb.append(mins).append("m"); count++; }
                    if (secs > 0 && count < 2) { sb.append(secs).append("s"); count++; }
                    return sb.length() > 0 ? sb.toString() : "0s";
            }
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
