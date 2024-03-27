package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

import static haven.OCache.posres;

public class OisterFounder implements Action {
    static NAlias animals = new NAlias(new ArrayList<String>(
            Arrays.asList("/boar", "/badger", "/wolverine", "/adder", "/bat", "/moose", "/bear", "/wolf", "/lynx", "/walrus")));

    public static boolean alarmAnimal() throws InterruptedException {
        if(!Finder.findGobs(animals, 275).isEmpty()){
            return true;
        }
        return false;
    }
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {


        ArrayList<Gob> oysters = Finder.findGobs(new NAlias("gfx/terobjs/herbs/oyster"), 275);
        gui.msg("Found " + oysters.size());

        for(Gob oysterr : oysters) {
            if(alarmAnimal()){
                continue;
            }
            PathFinder pf = new PathFinder(oysterr);
            pf.waterMode = true;
            pf.run(gui);
            Results res = new SelectFlowerAction("Pick", oysterr).run(gui);
            if(!res.IsSuccess()){
                WItem oysterToDrop = gui.getInventory().getItem(new NAlias("Oyster"));
                oysterToDrop.item.wdgmsg("drop", oysterToDrop.item.sz, gui.map.player().rc.floor(posres), 0);
                NUtils.getUI().core.addTask(new WaitItems(
                        NUtils.getGameUI().getInventory(),
                        new NAlias("Oyster"),
                        0));
            }

            NUtils.getUI().core.addTask(new WaitItems(
                    NUtils.getGameUI().getInventory(),
                    new NAlias("Oyster"),
                    1));
            WItem wi = gui.getInventory().getItem(new NAlias(new ArrayList(Arrays.asList("Oyster")),new ArrayList(Arrays.asList("Opened"))));
            if (wi != null) {
                new SelectFlowerAction("Crack open", wi).run(gui);
                NUtils.getUI().core.addTask(new WaitItemInInventory(new NAlias("Opened")));

                WItem witem = gui.getInventory().getItem(new NAlias("Opened"));
                witem.item.wdgmsg("drop", witem.item.sz, gui.map.player().rc.floor(posres), 0);
                NUtils.getUI().core.addTask(new WaitItems(
                        NUtils.getGameUI().getInventory(),
                        new NAlias("Opened"),
                        0));
            }else {
                return Results.ERROR("Did not find Cracked Oyster to drop.");
            }
        }
        return Results.SUCCESS();
    }
}