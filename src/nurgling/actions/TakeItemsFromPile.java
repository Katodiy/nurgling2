package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

import java.util.ArrayList;

public class TakeItemsFromPile implements Action
{
    NISBox pile;
    Gob gpile;
    int target_size = Integer.MAX_VALUE;
    int took = 0;
    ArrayList<NGItem> items = new ArrayList<>();

    public TakeItemsFromPile(Gob gob, NISBox pile, int target_size)
    {
        this.pile = pile;
        this.target_size = target_size;
        this.gpile = gob;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        int count = Math.min(pile.calcCount(), target_size);
        while (gui.getStockpile()!=null)
        {
            ((NUI)gui.ui).enableMonitor(gui.maininv);
            gui.getStockpile().transfer(count);
            WaitItemFromPile wifp = new WaitItemFromPile(count);
            NUtils.getUI().core.addTask(wifp);
            took += wifp.getResult().size();
            ((NUI)gui.ui).disableMonitor();
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

    public ArrayList<NGItem> newItems(){
        return items;
    }
}
