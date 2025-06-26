package nurgling.actions.bots;

import haven.*;
import haven.res.gfx.terobjs.roastspit.Roastspit;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitCarveState;
import nurgling.tasks.WaitPose;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FriedFish implements Action {

    NAlias powname = new NAlias(new ArrayList<String>(List.of("gfx/terobjs/pow")));


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area with raw fish");
        (insa = new SelectArea(Resource.loadsimg("baubles/rawFish"))).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please select area for results");
        (outsa = new SelectArea(Resource.loadsimg("baubles/prepFish"))).run(gui);

        SelectArea powsa;
        NUtils.getGameUI().msg("Please select area with fireplaces");
        (powsa = new SelectArea(Resource.loadsimg("baubles/fireplace"))).run(gui);

        Context context = new Context();
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(outsa.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }


            ArrayList<Gob> allPow = Finder.findGobs(powsa.getRCArea(), powname);
            ArrayList<Gob> pows = new ArrayList<>();
            for (Gob gob : allPow) {
                if ((gob.ngob.getModelAttribute() & 48) == 0) {
                    Gob.Overlay ol = (gob.findol(Roastspit.class));
                    if (ol != null) {
                        pows.add(gob);
                    }
                }
            }
            pows.sort(NUtils.y_min_comp);
            pows.sort(NUtils.x_min_comp);
        while (Finder.findGob(insa.getRCArea(), new NAlias("stockpile"))!=null) {
            boolean readyToWork = false;
            for (Gob gob : pows) {
                Gob.Overlay ol = (gob.findol(Roastspit.class));
                String content = ((Roastspit) ol.spr).getContent();
                if (content == null || !content.contains("raw") ||  (gob.ngob.getModelAttribute() & 5) != 5) {
                    readyToWork = true;
                    break;
                }
            }
            if(!readyToWork) {
                ArrayList<Gob> borkas = Finder.findGobs(new NAlias("borka"));
                for (Gob gob : pows) {
                    boolean busy = false;
                    for(Gob borka : borkas) {
                        Following fl;
                        if((fl = borka.getattr(Following.class))!=null && fl.tgt == gob.id) {
                            busy = true;
                            break;
                        }
                    }
                    if(!busy) {
                        new PathFinder(gob).run(gui);
                        Gob.Overlay ol = (gob.findol(Roastspit.class));
                        new SelectFlowerAction("Turn",gob,(Roastspit) ol.spr).run(gui);
                        NUtils.addTask(new WaitPose(NUtils.player(),"gfx/borka/roasting"));
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                boolean readyToWork = false;
                                for (Gob gob : pows) {
                                    Gob.Overlay ol = (gob.findol(Roastspit.class));
                                    String content = ((Roastspit) ol.spr).getContent();
                                    if (content == null || !content.contains("raw") ||  (gob.ngob.getModelAttribute() & 5) != 5) {
                                        readyToWork = true;
                                        break;
                                    }
                                }
                                return readyToWork;
                            }
                        });
                        break;
                    }
                }
            }


            int count = 0;
            for (Gob gob : pows) {
                Gob.Overlay ol = (gob.findol(Roastspit.class));
                String content = ((Roastspit) ol.spr).getContent();
                if (content != null) {
                    while (!content.contains("raw")) {
                        new PathFinder(gob).run(gui);
                        new SelectFlowerAction("Carve", gob, ((Roastspit) ol.spr)).run(gui);
                        NUtils.addTask(new WaitPose(NUtils.player(),"gfx/borka/carving"));
                        WaitCarveState wcs = new WaitCarveState(gob);
                        NUtils.addTask(wcs);
                        if(wcs.getState()== WaitCarveState.State.NOCONTENT) {
                            count++;
                            break;
                        }
                        if(wcs.getState()== WaitCarveState.State.NOFREESPACE)
                        {
                            for(Container container : containers){
                                if(container.getattr(Container.Space.class) != null)
                                {
                                    Container.Space space = (Container.Space) container.getattr(Container.Space.class);
                                    if(!space.isReady() || (Integer) space.getRes().get(Container.Space.FREESPACE) != 0)
                                    {
                                        new TransferToContainer(container, new NAlias("Spitrosted")).run(gui);
                                    }
                                }
                            }

                        }
                    }
                } else {
                    count++;
                }
            }

            if(!NUtils.getGameUI().getInventory().getItems("Spitroast").isEmpty()) {
                for (Container container : containers) {
                    if (container.getattr(Container.Space.class) != null) {
                        Container.Space space = (Container.Space) container.getattr(Container.Space.class);
                        if (!space.isReady() || (Integer) space.getRes().get(Container.Space.FREESPACE) != 0) {
                            new TransferToContainer(container, new NAlias("Spitroast")).run(gui);
                        }
                    }
                }
            }
            LinkedList<NGItem> targetItems = new LinkedList<>();
            while (count != 0) {
                for (int i = 0; i < count; i++) {
                    if (NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1, 3)) > 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(2, 1)) > 0) {
                        Gob pile = Finder.findGob(insa.getRCArea(), new NAlias("stockpile"));
                        if (pile == null) {
                            return Results.ERROR("No raw fish");
                        }
                        new PathFinder(pile).run(gui);
                        new OpenTargetContainer("Stockpile", pile).run(gui);
                        TakeItemsFromPile tifp;
                        (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), 1)).run(gui);
                        targetItems.addAll(tifp.newItems());
                    } else
                        break;
                }

                for (Gob gob : pows) {
                    Gob.Overlay ol = (gob.findol(Roastspit.class));
                    String content = ((Roastspit) ol.spr).getContent();
                    if (content == null) {
                        if (!targetItems.isEmpty()) {
                            new PathFinder(gob).run(gui);
                            NUtils.takeItemToHand(targetItems.pollFirst());
                            NUtils.activateRoastspit(ol);
                            NUtils.addTask(new NTask() {
                                @Override
                                public boolean check() {
                                    return NUtils.getGameUI().vhand == null && ((Roastspit) ol.spr).getContent()!=null;
                                }
                            });
                            count--;
                        }
                    }
                }
            }

            if(!new FillFuelPowOrCauldron(pows, 1).run(gui).IsSuccess())
                return Results.FAIL();
            if (!new LightGob(pows, 4).run(gui).IsSuccess())
                return Results.ERROR("I can't start a fire");

        }
        return Results.SUCCESS();
    }
}
