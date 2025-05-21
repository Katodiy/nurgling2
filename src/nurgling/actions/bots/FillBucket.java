package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class FillBucket implements Action {

    public FillBucket() {
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        WItem bucket = NUtils.getEquipment().findBucket("Empty");
        NArea.Specialisation barrels_s = new NArea.Specialisation(Specialisation.SpecName.barrel.toString(), "Picklebrine");
        if(barrels_s == null)
            return Results.ERROR("Barrels spec not found");
        NArea area = NArea.findSpec(barrels_s);
        if (bucket != null && area != null) {
            ArrayList<Gob> all_barrels = Finder.findGobs(area, new NAlias("barrel"));
            ArrayList<Gob> brine_barrels = new ArrayList<>();
            for (Gob gob : all_barrels) {
                if (NUtils.barrelHasContent(gob) && NUtils.isOverlay(gob, new NAlias("picklebrine"))) {
                    brine_barrels.add(gob);
                }
            }
            if(brine_barrels.isEmpty()) return Results.ERROR("No barrels with brine in `barrel` specialized area.");

            if (NUtils.getGameUI().vhand == null) {
                NUtils.takeItemToHand(bucket);
                NUtils.getUI().core.addTask(new HandNotFree());
            }

            if (NUtils.getGameUI().vhand != null) {
                if(((NGItem)bucket.item).content().isEmpty()){
                    Gob bbr = brine_barrels.getFirst();
                    new PathFinder(bbr).run(gui);
                    NUtils.activateItem(bbr, true);
                    NUtils.getUI().core.addTask(new HandWithContent());
                    NUtils.getEquipment().wdgmsg("drop", -1);
                    NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
                }
            }

            WItem brinebucket = NUtils.getEquipment().findBucket("Brine");
            if(brinebucket != null){
                NGItem bb = ((NGItem)brinebucket.item);
                ArrayList<NGItem.NContent> c = bb.content();
                if(!c.isEmpty()){
                    gui.msg("bucket:" + c.getFirst() + "[" + NUtils.parseStartDouble(c.getFirst().name()) +"]");
                }
            } else {gui.msg("brine bucket is null");}
        }

        return Results.SUCCESS();
    }
}
