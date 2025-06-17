package nurgling.actions.bots;

import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitMoreItems;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Collections;

public class Dropper implements Action {

    ArrayList<String> forSave;
    public Dropper(ArrayList<String> forSave) {
        this.forSave = forSave;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> oldItems = gui.getInventory().getItems();
        ArrayList<WItem> fordrop = new ArrayList<>();
        while(true)
        {
            NUtils.addTask(new WaitMoreItems( gui.getInventory(), oldItems.size() + 1));
            fordrop.clear();
            for(WItem item : gui.getInventory().getItems())
            {
                if(!oldItems.contains(item) && !forSave.contains(((NGItem)item.item).name())) {
                    NUtils.drop(item);
                    fordrop.add(item);
                }
            }
            NInventory inventory = NUtils.getGameUI().getInventory();
            NUtils.addTask(new NTask() {
                @Override
                public boolean check()
                {
                    if (checkContainer(inventory.child)) return false;
                    return true;
                }

                private boolean checkContainer(Widget first) {
                    for (Widget widget = first; widget != null; widget = widget.next)
                    {
                        if (widget instanceof WItem)
                        {
                            WItem item = (WItem) widget;
                            if (!NGItem.validateItem(item)) {
                                return true;
                            } else {
                                if (item.item.contents != null) {
                                    if(checkContainer(item.item.contents.child))
                                        return true;
                                }
                                else {
                                    if(fordrop.contains(item))
                                        return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            });
        }
    }
}
