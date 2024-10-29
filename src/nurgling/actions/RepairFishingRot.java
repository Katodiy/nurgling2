package nurgling.actions;

import haven.*;
import nurgling.*;

import nurgling.conf.NFishingSettings;
import nurgling.tasks.NTask;
import nurgling.tools.Context;

public class RepairFishingRot implements Action {

    NFishingSettings prop;
    Pair<Coord2d, Coord2d> repArea;
    Context context;


    public RepairFishingRot(Context context, NFishingSettings prop, Pair<Coord2d, Coord2d> repArea) {
        this.prop = prop;
        this.repArea = repArea;
        this.context = context;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem rod = NUtils.getEquipment().findItem(prop.tool);
        if(rod == null) {
            return Results.ERROR("No fishing rod");
        }

        if(!((NGItem)rod.item).findContent(prop.fishline)) {
            Results repRes = repItem(gui, rod,  prop.fishline, 1);
            if (!repRes.IsSuccess()) return repRes;
        }

        if(!((NGItem)rod.item).findContent(prop.hook)) {
            Results repRes = repItem(gui, rod,  prop.hook, 1);
            if (!repRes.IsSuccess()) return repRes;
        }

        if(!((NGItem)rod.item).findContent(prop.bait)) {
            Results repRes = repItem(gui, rod,  prop.bait, prop.tool.endsWith("Primitive Casting-Rod")?1:5);
            if (!repRes.IsSuccess()) return repRes;
        }
        return Results.SUCCESS();
    }

    private Results repItem(NGameUI gui, WItem rod, String item, int count) throws InterruptedException {
        WItem fl = NUtils.getGameUI().getInventory().getItem(item);
        if(fl == null) {
            if(repArea!=null) {
                new TakeItems(context,item,count).run(gui);
            }
            fl = NUtils.getGameUI().getInventory().getItem(item);
        }
        if(fl == null) {
            return Results.ERROR("No " + item);
        }
        NUtils.takeItemToHand(fl);
        NWItem itemHand = (NWItem) NUtils.getGameUI().vhand;
        NUtils.itemact(rod);
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return ((NWItem) NUtils.getGameUI().vhand !=itemHand || ((NGItem)rod.item).findContent(item)) ;
            }
        });
        if((NWItem) NUtils.getGameUI().vhand!=null) {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().vhand.item.spr!=null;
                }
            });
            NUtils.dropToInv();
        }
        return Results.SUCCESS();
    }

}
