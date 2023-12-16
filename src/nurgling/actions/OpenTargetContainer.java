package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import nurgling.tasks.*;

public class OpenTargetContainer implements Action
{
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        gui.map.wdgmsg ( "click", Coord.z, gob.rc.floor ( posres ), 3, 0, 0, ( int ) gob.id,
                gob.rc.floor ( posres ), 0, -1 );
        switch (name)
        {
            case "Stockpile":
                gui.ui.core.addTask(new FindNISBox(name));
                break;
            case "Barter Stand":
                gui.ui.core.addTask(new FindBarterStand());
                break;
            default:
                gui.ui.core.addTask(new FindNInventory(name));
        }
        return Results.SUCCESS();
    }

    public OpenTargetContainer(String name, Gob gob)
    {
        this.name = name;
        this.gob = gob;
    }

    String name;
    Gob gob;
}
