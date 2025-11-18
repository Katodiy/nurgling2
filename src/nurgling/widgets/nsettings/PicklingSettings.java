package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;

public class PicklingSettings extends Panel {
    private CheckBox beetrootsCheckbox;
    private CheckBox carrotsCheckbox;
    private CheckBox eggsCheckbox;
    private CheckBox herringCheckbox;
    private CheckBox olivesCheckbox;
    private CheckBox cucumbersCheckbox;
    private CheckBox redOnionCheckbox;
    private CheckBox yellowOnionCheckbox;

    public PicklingSettings() {
        super("Pickling Settings");

        int margin = UI.scale(10);
        int y = UI.scale(36);

        // Add descriptive text
        add(new Label("Select which items you want to be processed when pickling bot runs:"), new Coord(margin, y));
        y += UI.scale(32);

        // Add checkboxes for each pickling item
        beetrootsCheckbox = add(new CheckBox("Beetroots") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        carrotsCheckbox = add(new CheckBox("Carrots") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        eggsCheckbox = add(new CheckBox("Eggs") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        herringCheckbox = add(new CheckBox("Herring") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        olivesCheckbox = add(new CheckBox("Olives") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        cucumbersCheckbox = add(new CheckBox("Cucumbers") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        redOnionCheckbox = add(new CheckBox("Red Onion") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += UI.scale(28);

        yellowOnionCheckbox = add(new CheckBox("Yellow Onion") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
    }

    @Override
    public void load() {
        // Load checkbox states from NConfig
        Boolean beetroots = (Boolean) NConfig.get(NConfig.Key.picklingBeetroots);
        beetrootsCheckbox.a = beetroots != null && beetroots;

        Boolean carrots = (Boolean) NConfig.get(NConfig.Key.picklingCarrots);
        carrotsCheckbox.a = carrots != null && carrots;

        Boolean eggs = (Boolean) NConfig.get(NConfig.Key.picklingEggs);
        eggsCheckbox.a = eggs != null && eggs;

        Boolean herring = (Boolean) NConfig.get(NConfig.Key.picklingHerring);
        herringCheckbox.a = herring != null && herring;

        Boolean olives = (Boolean) NConfig.get(NConfig.Key.picklingOlives);
        olivesCheckbox.a = olives != null && olives;

        Boolean cucumbers = (Boolean) NConfig.get(NConfig.Key.picklingCucumbers);
        cucumbersCheckbox.a = cucumbers != null && cucumbers;

        Boolean redOnion = (Boolean) NConfig.get(NConfig.Key.picklingRedOnion);
        redOnionCheckbox.a = redOnion != null && redOnion;

        Boolean yellowOnion = (Boolean) NConfig.get(NConfig.Key.picklingYellowOnion);
        yellowOnionCheckbox.a = yellowOnion != null && yellowOnion;
    }

    @Override
    public void save() {
        // Save checkbox states to NConfig
        NConfig.set(NConfig.Key.picklingBeetroots, beetrootsCheckbox.a);
        NConfig.set(NConfig.Key.picklingCarrots, carrotsCheckbox.a);
        NConfig.set(NConfig.Key.picklingEggs, eggsCheckbox.a);
        NConfig.set(NConfig.Key.picklingHerring, herringCheckbox.a);
        NConfig.set(NConfig.Key.picklingOlives, olivesCheckbox.a);
        NConfig.set(NConfig.Key.picklingCucumbers, cucumbersCheckbox.a);
        NConfig.set(NConfig.Key.picklingRedOnion, redOnionCheckbox.a);
        NConfig.set(NConfig.Key.picklingYellowOnion, yellowOnionCheckbox.a);
        NConfig.needUpdate();
    }
}