package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectItemsToTrough implements Action{

    Pair<Coord2d,Coord2d> out;
    Pair<Coord2d,Coord2d> in;
    Coord2d playerc;
    NArea inA;
    NArea outA;

    NAlias items;
    public CollectItemsToTrough(Pair<Coord2d, Coord2d> input, Pair<Coord2d, Coord2d> output, NAlias items)
    {
        this.out = output;
        this.in = input;
        this.items = items;
    }

    public CollectItemsToTrough(Pair<Coord2d, Coord2d> input, Pair<Coord2d, Coord2d> output, NAlias items, Coord2d playerc)
    {
        this.out = output;
        this.in = input;
        this.items = items;
        this.playerc = playerc;
    }

    public CollectItemsToTrough(NArea input, NArea output, NAlias items)
    {
        this(input.getRCArea(), output.getRCArea(),items);
        this.inA = input;
        this.outA = output;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NAlias collected_items = new NAlias(items.keys, new ArrayList<>( Arrays.asList ( "stockpile" , "barrel") ));

        ArrayList<Gob> troughs = Finder.findGobs(outA, new NAlias(new ArrayList<>( Arrays.asList("trough")), new ArrayList<>( Arrays.asList ( "stockpile" , "barrel") )));

        HashMap<Gob, AtomicBoolean> troughInfo = new HashMap();
        for(Gob trough : troughs) {
            TransferToTrough ttt;
            (ttt = new TransferToTrough(trough, items)).run(gui);
            troughInfo.put(trough, new AtomicBoolean(ttt.isFull()));
        }

        while ( !Finder.findGobs (in,collected_items ).isEmpty () ){
            ArrayList<WItem> testItems = null;
            if(!(testItems = gui.getInventory ().getItems(items)).isEmpty()) {
                if (gui.getInventory().getNumberFreeCoord(testItems.get(0)) == 0) {
                    for(Gob trough: troughs){
                        new TransferToTrough(trough, items).run(gui);
                    }
                    //new TransferToPiles(out, items, playerc ).run(gui);
                }
            }

            Gob item = Finder.findGob ( in, collected_items );
            if(item == null)
                break;
            if(item.rc.dist(gui.map.player().rc)> MCache.tilesz2.x) {
                PathFinder pf = new PathFinder(item);
                pf.run(gui);
            }
            NUtils.takeFromEarth ( item );
            if (gui.getInventory().getFreeSpace() <= 2) {
                for(Gob trough : troughInfo.keySet()) {
                    if (!gui.getInventory().getItems(collected_items).isEmpty()) {
                        if (!troughInfo.get(trough).get()) {
                            TransferToTrough ttt;
                            (ttt = new TransferToTrough(trough, items)).run(gui);
                            troughInfo.put(trough, new AtomicBoolean(ttt.isFull()));
                        }
                    }
                }
                if (!gui.getInventory().getItems(collected_items).isEmpty()) {
                    return Results.ERROR("Troughs are full and there are leftovers");
                }
            }
        }
        /// /////
        if (!gui.getInventory().getItems(collected_items).isEmpty()) {
            for(Gob trough : troughInfo.keySet()) {
                if (!troughInfo.get(trough).get()) {
                    TransferToTrough ttt;
                    (ttt = new TransferToTrough(trough, items)).run(gui);
                    troughInfo.put(trough, new AtomicBoolean(ttt.isFull()));
                }
            }
        }
        return Results.SUCCESS();
    }
}
