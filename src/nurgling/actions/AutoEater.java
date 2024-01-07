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
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class AutoEater implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<WItem> witems = NUtils.getGameUI().getInventory().getItems(NFoodInfo.class);

        if(!witems.isEmpty()) {
            double cEnrj = NUtils.getEnergy();
            for (WItem item : witems) {
                NFoodInfo fi = ((NGItem) item.item).getInfo(NFoodInfo.class);
                if (cEnrj + fi.energy() < 8100) {
                    new SelectFlowerAction("Eat", (NWItem) item).run(gui);
                }
            }
        }
        return Results.SUCCESS();
    }
}
