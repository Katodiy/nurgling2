package nurgling.actions.bots;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.*;

import java.util.ArrayList;

public class FillJarWithJuice implements Action {

    String cap = "Pickling jar";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        ArrayList<WItem> bankies = gui.getInventory().getItems(cap);
        ArrayList<WItem> bankies_to_fill = new ArrayList<>();

        if(bankies.isEmpty()) return Results.ERROR("No jars in the inventory.");
        for (WItem banka : bankies){
            if(!banka_has_liquid(banka)){
                bankies_to_fill.add(banka);
            }

            NGItem.NContent content = getBankaContent(banka);
            if(content == null){
                gui.msg("Banka has no liquid, adding");
                continue;
            }
            if(NUtils.parseStartDouble(content.name()) < 1.12)
            {
                double banka_liters = NUtils.parseStartDouble(content.name());
                gui.msg("Banka has not enough liquid: " + banka_liters);
                bankies_to_fill.add(banka);
            }
        }

        if(bankies_to_fill.isEmpty()) return Results.ERROR("No jars to fill.");

        new FillBucket().run(gui);
        WItem bucket_with_brine = NUtils.getEquipment().findBucket("Brine");
        if(bucket_with_brine == null){
            return Results.ERROR("Bucket was not filled with brine.");
        }

        for(WItem banka_tf : bankies_to_fill){
            if(get_banka_free(banka_tf) != 1){
                gui.msg("Banka does not have 8 veges inside. Skipping.");
                continue;
            }
            bucket_with_brine = NUtils.getEquipment().findBucket("Brine");
            bucketToEmptyHand(bucket_with_brine);

            if (NUtils.getGameUI().vhand != null) {
                if(!((NGItem)bucket_with_brine.item).content().isEmpty()){
                    double old_brine_lvl = NUtils.parseStartDouble(((NGItem)bucket_with_brine.item).content().getFirst().name());
                    NUtils.itemact(banka_tf);

                    NUtils.getUI().core.addTask(new BucketLiquidLevelChanged(NUtils.getGameUI().vhand, old_brine_lvl));
                    NGItem.NContent content = getBankaContent(banka_tf);
                    if(NUtils.parseStartDouble(content.name()) < 1.12){
                        dropToHandSlot();
                        new FillBucket().run(gui);
                        bucket_with_brine = NUtils.getEquipment().findBucket("Brine");
                        bucketToEmptyHand(bucket_with_brine);
                        NUtils.itemact(banka_tf);
                    }

                    NUtils.getUI().core.addTask(new HandNotFree());
                    dropToHandSlot();
                }
            }
        }

        return Results.SUCCESS();
    }

    private static void bucketToEmptyHand(WItem bucket_with_brine) throws InterruptedException {
        if (NUtils.getGameUI().vhand == null) {
            NUtils.takeItemToHand(bucket_with_brine);//take item to vhand
            NUtils.getUI().core.addTask(new HandNotFree());
        }
    }

    private static void dropToHandSlot() throws InterruptedException {
        NUtils.getEquipment().wdgmsg("drop", -1);
        NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
    }

    private static NGItem.NContent getBankaContent(WItem banka) {
        if(!((NGItem) banka.item).content().isEmpty()){
            NGItem.NContent content = ((NGItem) banka.item).content().getFirst();
            return content;
        } else {
            return null;
        }
    }

    private static boolean banka_has_liquid(WItem banka) {
        ArrayList<NGItem.NContent> items = ((NGItem) banka.item).content();
        return (items.size() != 0);
    }

    private static int get_banka_free(WItem banka) throws InterruptedException {
        NGItem ngi = ((NGItem) banka.item);
        NInventory banka_inv = (NInventory) ngi.contents;
        int fspace = banka_inv.getFreeSpace();

        return fspace;
    }

}
