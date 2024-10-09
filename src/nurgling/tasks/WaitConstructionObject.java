package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import nurgling.tools.Finder;

public class WaitConstructionObject implements NTask {
    Coord2d position;

    public WaitConstructionObject(Coord2d position) {
        this.position = position;
    }

    @Override
    public boolean check() {
        return Finder.findGob(position)!=null;
    }
}
