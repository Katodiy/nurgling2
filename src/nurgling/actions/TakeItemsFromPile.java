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
        else
        {
            if(target_coord.x>1 || target_coord.y>1)
            {
                int count = 0;
                while (gui.getInventory().getNumberFreeCoord(target_coord) > 0 && gui.getStockpile()!=null)
                {
                    int oldSpace = gui.getInventory().getItems().size();
                    gui.getStockpile().transfer(1);
                    NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(), oldSpace + 1));
                    count++;
                }
                target_size = count;
            }
            else
            {
                target_size = Math.min(gui.getInventory().getFreeSpace(),gui.getStockpile().total());
                int oldSpace = gui.getInventory().getItems().size();
                gui.getStockpile().transfer(Math.min(gui.getInventory().getFreeSpace(),gui.getStockpile().total()));
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(), oldSpace + target_size));
            }
        }
        return Results.SUCCESS();
    }

    public int getResult()
    {
        return target_size;
    }
}
