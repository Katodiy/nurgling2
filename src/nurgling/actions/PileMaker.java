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

import static haven.OCache.posres;

public class PileMaker implements Action{
    Pair<Coord2d, Coord2d> out;
    NAlias items;
    NAlias pileName;

    public Gob getPile() {
        return pile;
    }

    Gob pile = null;
    public PileMaker(Pair<Coord2d, Coord2d> out, NAlias items, NAlias pileName) {
        this.out = out;
        this.items = items;
        this.pileName = pileName;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (gui.hand.isEmpty()) {
            if(NUtils.takeItemToHand(NUtils.getGameUI().getInventory().getItem(items))==null)
                
                return Results.FAIL();
        }
        NUtils.activateItem(NUtils.player().rc);
        NUtils.getUI().core.addTask(new WaitPlob());
        Coord2d pos = null;
        NHitBox hitbox = NUtils.getGameUI().map.placing.get().ngob.hitBox;
        if((pos = Finder.getFreePlace(out,hitbox))==null)
            return Results.ERROR("No free space");

        new PathFinder( NGob.getDummy(pos, 0, hitbox),true).run(gui);

        NUtils.getGameUI().map.wdgmsg("place", pos.floor(posres), 0, 1, 0);
        WaitPile wp = new WaitPile(pos);
        NUtils.getUI().core.addTask(wp);
        pile = wp.getPile();
        return Results.SUCCESS();
    }
}
