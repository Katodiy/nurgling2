package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.NFishingSettings;
import nurgling.tasks.*;
import nurgling.tools.Context;
import nurgling.tools.NAlias;
import nurgling.tools.VSpec;

public class Fishing implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Fishing w = null;
        NFishingSettings prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.Fishing()), UI.scale(200,200))));
            prop = w.prop;
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        finally {
            if(w!=null)
                w.destroy();
        }
        if(prop == null)
        {
            return Results.ERROR("No config");
        }
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area for fishing");
        (insa = new SelectArea(Resource.loadsimg("baubles/fishingPlace"))).run(gui);
        NContext context = new NContext(gui);
        NArea repArea = null;
        NArea baitArea= null;
        if(prop.useInventoryTools) {
            // Using tools from inventory - no zone selection needed
            NUtils.getGameUI().msg("Using tools from inventory/equipment");
        } else {

            String repAreaId = context.createArea("Please select area with repair instruments", Resource.loadsimg("baubles/fishingRep"));
            repArea = context.getAreaById(repAreaId);

            String baitAreaId = context.createArea("Please select area with baits or lures", Resource.loadsimg("baubles/fishingBaits"));
            baitArea = context.getAreaById(baitAreaId);

        }

        Pair<Coord2d,Coord2d> outArea = null;
        if(!prop.noPiles) {
            SelectArea outsa;
            NUtils.getGameUI().msg("Please select area for piles");
            (outsa = new SelectArea(Resource.loadsimg("baubles/rawFish"))).run(gui);
            outArea = outsa.getRCArea();
        }

        if(!new Equip(new NAlias(prop.tool)).run(gui).IsSuccess())
        {
            return Results.ERROR("No equip found: " + prop.tool);
        };

        Coord2d currentPos = NUtils.player().rc;
        if(prop.useInventoryTools) {
            if(!new RepairFishingRotFromInventory(prop).run(gui).IsSuccess())
                return Results.FAIL();
        } else {
            if(!new RepairFishingRot(context, prop, repArea.getRCArea(), baitArea.getRCArea()).run(gui).IsSuccess())
                return Results.FAIL();
        }
        new PathFinder(currentPos).run(gui);
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
            if (pag.button() != null && pag.button().name().equals("Fish")) {
                pag.button().use(new MenuGrid.Interaction(1, 0));
            }
        }
        NUtils.addTask(new GetCurs("fish"));
        Coord2d fishPlace = insa.getRCArea().b.add(insa.getRCArea().a).div(2);
        NUtils.lclick(fishPlace);
        NUtils.addTask(new WaitPose(NUtils.player(),"fish"));
        currentPos = NUtils.player().rc;
        

        final Pair<Coord2d,Coord2d> finalOutArea = outArea;
        
        while (true)
        {
            FishingTask ft;
            NUtils.addTask(ft = new FishingTask(prop));
            switch (ft.getState())
            {
                case NOFISH:
                    return Results.FAIL();

                case SPINWND:
                {
                    Window tib = NUtils.getGameUI().getWindow("This is bait");
                    boolean isFound = false;
                    for ( Widget tc = tib.lchild ; tc != null ; tc = tc.prev ) {
                        if(!isFound && tc instanceof Label)
                        {
                            for(String cand: prop.targets)
                            {
                                if(((Label)tc).text().contains(cand))
                                {
                                    isFound = true;
                                    break;
                                }
                            }
                        }
                        if(isFound && tc instanceof Button)
                        {
                            ((Button)tc).click();
                            break;
                        }
                    }
                    if(!isFound)
                    {
                        NUtils.lclick(fishPlace);
                    }
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return NUtils.getGameUI().getWindow("This is bait")==null;
                        }
                    });
                    break;
                }
                case NEEDREP:
                {
                    if(prop.useInventoryTools) {
                        if(!new RepairFishingRotFromInventory(prop).run(gui).IsSuccess())
                            return Results.FAIL();
                    } else {
                        if(!new RepairFishingRot(context, prop, repArea.getRCArea(), baitArea.getRCArea()).run(gui).IsSuccess())
                            return Results.FAIL();
                    }
                    startFishing(gui, currentPos, fishPlace);
                    break;
                }
                case NOFREESPACE: {
                    if(prop.noPiles) {
                        NUtils.getGameUI().msg("Inventory full, stopping fishing");
                        return Results.SUCCESS();
                    }
                    new TransferToPiles(finalOutArea, VSpec.getAllFish()).run(gui);
                    startFishing(gui, currentPos, fishPlace);
                }
            }
        }
    }

    private void startFishing(NGameUI gui, Coord2d currentPos, Coord2d fishPlace) throws InterruptedException {
        new PathFinder(currentPos).run(gui);
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
            if (pag.button() != null && pag.button().name().equals("Fish")) {
                pag.button().use(new MenuGrid.Interaction(1, 0));
            }
        }
        NUtils.addTask(new GetCurs("fish"));
        NUtils.lclick(fishPlace);
        NUtils.addTask(new WaitPose(NUtils.player(),"fish"));
    }
}
