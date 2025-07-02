package nurgling.actions;

import haven.Coord;
import haven.Inventory;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TakeAvailableItemsFromContainer implements Action
{
    Coord target_coord = new Coord(1,1);
    Container cont;
    NAlias name;
    int needed = 0;
    NInventory.QualityType qualityType = null;

    public TakeAvailableItemsFromContainer(Container cont, NAlias names, int needed)
    {
        this.cont = cont;
        this.name = names;
        this.needed = needed;
    }

    public TakeAvailableItemsFromContainer(Container cont, NAlias names, int needed, NInventory.QualityType qualityType)
    {
        this(cont, names, needed);
        this.qualityType = qualityType;
    }

    boolean took = false;
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NInventory inv = gui.getInventory(cont.cap);
        WItem item = inv.getItem(name);
        if (item != null) {
            target_coord = inv.getItem(name).sz.div(Inventory.sqsz);
            int oldSpace = gui.getInventory().getItems(name).size();
            ArrayList<WItem> items = gui.getInventory(cont.cap).getItems(name);

            if(qualityType == NInventory.QualityType.High) {
                items.sort((a, b) -> Float.compare(((NGItem)b.item).quality, ((NGItem)a.item).quality));
            } else if(qualityType == NInventory.QualityType.Low) {
                items.sort((a, b) -> Float.compare(((NGItem)a.item).quality, ((NGItem)b.item).quality));
            }

            int target_size = Math.min(needed,Math.min(gui.getInventory().getNumberFreeCoord(target_coord), items.size()));


            for (int i = 0; i < target_size; i++) {
                items.get(i).item.wdgmsg("transfer", Coord.z);
            }
            WaitItems wi = new WaitItems(gui.getInventory(), name, oldSpace + target_size);
            NUtils.getUI().core.addTask(wi);
            cont.update();
            count = target_size;
            if (items.size() > target_size) {
                took = false;
                return Results.FAIL();
            }
        }
        took = true;
        return Results.SUCCESS();
    }

    public boolean getResult()
    {
        return took;
    }

    public int getCount()
    {
        return count;
    }
    int count = 0;
}
