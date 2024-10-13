package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

public class TakeItemsFromPile implements Action
{
    NISBox pile;
    Gob gpile;
    Coord target_coord = new Coord(1,1);

    int target_size = Integer.MAX_VALUE;
    int took = 0;


    public TakeItemsFromPile(Gob gob, NISBox pile, int target_size)
    {
        this.pile = pile;
        this.target_size = target_size;
        this.gpile = gob;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        while (gui.getInventory().getNumberFreeCoord(target_coord) > 0 && gui.getStockpile()!=null)
        {
            gui.getStockpile().transfer(1);
            WaitItemFromPile wifp = new WaitItemFromPile(gui.getInventory().getItems());
            NUtils.getUI().core.addTask(wifp);
            took += wifp.getResult().size();
            if(target_size <=took)
                return Results.SUCCESS();
        }

        return Results.SUCCESS();
    }

    public int getResult()
    {
        return took;
    }
}
