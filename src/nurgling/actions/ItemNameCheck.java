package nurgling.actions;

import haven.GItem;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class ItemNameCheck implements Action {
    public Results run(NGameUI gui) throws InterruptedException {
        String targetName = "Yellow Onion";

        ArrayList<WItem> items = gui.getInventory().getWItems(new NAlias(targetName));

        for (WItem item : items) {
                NUtils.drop(item);
        }

        return Results.SUCCESS();
    }

}
