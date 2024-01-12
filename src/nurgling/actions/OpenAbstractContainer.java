package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.tasks.*;
import nurgling.tools.Context;

import java.util.ArrayList;

import static haven.OCache.posres;

public class OpenAbstractContainer implements Action
{
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        gui.map.wdgmsg ( "click", Coord.z, cont.gob.rc.floor ( posres ), 3, 0, 0, ( int ) cont.gob.id,
                cont.gob.rc.floor ( posres ), 0, -1 );

        FindAbstractNInventory fani = new FindAbstractNInventory(candidats);
        gui.ui.core.addTask(fani);
        context.updateContainer(fani.getCap(),fani.getInv(),cont);
        return Results.SUCCESS();
    }

    public OpenAbstractContainer(ArrayList<String> candidats, Context.Container cont, Context context)
    {
        this.candidats = candidats;
        this.cont = cont;
        this.context = context;
    }

    ArrayList<String> candidats;
    Context.Container cont;
    Context context;
    String result;
}
