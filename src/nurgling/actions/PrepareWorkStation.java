package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.NTask;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

public class PrepareWorkStation implements Action
{
    public PrepareWorkStation(NContext context, String name)
    {
        this.name = name;
        this.context = context;
    }

    String name;
    NContext context;
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NArea area = context.getSpecArea(context.workstation);
        if(area == null)
            return Results.ERROR("NO WORKSTATION");
        Gob ws = null;
        if(name.startsWith("gfx/terobjs/pow"))
        {
            ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias("gfx/terobjs/pow"));
            gobs.sort(NUtils.d_comp);
            for(Gob gob: gobs)
            {
                if ((gob.ngob.getModelAttribute() & 48) == 0) {
                    ws = gob;
                    break;
                }
            }
        }
        else {
            ws = Finder.findGob(area, new NAlias(name));
        }
        if(ws == null)
            return Results.ERROR("NO WORKSTATION");
        else
        {
            context.workstation.selected = ws.id;
            if(name.contains("crucible"))
            {
                if(fillCrucible(ws,gui))
                    new LightGob(new ArrayList<>(Arrays.asList(ws.id)),4).run(gui);
            }
            else if(name.startsWith("gfx/terobjs/pow"))
            {
                ArrayList<Gob> pows = new ArrayList<>(Arrays.asList(ws));
                if(!new FillFuelPowOrCauldron(context, pows, 1).run(gui).IsSuccess())
                    return Results.FAIL();
                ArrayList<Long> flighted = new ArrayList<>();
                for (Gob cont : pows) {
                    flighted.add(cont.id);
                }
                if (!new LightGob(flighted, 4).run(gui).IsSuccess())
                    return Results.ERROR("I can't start a fire");
            }
            else if(name.startsWith("gfx/terobjs/cauldron"))
            {
                ArrayList<Gob> pows = new ArrayList<>(Arrays.asList(ws));
                if(!new FillFuelPowOrCauldron(context, pows, 1).run(gui).IsSuccess())
                    return Results.FAIL();
                ArrayList<Long> flighted = new ArrayList<>();
                for (Gob cont : pows) {
                    flighted.add(cont.id);
                }
                if (!new LightGob(flighted, 2).run(gui).IsSuccess())
                    return Results.ERROR("I can't start a fire");
                if((Finder.findGob(context.workstation.selected).ngob.getModelAttribute()&4)==0)
                {
                    new FillFluid(Finder.findGob(context.workstation.selected), NContext.findSpec(Specialisation.SpecName.water.toString())!=null ? NContext.findSpec(Specialisation.SpecName.water.toString()).getRCArea():null,new NAlias("water"),4).run(gui);
                }
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return (Finder.findGob(context.workstation.selected).ngob.getModelAttribute()&8)==8;
                    }
                });

            }
        }
        return Results.SUCCESS();
    }

    boolean fillCrucible(Gob crucible, NGameUI gui) throws InterruptedException
    {
        if((crucible.ngob.getModelAttribute()&2)==2)
            return true;
        int count = 1;
        if(NUtils.getGameUI().getInventory().getFreeSpace()==0)
            return false;

        if(NUtils.getGameUI().getInventory().getItems("Coal").isEmpty()) {
            int target_size = count;
            while (target_size != 0 && NUtils.getGameUI().getInventory().getFreeSpace() != 0) {
                ArrayList<Gob> piles = Finder.findGobs(NContext.findSpec(Specialisation.SpecName.fuel.toString(), "Coal"), new NAlias("stockpile"));
                if (piles.isEmpty()) {
                    if (gui.getInventory().getItems().isEmpty())
                        return false;
                    else
                        break;
                }
                piles.sort(NUtils.d_comp);

                Gob pile = piles.get(0);
                new PathFinder(pile).run(gui);
                new OpenTargetContainer("Stockpile", pile).run(gui);
                TakeItemsFromPile tifp;
                (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(target_size, gui.getInventory().getFreeSpace()))).run(gui);
                new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                target_size = target_size - tifp.getResult();
            }
        }
        new PathFinder(crucible).run(gui);
        ArrayList<WItem> fueltitem = NUtils.getGameUI().getInventory().getItems("Coal");
        if (fueltitem.isEmpty()) {
            return false;
        }
        for(int i=0; i<1;i++) {
            NUtils.takeItemToHand(fueltitem.get(i));
            NUtils.activateItem(crucible);
            NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
        }
        return true;
    }
}
