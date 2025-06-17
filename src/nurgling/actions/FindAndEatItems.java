package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FindAndEatItems implements Action
{
    final Context cnt;
    ArrayList<String> items;
    double level;
    Pair<Coord2d,Coord2d> area;
    public FindAndEatItems(Context context, ArrayList<String> items, int level, Pair<Coord2d,Coord2d> area)
    {
        this.cnt = context;
        this.items = items;
        this.level = level;
        this.area = area;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        for(String item: items)
        {
            ArrayList<Context.Input> inputs = cnt.getInputs(item);
            if(inputs == null)
            {
                cnt.addInput(item,Context.GetInput(area));
            }
        }

        for(String item: items) {
            for (Context.Input input : cnt.getInputs(item)) {
                if (input instanceof Context.InputPile) {
                    takeFromPile(gui, (Context.InputPile) input);
                } else if (input instanceof Context.InputContainer) {
                    takeFromContainer(gui, (Context.InputContainer) input);
                }
                if(!calcCalories())
                    break;
            }
            if(!calcCalories())
                break;
        }
        return Results.SUCCESS();
    }

    public Results takeFromPile(NGameUI gui, Context.InputPile pile) throws InterruptedException
    {
        new PathFinder(pile.pile).run(gui);
        new OpenTargetContainer("Stockpile",  pile.pile).run(gui);
        while (calcCalories()) {
            if(gui.getInventory().getNumberFreeCoord(new Coord(1,1))==0)
            {
                eatAll(gui);
            }
            TakeItemsFromPile tifp;
            (tifp = new TakeItemsFromPile(pile.pile, gui.getStockpile(), 1)).run(gui);
            if(tifp.getResult() == 0)
                break;
        }
        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
        return Results.SUCCESS();
    }

    public Results takeFromContainer(NGameUI gui, Container cont) throws InterruptedException
    {
        new PathFinder(cont.gob).run(gui);
        new OpenTargetContainer(cont).run(gui);
        while (calcCalories()) {
            if(gui.getInventory().getNumberFreeCoord(new Coord(1,1))==0)
            {
                eatAll(gui);
            }
            WItem taritem = NUtils.getGameUI().getInventory(cont.cap).getItem(new NAlias(items));
            int oldSize = NUtils.getGameUI().getInventory().getItems(new NAlias(items)).size();
            taritem.item.wdgmsg("transfer", Coord.z);
            gui.ui.core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(items), oldSize + 1));
        }

        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
        return Results.SUCCESS();
    }

    boolean calcCalories() throws InterruptedException {
        double curlvl = NUtils.getEnergy()*10000;
        ArrayList<WItem> taritems = NUtils.getGameUI().getInventory().getItems(new NAlias(items));
        for(WItem item: taritems)
        {
            NFoodInfo fi = ((NGItem)item.item).getInfo(NFoodInfo.class);
            curlvl+=fi.end*100;
        }
        return curlvl<level;
    }

    void eatAll(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> titems = NUtils.getGameUI().getInventory().getItems(new NAlias(items));

        for (WItem item : titems)
        {
            new SelectFlowerAction("Eat", (NWItem) item).run(gui);
        }
    }
}
