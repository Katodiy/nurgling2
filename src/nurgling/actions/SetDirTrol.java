package nurgling.actions;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitDir;

import static haven.OCache.posres;

public class SetDirTrol implements Action{
    Coord2d dir;

    public SetDirTrol(Coord2d dir) {
        this.dir = dir;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        dir = dir.norm().div(10);
        Gob pl = NUtils.player();
        gui.map.wdgmsg("click", Coord.z, pl.rc.add(dir).floor(posres), 1, 0);
        NUtils.getUI().core.addTask(new WaitDir.WaitDirTrol(pl,pl.a));
        return Results.SUCCESS();
    }
}
