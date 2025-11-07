package nurgling.actions.bots;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import haven.res.ui.relcnt.RelCont;
import haven.res.ui.tt.cn.CustomName;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import javax.print.attribute.standard.MediaSize;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static haven.OCache.posres;


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

    private int getActualItemCount(WItem item) {
        if (item.item.info != null) {
            for (ItemInfo inf : item.item.info) {
                if (inf instanceof CustomName) {
                    float count = ((CustomName) inf).count;
                    if (count > 0) {
                        return (int) (count * 100);
                    }
                }
            }
        }
        return 1;
    }

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

            if (!s.categories) {
                ncontext.addInItem(s.name, ItemTex.create(ItemTex.save(s.spr)));
                if (!ncontext.isInBarrel(s.name)) {
                    size += s.count;
                }
            } else if (s.ing != null) {
                ncontext.addInItem(s.ing.name, ItemTex.create(ItemTex.save(s.spr)));
                if (!ncontext.isInBarrel(s.ing.name)) {
                    size += s.count;
                }
            }
        }

        for (NMakewindow.Spec s : mwnd.outputs) {

            if (!mwnd.noTransfer.a) {
                if (!s.categories) {
                    if(!ncontext.isInBarrel(s.name))
                        size += s.count;
                    ncontext.addOutItem(s.name, ItemTex.create(ItemTex.save(s.spr)), 1);
                } else if (s.ing != null) {
                    if(!ncontext.isInBarrel(s.ing.name))
                        size += s.count;
                    ncontext.addOutItem(s.ing.name, ItemTex.create(ItemTex.save(s.spr)), 1);
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

        AtomicInteger left = new AtomicInteger(count);

        for (NMakewindow.Spec s : mwnd.inputs) {
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                if (ncontext.workstation == null) {
                    NArea barrelwa = ncontext.getSpecArea(Specialisation.SpecName.barrelworkarea);
                    if (barrelwa == null)
                        return Results.ERROR("Not found area for work with barrels!");
                    else
                        ncontext.bwaused = true;
                }
                else
                {
                    ncontext.bwaused = true;
                }
            }
        }


        Results craftResult = null;
        while (left.get() > 0) {
            craftResult = crafting(ncontext, gui, size, left);
            if (!craftResult.IsSuccess()) {
                return craftResult;
            }
        }

        for (NMakewindow.Spec s : mwnd.inputs) {
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                new ReturnBarrelFromWorkArea(ncontext, item).run(gui);
            }
        }

        if (!mwnd.noTransfer.a) {
            new FreeInventory2(ncontext).run(gui);
        }


        return Results.SUCCESS();
    }

    Results crafting(NContext ncontext, NGameUI gui, int size, AtomicInteger left) throws InterruptedException {

        double currentEnergy = NUtils.getEnergy();

        if (currentEnergy < 0.25) {
            if (!new RestoreResources().run(gui).IsSuccess()) {
                return Results.ERROR("Energy too low and failed to restore resources");
            }
        }
        
        int freeSpace = NUtils.getGameUI().getInventory().getFreeSpace();

        int for_craft;
        if (size == 0) {
            for_craft = left.get();
        } else {
            for_craft = Math.min(left.get(), freeSpace / size);
        }
        

        if (for_craft <= 0) {
            return Results.ERROR("Not enough inventory space");
        }
        
        for (NMakewindow.Spec s : mwnd.inputs) {
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                if(ncontext.workstation == null) {
                    new TransferBarrelInWorkArea(ncontext, item).run(gui);
                }
                else if(ncontext.workstation.targetPoint == null)
                {
                    new TransferBarrelToWorkstation(ncontext, item).run(gui);
                }
            } else {
                if (!new TakeItems2(ncontext, s.ing == null ? s.name : s.ing.name, s.count * for_craft).run(gui).IsSuccess()) {
                    return Results.ERROR("Failed to take items: " + item);
                }
            }
        }



        if (ncontext.workstation != null) {
            if (!new PrepareWorkStation(ncontext, ncontext.workstation.station).run(gui).IsSuccess()) {
                return Results.ERROR("Failed to prepare workstation");
            }
            if (ncontext.workstation.targetPoint != null) {
                new PathFinder(ncontext.workstation.targetPoint.getCurrentCoord()).run(gui);
            }
            if (!new UseWorkStation(ncontext).run(gui).IsSuccess()) {
                return Results.ERROR("Failed to use workstation");
            }
        }
        else if (ncontext.bwaused) {
            NArea barrelwa = ncontext.getSpecArea(Specialisation.SpecName.barrelworkarea);
            Pair<Coord2d, Coord2d> rcArea = barrelwa.getRCArea();
            Coord2d center = rcArea.b.sub(rcArea.a).div(2).add(rcArea.a);
            new PathFinder(center).run(gui);
        }

        int count = 0;

        for (Long barrelid : GetBarrelsIds(ncontext)) {
            Gob barrel = Finder.findGob(barrelid);
            gui.map.wdgmsg("click", Coord.z, barrel.rc.floor(posres), 3, 0, 0, (int) barrel.id,
                    barrel.rc.floor(posres), 0, -1);
            count++;
            int finalCount = count;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return NUtils.getGameUI().getWindowsNum("Barrel") == finalCount;
                }
            });
        }
        ArrayList<Window> windows = NUtils.getGameUI().getWindows("Barrel");
        boolean hasEnoughResources = true;
        for (NMakewindow.Spec s : mwnd.inputs) {
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item)) {
                double val = gui.findBarrelContent(windows, new NAlias(item));
                double valInMilligrams = val * 100;
                if(valInMilligrams < s.count)
                {
                    hasEnoughResources = false;
                    break;
                }
            }
        }
        
        if (!hasEnoughResources) {
            for (NMakewindow.Spec s : mwnd.inputs) {
                String item = s.ing == null ? s.name : s.ing.name;
                if (ncontext.isInBarrel(item)) {
                    new ReturnBarrelFromWorkArea(ncontext, item).run(gui);
                }
            }
            return Results.ERROR("Not enough resources in barrels");
        }

        new Drink(0.9, false).run(gui);
        int resfc = for_craft;
        String targetName = null;
        for (NMakewindow.Spec s : mwnd.outputs) {
            resfc = s.count * for_craft;
            ArrayList<WItem> currentItems;
            if (s.ing != null) {
                targetName = s.ing.name;
                currentItems = NUtils.getGameUI().getInventory().getItems(new NAlias(s.ing.name));
            } else {
                targetName = s.name;
                currentItems = NUtils.getGameUI().getInventory().getItems(new NAlias(s.name));
            }
            
            int actualCurrentCount = 0;
            for (WItem item : currentItems) {
                actualCurrentCount += getActualItemCount(item);
            }
            
            resfc += actualCurrentCount;
            
        }

        mwnd.wdgmsg("make", 1);
        int finalResfc = resfc;
        String finalTargetName = targetName;
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {

                return (((gui.prog != null) && (gui.prog.prog > 0) && ((ncontext.workstation == null) || (ncontext.workstation.selected == -1) || NUtils.isWorkStationReady(ncontext.workstation.station, Finder.findGob(ncontext.workstation.selected)))));
            }
        });
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                GetItems gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(finalTargetName));
                gi.check();
                return gui.prog == null || !gui.prog.visible || gi.getResult().size() >= finalResfc;
            }
        });
        NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),3, 0);
        NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),1, 0);
        for (NMakewindow.Spec s : mwnd.outputs) {
            if (s.ing != null) {
                NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.ing.name), resfc));
            } else {
                NUtils.getUI().core.addTask(new WaitItems(NUtils.getGameUI().getInventory(), new NAlias(s.name), resfc));
            }
        }
        HashSet<String> targets = new HashSet<>();
        for (NMakewindow.Spec s : mwnd.outputs) {
            GetItems gi;
            if (s.ing != null) {
                NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.ing.name)));
                targets.add(s.ing.name);
            } else {
                NUtils.getUI().core.addTask(gi = new GetItems(NUtils.getGameUI().getInventory(), new NAlias(s.name)));
                targets.add(s.name);
            }


        }
        if (!mwnd.noTransfer.a) {
            new FreeInventory2(ncontext).run(gui);
        }
        left.set(left.get() - for_craft);
        return Results.SUCCESS();
    }

    ArrayList<Long> GetBarrelsIds(NContext ncontext) throws InterruptedException
    {
        ArrayList<Long> ids = new ArrayList<>();
        for (NMakewindow.Spec s : mwnd.inputs)
        {
            String item = s.ing == null ? s.name : s.ing.name;
            if (ncontext.isInBarrel(item))
            {
                Gob barrel = ncontext.getBarrelInWorkArea(item);
                if (barrel != null)
                    ids.add(barrel.id);
            }
        }
        return ids;
    }
}
