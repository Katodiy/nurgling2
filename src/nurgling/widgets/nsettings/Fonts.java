package nurgling.widgets.nsettings;

import haven.*;
import haven.Label;
import nurgling.NConfig;
import nurgling.conf.FontSettings;
import nurgling.widgets.NColorWidget;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Fonts extends Panel {
    private static final List<String> FONT_FAMILIES = Arrays.asList(
            "Inter", "Roboto", "Sans", "Serif", "Fractur"
    );

    public enum FontType {
        DEFAULT("Default font"),
        UI("Interface font"),
        QUESTS("Quests list"),
        BARRELS("Barrels labels"),
        CHARACTERS("Characters names");

        final String displayName;
        FontType(String displayName) {
            this.displayName = displayName;
        }
    }

    private Dropbox<FontType> fontTypeSelector;
    private Dropbox<String> familySelector;
    private HSlider sizeSlider;
    private Label sizeLabel;
    private PreviewLabel preview;
    private FontSettings currentSettings;
    private NColorWidget colorButton;
    private boolean colorable = false;

    public Fonts() {
        super("Fonts");

        // Главный комбобокс для выбора типа текста
        add(new Label("Text type:"), UI.scale(10, 40));
        fontTypeSelector = add(new Dropbox<FontType>(UI.scale(200), FontType.values().length, UI.scale(16)) {
            @Override
            protected FontType listitem(int i) { return FontType.values()[i]; }
            @Override
            protected int listitems() { return FontType.values().length; }
            @Override
            protected void drawitem(GOut g, FontType item, int i) {
                g.text(item.displayName, Coord.z);
            }
            @Override
            public void change(FontType item) {
                super.change(item);
                updateControls();
            }
        }, UI.scale(100, 40));

        // Выбор семейства шрифтов
        add(new Label("Font family:"), UI.scale(10, 80));
        familySelector = add(new Dropbox<String>(UI.scale(200), FONT_FAMILIES.size(), UI.scale(16)) {
            @Override protected String listitem(int i) { return FONT_FAMILIES.get(i); }
            @Override protected int listitems() { return FONT_FAMILIES.size(); }
            @Override protected void drawitem(GOut g, String item, int i) { g.text(item, Coord.z); }
            @Override public void change(String item) {
                super.change(item);
                updatePreview();
            }
        }, UI.scale(100, 80));
        colorButton = add(new NColorWidget("Color"), familySelector.pos("ur").add(UI.scale(20,-8)));
        colorButton.hide();

        // Размер шрифта
        add(new Label("Font size:"), UI.scale(10, 120));
        sizeLabel = new Label("12");
        addhlp(UI.scale(100, 122), UI.scale(5),
                sizeSlider = new HSlider(UI.scale(200), 8, 24, 12) {
                    protected void added() { updateSize(); }
                    public void changed() {
                        updateSize();
                        updatePreview();
                    }
                    private void updateSize() {
                        sizeLabel.settext(Integer.toString(val));
                    }
                }, sizeLabel);

        // Область предпросмотра
        preview = add(new PreviewLabel(), UI.scale(10, 160));

        // Загружаем настройки
        load();
        fontTypeSelector.change(FontType.DEFAULT);
    }

    private void updateControls() {
        FontType type = fontTypeSelector.sel;
        if (type == null || currentSettings == null) return;

        FontSettings.FontConfig config;
        switch (type) {
            case UI: config = currentSettings.uiFont; break;
            case QUESTS: config = currentSettings.questsFont; break;
            case BARRELS: config = currentSettings.barrelsFont; break;
            case CHARACTERS: config = currentSettings.charactersFont; break;
            default: config = currentSettings.defaultFont;
        }

        familySelector.sel = config.family;
        sizeSlider.val = config.size;
        sizeSlider.changed();
        colorable = config.isColorable;
        if(config.isColorable)
        {
            colorButton.show();
            colorButton.color = config.color;
        }
        else
        {
            colorButton.hide();
        }
        updatePreview();
    }

    private void updatePreview() {
        if (familySelector.sel != null && sizeSlider != null) {
            preview.updateFont(familySelector.sel, sizeSlider.val);
        }
    }

    @Override
    public void load() {
        currentSettings = (FontSettings)NConfig.get(NConfig.Key.fonts);
        if (currentSettings == null) {
            currentSettings = new FontSettings();
        }
        updateControls();
    }

    @Override
    public void save() {
        FontType type = fontTypeSelector.sel;
        if (type == null || currentSettings == null) return;

        FontSettings.FontConfig config = new FontSettings.FontConfig(
                familySelector.sel,
                sizeSlider.val,
                colorable,
                !colorable ? Color.BLACK : colorButton.color
        );

        switch (type) {
            case UI:
                currentSettings.uiFont = config;
                break;
            case QUESTS:
                currentSettings.questsFont = config;
                break;
            case BARRELS:
                currentSettings.barrelsFont = config;
                break;
            case CHARACTERS:
                currentSettings.charactersFont = config;
                break;
            default:
                currentSettings.defaultFont = config;
        }

        NConfig.set(NConfig.Key.fonts, currentSettings);
    }

    private class PreviewLabel extends Widget {
        private Text previewText;

        public PreviewLabel() {
            super(UI.scale(300, 50));
            updateFont("Sans", 12);
        }

        public void updateFont(String family, int size) {
            if(family!=null && currentSettings!=null)
                previewText = new Text.Foundry(currentSettings.getFont(family),size).render("Sample text: 123 ABC abc");
        }

        public void draw(GOut g) {
            if(previewText!=null)
                g.image(previewText.tex(), Coord.z);
            super.draw(g);
        }
    }
}