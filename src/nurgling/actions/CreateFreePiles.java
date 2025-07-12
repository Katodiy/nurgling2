package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.tasks.WaitPile;
import nurgling.tasks.WaitPlob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import static haven.OCache.posres;

public class CreateFreePiles implements Action{
    Pair<Coord2d, Coord2d> out;
    NAlias items;
    NAlias pileName;

    public Gob getPile() {
        return pile;
    }

    Gob pile = null;
    public CreateFreePiles(Pair<Coord2d, Coord2d> out, NAlias items, NAlias pileName) {
        this.out = out;
        this.items = items;
        this.pileName = pileName;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while(new PileMaker(out,items,pileName).run(gui).IsSuccess());
        return Results.SUCCESS();
    }
}
