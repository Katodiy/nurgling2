package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.conf.NChipperProp;
import nurgling.tasks.WaitChipperState;
import nurgling.tasks.WaitLifted;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitPoseOrNoGob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

public class BuildCellar implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if(!(new Equip(new NAlias("Pickaxe")).run(gui).IsSuccess()))
            return Results.ERROR("no Pickaxe!");
        Gob gob = Finder.findGob(new NAlias("cellar"));
        if(gob!=null) {
            Pair<Coord2d, Coord2d> area = new Pair<>(gob.rc.sub(MCache.tilehsz), gob.rc.add(MCache.tilehsz));
            while (Finder.findGob(gob.id) != null) {
                new PathFinder(gob.rc).run(gui);
                NUtils.rclickGob(gob);
                NUtils.getUI().core.addTask(new WaitPoseOrNoGob(NUtils.player(), gob, "gfx/borka/banzai"));
                FindPlaceAndAction fpa = new FindPlaceAndAction(null, area);
                fpa.run(gui);

                Gob bum = fpa.getPlaced();
                while (Finder.findGob(bum.id) != null) {
                    new SelectFlowerAction("Chip stone", bum).run(gui);
                    WaitChipperState wcs = new WaitChipperState(bum);
                    NUtils.getUI().core.addTask(wcs);
                    switch (wcs.getState()) {
                        case BUMLINGNOTFOUND:
                            break;
                        case BUMLINGFORDRINK: {
                            new Drink(0.9).run(gui);
                            break;
                        }
                        case DANGER: {
                            return Results.ERROR("SOMETHING WRONG, STOP WORKING");
                        }
                        case TIMEFORPILE: {
                            if(NUtils.getGameUI().vhand!=null) {
                                NUtils.drop(NUtils.getGameUI().vhand);
                            }
                            for (WItem item : NUtils.getGameUI().getInventory().getItems(Chipper.stones)) {
                                NUtils.drop(item);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            return Results.ERROR(" No cellar near! ");
        }
        return Results.SUCCESS();
    }
}
