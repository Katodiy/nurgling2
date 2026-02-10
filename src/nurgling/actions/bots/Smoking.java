package nurgling.actions.bots;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.NSmokProp;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.SmokingSettings;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static nurgling.widgets.Specialisation.SpecName.logs;

public class Smoking implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SmokingSettings w = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new SmokingSettings()), UI.scale(200,200))));
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        finally {
            if(w!=null)
                w.destroy();
        }
        ArrayList<NSmokProp> allSmokProps = (ArrayList<NSmokProp>)NConfig.get(NConfig.Key.smokeprop);
        if(allSmokProps==null || allSmokProps.isEmpty())
        {
            return Results.ERROR("No config");
        }
        
        // Filter only selected recipes
        ArrayList<NSmokProp> smokProps = new ArrayList<>();
        for(NSmokProp prop : allSmokProps) {
            if(prop.isSelected) {
                smokProps.add(prop);
            }
        }
        
        if(smokProps.isEmpty())
        {
            return Results.ERROR("No active recipes selected");
        }

        NArea.Specialisation slogs = new NArea.Specialisation(Specialisation.SpecName.smokedlog.toString());
        NArea.Specialisation ssmokshed = new NArea.Specialisation(Specialisation.SpecName.smokshed.toString());
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(slogs);
        req.add(ssmokshed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        
        // Check if required areas exist
        if(!new Validator(req, opt).run(gui).IsSuccess()) {
            return Results.FAIL();
        }
        
        // Check global route availability
        NArea smokeArea = NContext.findSpecGlobal(ssmokshed);
        NArea logsArea = NContext.findSpecGlobal(slogs);

        ArrayList<NSmokProp> cands = new ArrayList<>();
        Pair<Coord2d,Coord2d> sheds = smokeArea.getRCArea();
        for(NSmokProp prop : smokProps) {
            // Check if logs area exists (will navigate there later)
            NArea testarea = NContext.findInGlobal(prop.iconName);
            if(logsArea != null && testarea != null) {
                cands.add(prop);
            }
        }
        if(cands.isEmpty()) {
            return Results.ERROR("No logs, or input areas not found");
        }
        HashMap<String,ArrayList<NSmokProp>> fuels = new HashMap<String,ArrayList<NSmokProp>>();
        NContext context = new NContext(gui);
        for(NSmokProp prop : cands) {
            if(!fuels.containsKey(prop.fuel)) {
                fuels.put(prop.fuel, new ArrayList<>());
            }
            fuels.get(prop.fuel).add(prop);
        }

        ArrayList<Container> containers = new ArrayList<>();

        for (Gob sm : Finder.findGobs(sheds, new NAlias("gfx/terobjs/smokeshed"))) {
            if((sm.ngob.getModelAttribute()&16)==16)
                continue;
            Container cand = new Container(sm, "Smoke shed", smokeArea);

            cand.initattr(Container.Space.class);
            cand.initattr(Container.FuelLvl.class);
            cand.getattr(Container.FuelLvl.class).setMaxlvl(5);
            cand.getattr(Container.FuelLvl.class).setAbsMaxlvl(10);
            cand.getattr(Container.FuelLvl.class).setFueltype("Log");
            cand.getattr(Container.FuelLvl.class).setFuelArea(logsArea);
            containers.add(cand);
        }
        if(containers.isEmpty())
        {
            return Results.ERROR("No containers found");
        }

        new FreeContainers(containers).run(gui);

        ArrayList<String> lighted = new ArrayList<>();
        for(String fuel : fuels.keySet()) {
            for(NSmokProp prop : fuels.get(fuel)) {
                new FillContainers(containers,prop.iconName, context).run(gui);
            }
            ArrayList<Container> forRemove = new ArrayList<>();
            ArrayList<Container> forClear = new ArrayList<>();
            for(Container cand : containers) {
                Container.Space space = cand.getattr(Container.Space.class);
                if(space.getFreeSpace()>0 && space.getFreeSpace()!=space.getMaxSpace()) {
                    if (space.getFreeSpace() > space.getMaxSpace() / 2) {
                        forClear.add(cand);
                    }
                } else {
                    if(space.getFreeSpace()!=space.getMaxSpace())
                        forRemove.add(cand);
                }
            }
            new FreeContainers(forClear).run(gui);
            new FuelByLogs(forRemove, fuel).run(gui);
            for(Container cand : forRemove) {
                lighted.add(cand.gobHash);
            }
            containers.removeAll(forRemove);
            for(Container cand : forClear) {
                Container.Space space = cand.getattr(Container.Space.class);
                if(space.getFreeSpace()!=space.getMaxSpace())
                    containers.remove(cand);
            }
        }
        new LightGob(lighted,16).run(gui);
        return Results.SUCCESS();
    }
}
