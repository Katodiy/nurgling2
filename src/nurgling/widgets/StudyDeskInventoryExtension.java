package nurgling.widgets;

import haven.*;
import nurgling.*;

/**
 * Extension for adding Study Desk specific functionality to inventory windows
 */
public class StudyDeskInventoryExtension {

    /**
     * Adds a Plan button to inventory windows that belong to Study Desk containers
     * @param inventory The inventory to potentially extend
     */
    public static void addPlanButtonIfStudyDesk(NInventory inventory) {
        if (inventory != null && isStudyDeskInventory(inventory)) {
            addPlanButton(inventory);
        }
    }

    /**
     * Checks if the given inventory belongs to a Study Desk container
     * @param inventory The inventory to check
     * @return true if this is a Study Desk inventory
     */
    private static boolean isStudyDeskInventory(NInventory inventory) {
        if (inventory.parentGob == null) return false;
        // Get the drawable attribute from the gob
        Drawable drawable = inventory.parentGob.getattr(Drawable.class);
        if (drawable != null && drawable.getres() != null) {
            String resName = drawable.getres().name;
            return "gfx/terobjs/studydesk".equals(resName);
        }
        return false;
    }

    /**
     * Adds the Plan button to the inventory window
     * @param inventory The inventory to add the button to
     */
    private static void addPlanButton(NInventory inventory) {
        if (inventory.parent == null) return;

        Button planButton = new Button(UI.scale(50), "Plan") {
            @Override
            public void click() {
                openStudyDeskPlanner();
            }
        };

        // Position the button below the inventory grid, centered
        inventory.parent.add(planButton, new Coord(
            inventory.sz.x / 2 - UI.scale(25),
            inventory.sz.y + UI.scale(5)
        ));
    }

    /**
     * Opens the Study Desk Planner widget
     */
    private static void openStudyDeskPlanner() {
        NGameUI gameUI = NUtils.getGameUI();
        if (gameUI != null) {
            if (gameUI.studyDeskPlanner == null) {
                gameUI.studyDeskPlanner = new StudyDeskPlannerWidget();
                gameUI.add(gameUI.studyDeskPlanner, new Coord(200, 100));
            }
            if (gameUI.studyDeskPlanner.visible()) {
                gameUI.studyDeskPlanner.hide();
            } else {
                gameUI.studyDeskPlanner.show();
            }
        }
    }
}