package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class ShearWool implements Action {
    NAlias type;
    String spec;

    public ShearWool(String spec, NAlias type) {
        this.type = type;
        this.spec = spec;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> gobs = Finder.findGobs(NContext.findSpec(spec), type);
        Context context = new Context();
        boolean needRestart = true;
        while (needRestart) {
            needRestart = false;
            for (Gob gob : gobs) {
                if (NUtils.getGameUI().getInventory().getNumberFreeCoord(Coord.of(1, 1)) < 3) {
                    new FreeInventory(context).run(gui);
                    gobs = Finder.findGobs(NContext.findSpec(spec), type);
                    needRestart = true;
                    break;
                }
                new DynamicPf(gob).run(gui);
                new CollectFromGob(gob, "Shear wool", "gfx/borka/carving", Coord.of(1, 1), new NAlias("Wool"), true).run(gui);
            }
        }
        new FreeInventory(context).run(gui);
        return null;
    }
}
