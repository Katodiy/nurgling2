package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

public class TakeItemsFromPile implements Action
{
    NISBox pile;
    Coord target_coord = new Coord(1,1);

    int target_size = Integer.MAX_VALUE;

    public TakeItemsFromPile(NISBox pile)
    {
        this.pile = pile;
    }

    public TakeItemsFromPile(NISBox pile, Coord target_coord)
    {
        this.pile = pile;
        this.target_coord = target_coord;
    }

    public TakeItemsFromPile(NISBox pile, int target_size)
    {
        this.pile = pile;
        this.target_size = target_size;
    }

    public TakeItemsFromPile(NISBox pile, Coord target_coord, int target_size)
    {
        this.pile = pile;
        this.target_coord = target_coord;
        this.target_size = target_size;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(target_size!=Integer.MAX_VALUE)
        {
            target_size = Math.min(target_size, gui.getInventory().getNumberFreeCoord(target_coord));

            int oldSpace = gui.getInventory().getItems().size();
            gui.getStockpile().transfer(target_size);
            NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(), oldSpace + target_size));
        }
        return Results.SUCCESS();
    }

    public int getResult()
    {
        return target_size;
    }
}
