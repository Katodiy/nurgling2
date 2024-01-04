package nurgling.actions;

import haven.*;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.FilledPile;
import nurgling.tasks.NotThisInHand;
import nurgling.tasks.WaitItems;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class TransferToPiles implements Action{

    NAlias items;

    Pair<Coord2d,Coord2d> out;

    public TransferToPiles(Pair<Coord2d,Coord2d> out, NAlias items) {
        this.out = out;
        this.items = items;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> witems;
        NAlias pileName;
        if (!(witems = gui.getInventory().getItems(items)).isEmpty() ) {
                Gob target = null;
                for (Gob gob : Finder.findGobs(out, pileName = getStockpileName(items))) {
                    if (gob.ngob.getModelAttribute() != 31) {
                        if(PathFinder.isAvailable(gob)) {
                            target = gob;
                            new PathFinder(target).run(gui);
                            if (gui.hand.isEmpty()) {
                                NUtils.takeItemToHand(witems.get(0));
                            }
                            NUtils.dropsame(gob);
                            NUtils.getUI().core.addTask(new FilledPile(gob, items));

                            if((witems = gui.getInventory().getItems(items)).isEmpty())
                                return Results.SUCCESS();
                        }
                    }
                }

                while(!(gui.getInventory().getItems(items)).isEmpty()) {
                    PileMaker pm;
                    (pm = new PileMaker(out, items, pileName)).run(gui);
                    witems = gui.getInventory().getItems(items);
                    if (!gui.hand.isEmpty()) {
                        NUtils.takeItemToHand(witems.get(0));
                    }
                    Gob pile = pm.getPile();
                    NUtils.dropsame(pile);
                    NUtils.getUI().core.addTask(new FilledPile(pile, items));
                }
            }
        return Results.SUCCESS();
        }



    NAlias getStockpileName(NAlias items) {
        if (NParser.checkName(items.getDefault(), new NAlias("soil"))) {
            return new NAlias("gfx/terobjs/stockpile-soil");
        } else if (NParser.checkName(items.getDefault(), new NAlias("board"))) {
            return new NAlias("gfx/terobjs/stockpile-board");
        } else if (NParser.checkName(items.getDefault(), new NAlias("pumpkin"))) {
            return new NAlias("gfx/terobjs/stockpile-pumpkin");
        } else if (NParser.checkName(items.getDefault(), new NAlias("metal"))) {
            return new NAlias("gfx/terobjs/stockpile-metal");
        } else if (NParser.checkName(items.getDefault(), new NAlias("brick"))) {
            return new NAlias("gfx/terobjs/stockpile-brick");
        } else if (NParser.checkName(items.getDefault(), new NAlias("leaf"))) {
            return new NAlias("gfx/terobjs/stockpile-leaf");
        }
        else
            return new NAlias("stockpile");
    }
}
