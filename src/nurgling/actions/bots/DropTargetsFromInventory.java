package nurgling.actions.bots;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class DropTargetsFromInventory implements Action {


    NAlias target;

    public DropTargetsFromInventory(NAlias target) {
        this.target = target;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        for(WItem item : NUtils.getGameUI().getInventory().getItems(target))
        {
            NUtils.drop(item);
        }
        NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(),target,0));


        return Results.SUCCESS();
    }
}
