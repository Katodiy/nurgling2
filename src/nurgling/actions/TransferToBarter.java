package nurgling.actions;

import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.res.ui.barterbox.Shopbox;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitItems;
import nurgling.tools.Context;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class TransferToBarter implements Action{

    NAlias items;
    Context.OutputBarter barter;

    public TransferToBarter(Context.OutputBarter barter, NAlias items) {
        this.barter = barter;
        this.items = items;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        new PathFinder(barter.barter).run(gui);
        new OpenTargetContainer("Barter Stand", barter.barter).run(gui);

        Window barter_wnd = gui.getWindow("Barter Stand");
        if(barter_wnd==null)
        {
            return Results.ERROR("No Barter window");
        }
        ArrayList<WItem> wItems = NUtils.getGameUI().getInventory().getItems(items);
        for(Widget ch = barter_wnd.child; ch != null; ch = ch.next)
        {
            if (ch instanceof Shopbox)
            {
                Shopbox sb = (Shopbox) ch;
                Shopbox.ShopItem price = sb.getPrice();
                if (price != null)
                {
                    if (NParser.checkName(price.name, items))
                    {
                        while (!wItems.isEmpty()) {
                            int target_size = (sb.leftNum != 0) ? Math.min(wItems.size(), sb.leftNum) : wItems.size();
                            for (int i = 0; i < target_size; i++) {
                                sb.wdgmsg("buy", new Object[0]);
                            }
                            NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), items, wItems.size() - target_size));
                            new PathFinder(barter.chest).run(gui);
                            new OpenTargetContainer("Chest", barter.chest).run(gui);
                            ArrayList<WItem> items = gui.getInventory("Chest").getItems("Branch");
                            new SimpleTransferToContainer(gui.getInventory("Chest"), gui.getInventory().getItems("Branch"), items.size()).run(gui);
                        }
                    }
                }
            }
        }
        return Results.SUCCESS();
    }

}
