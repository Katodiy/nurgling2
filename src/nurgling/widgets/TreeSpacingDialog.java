package nurgling.widgets;

import haven.*;
import nurgling.actions.bots.PlantTrees;
import nurgling.i18n.L10n;

/**
 * Dialog for configuring tree spacing options during tree planting
 *
 * Provides radio button selection for different spacing intervals:
 * - Every 2 tiles (default)
 * - Every 3 tiles
 * - Every 4 tiles
 * - Every 5 tiles
 *
 * Updates preview in real-time as user changes spacing options.
 */
public class TreeSpacingDialog extends Window {

    private final PlantTrees parentBot;
    private final int[] spacingOptions = {2, 3, 4, 5};
    private final String[] spacingLabels = {
        "Every other tile (2)",
        "Every 3rd tile (3)",
        "Every 4th tile (4)",
        "Every 5th tile (5)"
    };

    private NRadioButton[] spacingButtons;
    private int selectedSpacing = 2; // Default spacing
    private Label countLabel;

    public TreeSpacingDialog(PlantTrees parentBot) {
        super(UI.scale(new Coord(280, 200)), L10n.get("tree.spacing_title"));
        this.parentBot = parentBot;

        createUI();
        updateCountLabel();
    }

    /**
     * Creates the dialog UI components
     */
    private void createUI() {
        // Title label
        add(new Label("Select tree spacing:"), UI.scale(10, 10));

        // Radio button group for spacing options
        spacingButtons = new NRadioButton[spacingOptions.length];
        int startY = 35;

        for (int i = 0; i < spacingOptions.length; i++) {
            final int spacing = spacingOptions[i];
            final int index = i;

            spacingButtons[i] = new NRadioButton(spacingLabels[i]) {
                @Override
                public void changed(boolean val) {
                    if (val) {
                        // Uncheck other buttons
                        for (int j = 0; j < spacingButtons.length; j++) {
                            if (j != index && spacingButtons[j] != null) {
                                spacingButtons[j].a = false;
                            }
                        }

                        // Update spacing
                        selectedSpacing = spacing;
                        parentBot.updateSpacing(spacing);
                        updateCountLabel();
                    }
                }
            };

            // Set default selection
            if (spacing == 2) {
                spacingButtons[i].a = true;
            }

            add(spacingButtons[i], UI.scale(20, startY + (i * 25)));
        }

        // Tree count label
        countLabel = new Label("Trees to plant: 0");
        add(countLabel, UI.scale(20, startY + (spacingOptions.length * 25) + 10));

        // Buttons
        int buttonY = startY + (spacingOptions.length * 25) + 40;

        // Confirm button
        add(new Button(UI.scale(60), L10n.get("common.confirm")) {
            @Override
            public void click() {
                parentBot.confirmPlacement();
            }
        }, UI.scale(50, buttonY));

        // Cancel button
        add(new Button(UI.scale(60), L10n.get("common.cancel")) {
            @Override
            public void click() {
                parentBot.cancelPlacement();
            }
        }, UI.scale(130, buttonY));
    }

    /**
     * Updates the tree count label based on current spacing
     */
    private void updateCountLabel() {
        try {
            int count = parentBot.getPlantingCount();
            countLabel.settext("Trees to plant: " + count);
        } catch (Exception e) {
            countLabel.settext("Trees to plant: Error");
        }
    }

    /**
     * Custom radio button implementation
     */
    private static class NRadioButton extends CheckBox {
        public NRadioButton(String text) {
            super(text);
        }

        @Override
        public void click() {
            a = !a;
            changed(a);
        }
    }

    @Override
    public void destroy() {
        try {
            super.destroy();
        } catch (Exception e) {
            // Silent cleanup
        }
    }
}