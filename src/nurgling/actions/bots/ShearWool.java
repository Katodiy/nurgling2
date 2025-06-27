package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.MenuSearch;
import nurgling.NFlowerMenu;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NFlowerMenuIsClosed;
import nurgling.tasks.WaitCollectState;
import nurgling.tasks.WaitPose;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

import static haven.OCache.posres;

public class ShearWool implements Action {
    NAlias type;
    Specialisation.SpecName spec;

    public ShearWool(Specialisation.SpecName spec, NAlias type) {
        this.type = type;
        this.spec = spec;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        ArrayList<Gob> gobs = Finder.findGobs(context.getSpecArea(spec), type);

        boolean needRestart = true;
        while (needRestart) {
            needRestart = false;
            String action = "Shear wool";
            for (Gob target : gobs) {
                if (NUtils.getGameUI().getInventory().getNumberFreeCoord(Coord.of(1, 1)) < 3) {
                    new FreeInventory2(context).run(gui);
                    gobs = Finder.findGobs(context.getSpecArea(spec), type);
                    needRestart = true;
                    break;
                }

                gui.map.wdgmsg("click", Coord.z, target.rc.floor(posres), 3, 0, 1, (int) target.id, target.rc.floor(posres),
                        0, -1);
                NFlowerMenu fm = NUtils.findFlowerMenu();
                if (fm != null) {
                    if (fm.hasOpt(action)) {
                        new DynamicPf(target).run(gui);
                        if (fm.chooseOpt(action)) {
                            NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                            NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/carving"));
                            WaitCollectState wcs = new WaitCollectState(target, Coord.of(1, 1));
                            NUtils.getUI().core.addTask(wcs);
                        } else {
                            NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                        }
                    } else {
                        fm.wdgmsg("cl", -1);
                        NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                    }
                }
            }
        }
        new FreeInventory2(context).run(gui);
        context.getSpecArea(spec);
        return Results.SUCCESS();
    }
}
