package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.*;
import nurgling.actions.SelectFlowerAction;
import nurgling.tasks.NTask;
import nurgling.tools.NAlias;
import nurgling.cheese.CheeseOrdersManager;
import nurgling.cheese.CheeseOrder;

/**
 * Handles cheese slicing and empty tray management
 */
public class CheeseSlicingManager {
    /**
     * Slice a cheese tray with order saving
     */
    public void sliceCheese(NGameUI gui, WItem tray, CheeseOrdersManager ordersManager) throws InterruptedException {
        if (tray == null) {
            gui.msg("Cannot slice: tray is null");
            return;
        }
        
        String cheeseType = CheeseUtils.getContentName(tray);
        if (cheeseType == null || cheeseType.isEmpty()) {
            gui.msg("Cannot slice: unable to determine cheese type");
            return;
        }
        
        // Check inventory space before slicing
        // Slicing typically produces 4-5 cheese pieces + 1 empty tray
        if (gui.getInventory().getFreeSpace() < CheeseConstants.SLICING_INVENTORY_REQUIREMENT) {
            return;
        }
        
        // Count items before slicing to detect changes
        int initialCheeseCount = getCheeseCount(gui, cheeseType);
        int initialEmptyTrayCount = getEmptyTrayCount(gui);
        
        // Right-click the tray to open flower menu and select "Slice up"
        new SelectFlowerAction("Slice up", tray).run(gui);
        
        // Wait for slicing to complete - check for cheese pieces OR empty tray to appear
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                try {
                    int currentCheeseCount = getCheeseCount(gui, cheeseType);
                    int currentEmptyTrayCount = getEmptyTrayCount(gui);
                    
                    // Success if we got more cheese pieces OR more empty trays
                    return currentCheeseCount > initialCheeseCount || currentEmptyTrayCount > initialEmptyTrayCount;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        });
        
        // Update and save orders after slicing
        if (ordersManager != null) {
            updateOrderAfterSlicing(ordersManager, cheeseType);
            ordersManager.writeOrders();
        }
    }
    
    /**
     * Get count of specific cheese type pieces in inventory
     */
    private int getCheeseCount(NGameUI gui, String cheeseType) throws InterruptedException {
        NAlias cheeseAlias = new NAlias(cheeseType);
        return gui.getInventory().getItems(cheeseAlias).size();
    }
    
    /**
     * Get count of empty cheese trays in inventory
     */
    private int getEmptyTrayCount(NGameUI gui) throws InterruptedException {
        return gui.getInventory().getItems(new NAlias(CheeseConstants.EMPTY_CHEESE_TRAY_NAME)).size();
    }
    
    /**
     * Update orders after slicing cheese - reduce count by 1
     */
    private void updateOrderAfterSlicing(CheeseOrdersManager ordersManager, String cheeseType) {
        for (CheeseOrder order : ordersManager.getOrders().values()) {
            if (order.getCheeseType().equals(cheeseType)) {
                for (CheeseOrder.StepStatus step : order.getStatus()) {
                    if (step.name.equals(cheeseType) && step.left > 0) {
                        step.left = Math.max(0, step.left - 1);
                        ordersManager.addOrUpdateOrder(order);
                        return;
                    }
                }
                break;
            }
        }
    }
}