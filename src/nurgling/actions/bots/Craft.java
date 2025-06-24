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
        Context context = new Context();
        int size = 0;
        for (NMakewindow.Spec s : mwnd.inputs) {
            if (!s.categories) {
                NArea area = NArea.findIn(s.name);
                if(area == null)
                {
                    area = NArea.findInGlobal(s.ing.name);
                    if(area!=null)
                    {
                        isGlobalMode = true;
                    }
                }
                if (area == null) {
                    SelectArea insa;
                    NUtils.getGameUI().msg("Please select area with:" + s.name);
                    (insa = new SelectArea(Resource.loadsimg("baubles/custom"), ItemTex.create(ItemTex.save(s.spr)))).run(gui);
                    context.addInput(s.name, Context.GetInput(s.name, insa.getRCArea()));
                    size += s.count;
                } else {

                    /// TODO global

                    context.addInput(s.name, Context.GetInput(s.name, area));
                    size += s.count;
                }
            } else if (s.ing != null) {
                NArea area = NArea.findIn(s.ing.name);
                if(area == null)
                {
                    area = NArea.findInGlobal(s.ing.name);
                    if(area!=null)
                    {
                        isGlobalMode = true;
                    }
                }
                if (area == null) {
                    SelectArea insa;
                    NUtils.getGameUI().msg("Please select area with:" + s.ing.name);
                    (insa = new SelectArea(Resource.loadsimg("baubles/custom"), s.ing.img)).run(gui);
                    context.addInput(s.ing.name, Context.GetInput(s.ing.name, insa.getRCArea()));
                    size += s.count;
                } else {

                    /// TODO global

                    context.addInput(s.ing.name, Context.GetInput(s.ing.name, area));
                    size += s.count;
                }
            }
        }

        for (NMakewindow.Spec s : mwnd.outputs) {
            size += s.count;
            if (!mwnd.noTransfer.a) {
                if (!s.categories) {
                    NArea area = NArea.findOut(s.name, 1);
                    if(area == null)
                    {
                        area = NArea.findOutGlobal(s.name, 1, gui);
                        if(area!=null) {
                            isGlobalMode = true;
                        }
                    }
                    if (area == null) {
                        SelectArea outsa;
                        NUtils.getGameUI().msg("Please select area for:" + s.name);
                        (outsa = new SelectArea(Resource.loadsimg("baubles/custom"), ItemTex.create(ItemTex.save(s.spr)))).run(gui);
                        context.addOutput(s.name, Context.GetOutput(s.name, outsa.getRCArea()));
                        size += s.count;
                    } else {


                        /// TODO global

                        context.addOutput(s.name, context.GetOutput(s.name, area));
                        size += s.count;
                    }
                } else if (s.ing != null) {
                    NArea area = NArea.findOut(s.ing.name, 1);
                    if(area == null)
                    {
                        area = NArea.findOutGlobal(s.name, 1, gui);
                        if(area!=null) {
                            isGlobalMode = true;
                        }
                    }
                    if (area == null) {
                        SelectArea outsa;
                        NUtils.getGameUI().msg("Please select area for:" + s.ing.name);
                        (outsa = new SelectArea(Resource.loadsimg("baubles/custom"), s.ing.img)).run(gui);
                        context.addOutput(s.ing.name, Context.GetOutput(s.ing.name, outsa.getRCArea()));
                        size += s.count;
                    } else {

                        /// TODO global

                        context.addOutput(s.name, context.GetOutput(s.name, area));
                        size += s.count;
                    }
                }
            }
        }

        if (!mwnd.tools.isEmpty()) {
            context.addTools(mwnd.tools);
        } else {
            if (mwnd.outputs.size() == 1) {
                String outName = mwnd.outputs.get(0).name;
                context.addCustomTool(outName);
            }
        }

        if (context.equip != null)
            new Equip(new NAlias(context.equip)).run(gui);

        int left = count;
        while (left > 0) {
            int for_craft = Math.min(left, NUtils.getGameUI().getInventory().getFreeSpace() / size);
            for (NMakewindow.Spec s : mwnd.inputs) {
                if (!new TakeItems(context, s.ing == null ? s.name : s.ing.name, s.count * for_craft).run(gui).IsSuccess())
                    return Results.FAIL();
            }


            new Drink(0.9, false).run(gui);
            if (context.workstation != null) {
                if (!new PrepareWorkStation(context, context.workstation.station).run(gui).IsSuccess() || !new UseWorkStation(context).run(gui).IsSuccess())
                    return Results.ERROR("NO WORKSTATION");
            }
            mwnd.wdgmsg("make", 1);
            HashMap<String, Integer> oldSize = new HashMap<>();
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
            int resfc = for_craft;
            for (NMakewindow.Spec s : mwnd.outputs) {
                resfc = s.count * for_craft;
            }

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
                new TransferItems(context, targets).run(gui);
            }
            left -= for_craft;
        }


        return Results.SUCCESS();
    }
}
