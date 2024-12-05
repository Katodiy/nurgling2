package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

import java.util.ArrayList;

public class TakeItemsFromPile implements Action
{
    NISBox pile;
    Gob gpile;
    Coord target_coord = new Coord(1,1);

    int target_size = Integer.MAX_VALUE;
    int took = 0;
    ArrayList<WItem> items = new ArrayList<>();

    public TakeItemsFromPile(Gob gob, NISBox pile, int target_size)
    {
        this.pile = pile;
        this.target_size = target_size;
        this.gpile = gob;
        if(gob.ngob.name.contains("block"))
        {
            target_coord = new Coord(1,2);
        }
        else if(gob.ngob.name.contains("board"))
        {
            target_coord = new Coord(4,1);
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        int count = Math.min(pile.calcCount(), Math.min(target_size,gui.getInventory().getNumberFreeCoord(target_coord)));
        while (gui.getInventory().getNumberFreeCoord(target_coord) > 0 && gui.getStockpile()!=null)
        {
            gui.getStockpile().transfer(count);
            WaitItemFromPile wifp = new WaitItemFromPile(gui.getInventory().getItems(), count);
            NUtils.getUI().core.addTask(wifp);
            took += wifp.getResult().size();
            items.addAll(wifp.getResult());
            if(target_size <=took)
                return Results.SUCCESS();
        }

        return Results.SUCCESS();
    }

    public int getResult()
    {
        return took;
    }

    public ArrayList<WItem> newItems(){
        return items;
    }
}
