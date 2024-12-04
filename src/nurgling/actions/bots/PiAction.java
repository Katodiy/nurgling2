package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.FindNInventory;
import nurgling.tasks.WaitForBurnout;
import nurgling.tasks.WaitItems;
import nurgling.tools.*;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class PiAction implements Action {
    String cap = "Cauldron";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {


        Gob player = NUtils.player();
        Coord current_coord = NUtils.toGC(player.rc);

        NArea body = NArea.findSpec(Specialisation.SpecName.swamp.toString());

        for (Gob drowned : Finder.findGobs(body, new NAlias("gfx/borka/body"))) {
            if(drowned == null || NParser.checkName(drowned.pose(), "gfx/borka/drowned")){
                PathFinder pf = new PathFinder(drowned);
                pf.run(gui);
                new SelectFlowerAction("Take", drowned).run(gui);
                Thread.sleep(200);//TODO: WaitForWindow to spawn
                NEquipory drowned_equip = NUtils.getEquipmentOthers();

                gui.msg("found intentory?");
                WItem leech = drowned_equip.findItem("Bloated Leech");
                Coord target_coord = new Coord(1,1);

                while(leech != null){
                    if(!(gui.getInventory().getFreeSpace() > 0))
                        break;
                    int oldSpace = gui.getInventory().getItems("Bloated Leech").size();
                    gui.msg("Leeches in inventory: " + oldSpace);
                    leech.item.wdgmsg("transfer", Coord.z);
                    WaitItems wi = new WaitItems(gui.getInventory(), new NAlias("Bloated"), oldSpace + 1);
                    NUtils.getUI().core.addTask(wi);
                    leech = drowned_equip.findItem("Leech");
                }

            }
        }
        return Results.SUCCESS();
    }
}
