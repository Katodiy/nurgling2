package nurgling.tasks;

import haven.Coord2d;
import haven.Following;
import haven.Gob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class WaitPile extends NTask {
    Coord2d pos;

    public WaitPile(Coord2d pos) {
        this.pos = pos;
    }


    @Override
    public boolean check() {
        Gob gob = Finder.findGob(pos);
        if(gob!=null) {
            return NParser.checkName((pile = gob).ngob.name, new NAlias("stockpile"));
        }
        return false;
    }

    Gob pile = null;

    public Gob getPile() {
        return pile;
    }
}


