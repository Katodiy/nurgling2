package nurgling.actions.bots;

import haven.*;
import haven.res.lib.tree.TreeScale;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.conf.NChipperProp;
import nurgling.conf.NChopperProp;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static haven.OCache.posres;

public class Chipper implements Action {

    public static final NAlias stones = new NAlias(new ArrayList<>(List.of(
            "Alabaster", "Apatite", "Arkose", "Basalt", "Bat Rock", "Black Coal", "Black Ore",
            "Bloodstone", "Breccia", "Cassiterite", "Cat Gold", "Chalcopyrite", "Chert", "Cinnabar",
            "Diabase", "Diorite", "Direvein", "Dolomite", "Dross", "Eclogite", "Feldspar", "Flint",
            "Fluorospar", "Gabbro", "Galena", "Gneiss", "Granite", "Graywacke", "Greenschist",
            "Heavy Earth", "Horn Silver", "Hornblende", "Iron Ochre", "Jasper", "Korund", "Kyanite",
            "Lava Rock", "Lead Glance", "Leaf Ore", "Limestone", "Malachite", "Marble", "Meteorite",
            "Mica", "Microlite", "Obsidian", "Olivine", "Orthoclase", "Peacock Ore", "Pegmatite",
            "Porphyry", "Pumice", "Quarryartz", "Quartz", "Rhyolite", "Rock Crystal", "Sandstone",
            "Schist", "Schrifterz", "Serpentine", "Shard of Conch", "Silvershine", "Slag", "Slate",
            "Soapstone", "Sodalite", "Sunstone", "Wine Glance", "Zincspar"
    )));

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.ChipperWnd w = null;
        NChipperProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.ChipperWnd()), UI.scale(200,200))));
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
        if((!prop.plateu && prop.tool == null))
        {
            return Results.ERROR("Not set required tools");
        }
        SelectArea insa;
        if(prop.plateu)
            NUtils.getGameUI().msg("Please select plateu area for chipping");
        else
            NUtils.getGameUI().msg("Please select area for chipping");
        (insa = new SelectArea()).run(gui);

        if(prop.plateu) {
            if (!new Equip(new NAlias("Pickaxe")).run(gui).IsSuccess())
                return Results.ERROR("Equipment not found: " + "Pickaxe");
        }
        else {
            if (!new Equip(new NAlias(prop.tool)).run(gui).IsSuccess())
                return Results.ERROR("Equipment not found: " + prop.tool);
        }
        SelectArea psa = null;
        if(!prop.nopiles)
        {
            NUtils.getGameUI().msg("Please select area for piles");
            (psa = new SelectArea()).run(gui);
        }

        if(!prop.plateu) {
            NAlias pattern = new NAlias(new ArrayList<String>(List.of("gfx/terobjs/bumlings")));

            ArrayList<Gob> bumlings;
            while (!(bumlings = Finder.findGobs(insa.getRCArea(), pattern)).isEmpty()) {
                bumlings.sort(NUtils.d_comp);

                Gob bumling = bumlings.get(0);

                new PathFinder(bumling).run(gui);

                while (Finder.findGob(bumling.id) != null) {
                    new SelectFlowerAction("Chip stone", bumling).run(gui);
                    if (prop.tool.equals("Pickaxe")) {
                        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/pickan"));
                    } else {
                        WItem item = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx);
                        if (item != null && NParser.checkName(((NGItem) item.item).name(), "Pickaxe"))
                            NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/chipping"));
                        else
                            NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/pickan"));
                    }
                    WaitChipperState wcs = new WaitChipperState(bumling, prop);
                    NUtils.getUI().core.addTask(wcs);
                    switch (wcs.getState()) {
                        case BUMLINGNOTFOUND:
                            break;
                        case BUMLINGFORDRINK: {
                            if (prop.autorefill) {
                                if (FillWaterskins.checkIfNeed())
                                    new FillWaterskins().run(gui);
                                new PathFinder(bumling).run(gui);
                            }
                            new Drink(0.9).run(gui);
                            break;
                        }
                        case BUMLINGFOREAT: {
                            new AutoEater().run(gui);
                            break;
                        }
                        case DANGER: {
                            return Results.ERROR("SOMETHING WRONG, STOP WORKING");
                        }
                        case TIMEFORPILE:
                        {
                            if(!prop.nopiles)
                                new TransferToPiles(psa.getRCArea(),stones).run(gui);
                            else
                                for(WItem item : NUtils.getGameUI().getInventory().getItems(stones))
                                {
                                    NUtils.drop(item);
                                }
                            new PathFinder(bumling).run(gui);
                        }
                    }
                }
            }
            if(!prop.nopiles)
                new TransferToPiles(psa.getRCArea(),stones).run(gui);
        }
        else
        {
            Coord2d mountain = NUtils.findMountain(insa.getRCArea());
            if(mountain == null)
            {
                return Results.ERROR(" no mountain terrain");
            }
            new PathFinder( mountain ).run (gui);
            do {
                NUtils.dig();
                NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/pickaxeanspot"));
                WaitPlateuState wcs = new WaitPlateuState(prop);
                NUtils.getUI().core.addTask(wcs);
                switch (wcs.getState()) {
                    case BUMLINGFORDRINK: {
                        if (prop.autorefill) {
                            if (FillWaterskins.checkIfNeed())
                                new FillWaterskins().run(gui);
                            new PathFinder(mountain).run(gui);
                        }
                        new Drink(0.9).run(gui);
                        break;
                    }
                    case BUMLINGFOREAT: {
                        new AutoEater().run(gui);
                        break;
                    }
                    case DANGER: {
                        return Results.ERROR("SOMETHING WRONG, STOP WORKING");
                    }
                    case TIMEFORPILE:
                    {
                        if(!prop.nopiles)
                            new TransferToPiles(psa.getRCArea(),stones).run(gui);
                        else
                            for(WItem item : NUtils.getGameUI().getInventory().getItems(stones))
                            {
                                NUtils.drop(item);
                            }
                        new PathFinder(mountain).run(gui);
                    }
                }
            }
            while (true);
        }

        return Results.SUCCESS();
    }
}
