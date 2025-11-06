package nurgling.actions;

import haven.*;
import haven.res.ui.tt.cn.CustomName;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TransferToBarrel implements Action{

    Gob barrel;
    NAlias items;

    int th = 9000;

    double total = 0;
    public TransferToBarrel(Gob barrel, NAlias items) {
        this.barrel = barrel;
        this.items = items;
    }

    public TransferToBarrel(Gob barrel, NAlias items, int th) {
        this(barrel, items);
        this.th = th;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("[TransferToBarrel] Starting transfer for: " + items + ", threshold: " + th);
        
        if(barrel==null){
            System.out.println("[TransferToBarrel] ERROR: Barrel is null");
            return Results.ERROR("NULL BARREL");
        }
        System.out.println("[TransferToBarrel] Moving to barrel...");
        new PathFinder( barrel ).run (gui);
        System.out.println("[TransferToBarrel] Opening barrel...");
        if ( !(new OpenTargetContainer (  "Barrel",barrel ).run ( gui ).isSuccess) ) {
            System.out.println("[TransferToBarrel] ERROR: Failed to open barrel");
            return Results.ERROR("OPEN FAIL");
        }
        double barrelCont = gui.getBarrelContent();
        System.out.println("[TransferToBarrel] Current barrel content: " + barrelCont);
        total+=barrelCont;
        if(barrelCont>-1 && barrelCont < th) {

            ArrayList<WItem> witems = gui.getInventory().getItems(items);
            System.out.println("[TransferToBarrel] Found " + witems.size() + " items in inventory matching " + items);
            ArrayList<WItem> targetItems = new ArrayList<>();
            double sum = 0;
            for (WItem item : witems) {
                if (sum + barrelCont > th) {
                    System.out.println("[TransferToBarrel] Stopping: would exceed threshold (" + (sum + barrelCont) + " > " + th + ")");
                    break;
                }
                for (ItemInfo inf : item.item.info) {
                    if (inf instanceof GItem.Amount) {
                        int itemNum = ((GItem.Amount) inf).itemnum();
                        System.out.println("[TransferToBarrel] Item Amount: " + itemNum);
                        if(sum + itemNum<10000) {
                            sum += itemNum;
                            targetItems.add(item);
                            System.out.println("[TransferToBarrel]   -> Added, sum now: " + sum);
                            break;
                        }
                    }
                    if (inf instanceof CustomName)
                    {
                        float count = ((CustomName) inf).count;
                        System.out.println("[TransferToBarrel] Item CustomName count: " + count + ", sum: " + sum + ", check: " + (sum + count) + " < 100");
                        if(count > 0 && sum + count < 100) {
                            sum += count;
                            targetItems.add(item);
                            System.out.println("[TransferToBarrel]   -> Added, sum now: " + sum);
                            break;
                        } else {
                            System.out.println("[TransferToBarrel]   -> SKIPPED (count <= 0 or would exceed 100)");
                        }
                    }
                }
            }
            total+=sum;
            System.out.println("[TransferToBarrel] Target items to transfer: " + targetItems.size() + ", total sum: " + sum);

            if(!targetItems.isEmpty()) {
                System.out.println("[TransferToBarrel] Transferring items to barrel...");
                NUtils.takeItemToHand(targetItems.get(0));
                if(witems.size() == targetItems.size()) {
                    if(barrelCont == 0)
                    {
                        NUtils.activateItem(barrel, true);
                        if (targetItems.size()>1) {
                            NUtils.getUI().core.addTask(new NotThisInHand(NUtils.getGameUI().vhand));
                        }
                    }
                    NUtils.dropsame(barrel);
                    NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), items, 0));
                }
                else
                {
                    for (int i = 0; i < targetItems.size(); i++) {
                        NUtils.activateItem(barrel, true);
                        if (i + 1 < targetItems.size()) {
                            NUtils.getUI().core.addTask(new NotThisInHand(NUtils.getGameUI().vhand));
                        }
                    }
                    NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), items, witems.size() - targetItems.size() - 1));


                    if (NUtils.getGameUI().vhand != null ) {
                        NUtils.getUI().core.addTask(new WaitItemInHand());
                        gui.getInventory().dropOn(gui.getInventory().findFreeCoord(NUtils.getGameUI().vhand));
                    }
                }
            } else {
                System.out.println("[TransferToBarrel] No items to transfer (targetItems is empty)");
            }
        } else {
            System.out.println("[TransferToBarrel] Barrel is full or invalid content: " + barrelCont + " (threshold: " + th + ")");
        }
        System.out.println("[TransferToBarrel] Closing barrel");
        new CloseTargetContainer ( "Barrel" ).run ( gui );
        return Results.SUCCESS();
    }

    public boolean isFull()
    {
        return total>th;
    }
}
