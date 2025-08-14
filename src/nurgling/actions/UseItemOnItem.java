package nurgling.actions;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitFreeHand;
import nurgling.tools.NAlias;

public class UseItemOnItem implements Action {
    private final NAlias from; // item in inv to pick up
    private final WItem to;    // WItem to use on

    /**
     * @param from NAlias or exact item name of what to pick up (from inventory)
     * @param to   The target WItem (from inventory or container)
     */
    public UseItemOnItem(NAlias from, WItem to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem src = gui.getInventory().getItem(from);
        if (src == null) {
            return Results.ERROR("No '" + from + "' in inventory");
        }
        if (to == null) {
            return Results.ERROR("Target item to use on is null");
        }

        NUtils.takeItemToHand(src);
        NUtils.itemact(to);
        NUtils.addTask(new WaitFreeHand());

        return Results.SUCCESS();
    }
}
