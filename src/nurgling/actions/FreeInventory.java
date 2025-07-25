package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Context;

import java.util.HashSet;

@Deprecated
public class FreeInventory implements Action
{
    Context context;

    public FreeInventory(Context context) {
        this.context = context;
    }

    HashSet<String> targets = new HashSet<>();

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

        for(WItem item : gui.getInventory().getItems())
        {
            String name = ((NGItem)item.item).name();
            NArea area = NContext.findOut(name, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1);
            if(area == null)
            {
                area = NContext.findOutGlobal(name, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1,gui);
            }
            if(area != null) {
                targets.add(name);
            }

        }

        new TransferItems(context, targets).run(gui);

        return Results.SUCCESS();
    }
}
