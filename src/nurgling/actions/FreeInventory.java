package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;

import java.util.ArrayList;
import java.util.HashSet;

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
            NArea area = NArea.findOut(name, ((NGItem)item.item).quality!=null?((NGItem)item.item).quality:1);
            if(area != null) {
                targets.add(name);
            }
        }

        for(String name: targets)
        {
            new TransferItems(context, name).run(gui);
        }

        return Results.SUCCESS();
    }
}
