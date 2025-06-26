package nurgling.actions.bots;

import haven.Resource;
import haven.StaticGSprite;
import haven.res.lib.itemtex.ItemTex;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import javax.print.attribute.standard.MediaSize;
import java.util.*;


public class Craft implements Action {


    public Craft(List<NMakewindow.Spec> in, List<NMakewindow.Spec> out, String station, int count) {

    }

    public Craft(List<NMakewindow.Spec> in, List<NMakewindow.Spec> out, String station) {
        this(in, out, station, 1);
    }

    public Craft(NMakewindow mwnd, int size) {
        this.mwnd = mwnd;
        this.count = size;
    }

    public Craft(NMakewindow mwnd) {
        this(mwnd, 1);
    }

    NMakewindow mwnd = null;
    String tools = null;
    int count = 0;

    boolean isGlobalMode = false;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (mwnd != null) {
            return mwnd_run(gui);
        }
        return Results.SUCCESS();
    }

    private Results mwnd_run(NGameUI gui) throws InterruptedException {
        NContext ncontext = new NContext(gui);
        int size = 0;
        for (NMakewindow.Spec s : mwnd.inputs) {

            if (!s.categories)
            {
                ncontext.addInItem(s.name,ItemTex.create(ItemTex.save(s.spr)));
                size += s.count;
            }
            else if (s.ing != null)
            {
                ncontext.addInItem(s.ing.name,ItemTex.create(ItemTex.save(s.spr)));
                size += s.count;
            }
        }

        for (NMakewindow.Spec s : mwnd.outputs) {
            size += s.count;
            if (!mwnd.noTransfer.a) {
                if (!s.categories) {
                    ncontext.addOutItem(s.name, ItemTex.create(ItemTex.save(s.spr)), 1);
                } else if (s.ing != null) {
                    ncontext.addOutItem(s.ing.name, ItemTex.create(ItemTex.save(s.spr)),1);
                }
            }
        }

        if (!mwnd.tools.isEmpty()) {
            ncontext.addTools(mwnd.tools);
        } else {
            if (mwnd.outputs.size() == 1) {
                String outName = mwnd.outputs.get(0).name;
                ncontext.addCustomTool(outName);
            }
        }

        if (ncontext.equip != null)
            new Equip(new NAlias(ncontext.equip)).run(gui);

        int left = count;
        while (left > 0) {
            int for_craft = Math.min(left, NUtils.getGameUI().getInventory().getFreeSpace() / size);
            for (NMakewindow.Spec s : mwnd.inputs) {
                if (!new TakeItems2(ncontext, s.ing == null ? s.name : s.ing.name, s.count * for_craft).run(gui).IsSuccess())
                    return Results.FAIL();
            }


            new Drink(0.9, false).run(gui);
            if (ncontext.workstation != null) {
                if (!new PrepareWorkStation(ncontext, ncontext.workstation.station).run(gui).IsSuccess() || !new UseWorkStation(ncontext).run(gui).IsSuccess())
                    return Results.ERROR("NO WORKSTATION");
            }

            int resfc = for_craft;
            for (NMakewindow.Spec s : mwnd.outputs) {
                resfc = s.count * for_craft;
                if(s.ing!=null)
                {
                    resfc += NUtils.getGameUI().getInventory().getItems(new NAlias(s.ing.name)).size();
                }
                else
                {
                    resfc += NUtils.getGameUI().getInventory().getItems(new NAlias(s.name)).size();
                }
            }

            mwnd.wdgmsg("make", 1);
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.prog != null && gui.prog.prog > 0;
                }
            });
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.prog == null || !gui.prog.visible;
                }
            });

            for (NMakewindow.Spec s : mwnd.outputs) {
                if(s.ing!=null)
                {
                    NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.ing.name), resfc));
                }
                else
                {
                    NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.name), resfc));
                }
            }
            HashSet<String> targets = new HashSet<>();
            for (NMakewindow.Spec s : mwnd.outputs) {
                GetItems gi;
                if(s.ing!=null)
                {
                    NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.ing.name)));
                    targets.add(s.ing.name);
                }
                else
                {
                    NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.name)));
                    targets.add(s.name);
                }


            }
            if (!mwnd.noTransfer.a) {

                new FreeInventory2(ncontext).run(gui);
            }
            left -= for_craft;
        }


        return Results.SUCCESS();
    }
}
