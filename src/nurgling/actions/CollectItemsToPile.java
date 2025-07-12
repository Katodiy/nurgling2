package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

public class CollectItemsToPile implements Action{

    Pair<Coord2d,Coord2d> out;
    Pair<Coord2d,Coord2d> in;
    Coord2d playerc;

    NAlias items;
    public CollectItemsToPile(Pair<Coord2d, Coord2d> input, Pair<Coord2d, Coord2d> output, NAlias items)
    {
        this.out = output;
        this.in = input;
        this.items = items;
    }

    public CollectItemsToPile(Pair<Coord2d, Coord2d> input, Pair<Coord2d, Coord2d> output, NAlias items, Coord2d playerc)
    {
        this.out = output;
        this.in = input;
        this.items = items;
        this.playerc = playerc;
    }

    CollectItemsToPile(NArea input, NArea output, NAlias items)
    {
        this(input.getRCArea(), output.getRCArea(),items);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NAlias collected_items = new NAlias(items.keys, new ArrayList<>( Arrays.asList ( "stockpile" , "barrel") ));

        while ( !Finder.findGobs (in,collected_items ).isEmpty () ){
            ArrayList<WItem> testItems = null;
            if(!(testItems = gui.getInventory ().getItems(items)).isEmpty()) {
                if (gui.getInventory().getNumberFreeCoord(testItems.get(0)) == 0) {
                    new TransferToPiles(out, items, playerc ).run(gui);
                }
            }

            Gob item = Finder.findGob (in, collected_items );
            if(item == null)
                break;
            if(item.rc.dist(gui.map.player().rc)> MCache.tilesz2.x) {
                PathFinder pf = new PathFinder(item);
                pf.run(gui);
            }
            NUtils.takeFromEarth ( item );
        }

        new TransferToPiles(out, items, playerc ).run ( gui );
        return Results.SUCCESS();
    }
}
