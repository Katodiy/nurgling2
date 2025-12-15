package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.tasks.WaitPile;
import nurgling.tasks.WaitPlaced;
import nurgling.tasks.WaitPlob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.NGItem;
import haven.WItem;
import java.util.ArrayList;

import static haven.OCache.posres;

public class PileMaker implements Action{
    Pair<Coord2d, Coord2d> out;
    NAlias items;
    NAlias pileName;
    int th = 0;

    // When set, use exact name matching instead of NAlias substring matching
    String exactName = null;

    public Gob getPile() {
        return pile;
    }

    Gob pile = null;
    public PileMaker(Pair<Coord2d, Coord2d> out, NAlias items, NAlias pileName) {
        this.out = out;
        this.items = items;
        this.pileName = pileName;
    }

    public PileMaker(Pair<Coord2d, Coord2d> out, NAlias items, NAlias pileName, int th) {
        this.out = out;
        this.items = items;
        this.pileName = pileName;
        this.th = th;
    }

    public PileMaker(Pair<Coord2d, Coord2d> out, String exactName, NAlias pileName, int th) {
        this.out = out;
        this.exactName = exactName;
        this.items = new NAlias(exactName);
        this.pileName = pileName;
        this.th = th;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (gui.hand.isEmpty()) {
            ArrayList<WItem> witems = getMatchingItems(gui);
            if(witems.isEmpty() || NUtils.takeItemToHand(witems.get(0))==null)

                return Results.FAIL();
        }
        NUtils.activateItem(out.a);
        NUtils.getUI().core.addTask(new WaitPlob());
        Coord2d pos = null;
        NHitBox hitbox = NUtils.getGameUI().map.placing.get().ngob.hitBox;
        if((pos = Finder.getFreePlace(out,hitbox))==null)
            return Results.ERROR("No free space");

        new PathFinder( NGob.getDummy(pos, 0, hitbox),true).run(gui);
        NUtils.addTask(new WaitStockpile(false));
        NUtils.getGameUI().map.wdgmsg("place", pos.floor(posres), 0, 1, 0);
        WaitPile wp = new WaitPile(pos);
        NUtils.getUI().core.addTask(wp);
        pile = wp.getPile();
        NUtils.addTask(new WaitStockpile(true));
        return Results.SUCCESS();
    }

    /**
     * Gets items from inventory, using exact name match if exactName is set,
     * otherwise uses NAlias substring matching.
     */
    private ArrayList<WItem> getMatchingItems(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> allItems = NUtils.getGameUI().getInventory().getItems(items, th);
        if (exactName == null) {
            return allItems;
        }
        ArrayList<WItem> exactMatches = new ArrayList<>();
        for (WItem witem : allItems) {
            if (((NGItem) witem.item).name().equals(exactName)) {
                exactMatches.add(witem);
            }
        }
        return exactMatches;
    }
}
