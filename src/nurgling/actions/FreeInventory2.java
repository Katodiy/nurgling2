package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NContext;

import java.util.HashSet;

public class FreeInventory2 implements Action
{
    NContext context;

    public FreeInventory2(NContext context) {
        this.context = context;
    }

    HashSet<String> targets = new HashSet<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

        for(WItem item : gui.getInventory().getItems())
        {
            String name = ((NGItem)item.item).name();
            if(context.addOutItem(name, null, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1))
                targets.add(name);
        }

        new TransferItems2(context, targets).run(gui);

        return Results.SUCCESS();
    }
}
