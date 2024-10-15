package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import haven.WItem;
import nurgling.*;
import nurgling.actions.bots.SelectArea;
import nurgling.areas.NArea;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitItemContent;
import nurgling.tasks.WaitPos;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class AutoEater implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<WItem> witems = NUtils.getGameUI().getInventory().getItems(NFoodInfo.class);

        /// Лучше все же не есть в движении
        Gob pl = NUtils.player();
        NUtils.clickGob(pl);
        NUtils.getUI().core.addTask(new WaitPose(pl,"gfx/borka/idle"));

        if(witems.isEmpty())
            return Results.ERROR("no food left");
        while (!witems.isEmpty()) {
            double cEnrj = NUtils.getEnergy();
            NFoodInfo fi = ((NGItem) witems.get(0).item).getInfo(NFoodInfo.class);
            if (cEnrj + fi.energy()/100 < 0.81) {
                new SelectFlowerAction("Eat", witems.get(0)).run(gui);
            }
            else
            {
                break;
            }
            witems = NUtils.getGameUI().getInventory().getItems(NFoodInfo.class);
        }

        return Results.SUCCESS();
    }
}
