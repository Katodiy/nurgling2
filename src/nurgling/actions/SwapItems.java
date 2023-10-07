package nurgling.actions;

import haven.*;
import static haven.Inventory.sqsz;
import nurgling.*;

public class SwapItems implements Action
{
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        String name = "Plate Greaves";
        while(true)
        {
            Coord pos;
            WItem item = gui.getInventory().getItem(name);
            if (item == null)
            {
                gui.tickmsg("error item " + name + "notfound");
                return Results.ERROR("error item " + name + "notfound");
            }
            pos = item.c.div(Inventory.sqsz);
            NUtils.takeItemToHand(item);
            gui.getInventory().dropOn(pos, name);
        }
        //return Results.SUCCESS();
    }
}
