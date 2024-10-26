package nurgling.actions.bots;

import haven.Resource;
import haven.StaticGSprite;
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
            if(!s.categories) {
                NArea area = NArea.findIn(s.name);
                if (area == null) {
                    SelectArea insa;
                    NUtils.getGameUI().msg("Please select area with:" + s.name);
                    (insa = new SelectArea(Resource.loadsimg("baubles/custom"),((StaticGSprite)s.spr).img.img)).run(gui);
                    context.addInput(s.name, Context.GetInput(s.name, insa.getRCArea()));
                    size += s.count;
                }
                else {
                    context.addInput(s.name, Context.GetInput(s.name, area));
                    size += s.count;
                }
            }
            else if(s.ing!=null)
            {
                NArea area = NArea.findIn(s.ing.name);
                if (area == null) {
                    SelectArea insa;
                    NUtils.getGameUI().msg("Please select area with:" + s.ing.name);
                    (insa = new SelectArea(Resource.loadsimg("baubles/custom"),s.ing.img)).run(gui);
                    context.addInput(s.ing.name, Context.GetInput(s.ing.name, insa.getRCArea()));
                    size += s.count;
                }
                else {
                    context.addInput(s.ing.name, Context.GetInput(s.ing.name, area));
                    size += s.count;
                }
            }
        }

        for(NMakewindow.Spec s: mwnd.outputs)
        {
            size +=s.count;
            NArea area = NArea.findOut(s.name, 1);
            if(area != null) {
                context.addOutput(s.name, Context.GetOutput(s.name, area));
            }
        }

        if(!mwnd.tools.isEmpty())
        {
           context.addTools(mwnd.tools);
        }

        if(context.equip!=null)
            new Equip(new NAlias(context.equip)).run(gui);

        int left = count;
        while (left>0)
        {
            int for_craft = Math.min(left,NUtils.getGameUI().getInventory().getFreeSpace()/size);
            for(NMakewindow.Spec s: mwnd.inputs)
            {
                if(!new TakeItems(context, s.ing==null?s.name:s.ing.name, s.count * for_craft).run(gui).IsSuccess())
                    return Results.FAIL();
            }



            new Drink(0.9, false).run(gui);
            if(context.workstation!=null)
            {
                if(!new PrepareWorkStation(context.workstation.station).run(gui).IsSuccess() || !new UseWorkStation(context).run(gui).IsSuccess())
                    return Results.ERROR("NO WORKSTATION");
            }
            mwnd.wdgmsg("make", 1);
            HashMap<String, Integer> oldSize = new HashMap<>();
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.prog!=null && gui.prog.prog>0;
                }
            });
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return  gui.prog==null || !gui.prog.visible;
                }
            });
            int resfc = for_craft;
            for(NMakewindow.Spec s: mwnd.outputs)
            {
                resfc = s.count*for_craft;
            }
            for(NMakewindow.Spec s: mwnd.outputs)
            {
                if(s.name.contains("nugget"))
                {
                    NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias("nugget"), 10*for_craft));
                }
                else {
                    NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.name), resfc));
                }
            }
            HashSet<String> targets = new HashSet<>();
            for(NMakewindow.Spec s: mwnd.outputs)
            {
                GetItems gi;
                NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.name)));
                NArea area = NArea.findOut(s.name, 1);
                if(area != null) {
                    targets.add(s.name);
                }
            }
            new TransferItems(context,targets).run(gui);
            left -=for_craft;
        }


        return Results.SUCCESS();
    }
}
