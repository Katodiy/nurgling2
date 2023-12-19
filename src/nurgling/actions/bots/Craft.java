package nurgling.actions.bots;

import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import java.util.*;

public class Craft implements Action
{

    public Craft(List<NMakewindow.Spec> in, List<NMakewindow.Spec> out, String station, int count)
    {

    }

    public Craft(List<NMakewindow.Spec> in, List<NMakewindow.Spec> out, String station)
    {
        this(in, out, station, 1);
    }

    public Craft(NMakewindow mwnd, int size)
    {
        this.mwnd = mwnd;
        this.count = size;
    }

    public Craft(NMakewindow mwnd)
    {
        this(mwnd,1);
    }

    NMakewindow mwnd = null;
    String tools = null;
    int count = 0;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(mwnd!=null)
        {
            return mwnd_run(gui);
        }
        return Results.SUCCESS();
    }

    private Results mwnd_run(NGameUI gui) throws InterruptedException
    {
        Context context = new Context();
        int size = 0;
        for(NMakewindow.Spec s: mwnd.inputs)
        {
            NArea area = NArea.findIn(s.name);
            if(area == null)
                return Results.ERROR("NO area for: " + s.name);
            context.addInput(s.name, Context.GetInput(s.name, area));
            size += s.count;
        }

        for(NMakewindow.Spec s: mwnd.outputs)
        {
            size +=s.count;
        }

        if(!mwnd.tools.isEmpty())
        {
           context.addTools(mwnd.tools);
        }

        int left = count;
        while (left>0)
        {
            int for_craft = Math.min(left,NUtils.getGameUI().getInventory().getFreeSpace()/size);
            for(NMakewindow.Spec s: mwnd.inputs)
            {
                new TakeItems(context, s.name, s.count * for_craft).run(gui);
            }

            new Drink(0.9).run(gui);
            if(context.workstation!=null)
            {
                new UseWorkStation(context).run(gui);
            }
            mwnd.wdgmsg("make", 1);
            for(NMakewindow.Spec s: mwnd.outputs)
            {
                NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.name), for_craft));
            }
            left -=for_craft;
        }


        return Results.SUCCESS();
    }
}
