package nurgling.actions.bots;

import haven.*;
import haven.res.lib.tree.TreeScale;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.conf.NChopperProp;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.bots.Checkable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Chopper implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Chopper w = null;
        NChopperProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.Chopper()), UI.scale(200,200))));
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
        if((prop.stumps && prop.shovel==null) || (prop.tool == null))
        {
            return Results.ERROR("Not set required tools");
        }
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area for deforestation");
        (insa = new SelectArea(Resource.loadsimg("baubles/chopperArea"))).run(gui);
        NAlias pattern = prop.stumps ? new NAlias(new ArrayList<String>(List.of("gfx/terobjs/tree")),new ArrayList<String>(Arrays.asList("log","oldtrunk"))) :
                new NAlias(new ArrayList<String>(List.of("gfx/terobjs/tree")),new ArrayList<String>(Arrays.asList("log", "oldtrunk", "stump")));

        if(!prop.bushes)
        {
            pattern.exceptions.add("bushes");
        }
        else
        {
            pattern.keys.add("gfx/terobjs/bushes");
        }
        ArrayList<Gob> trees;
        while (!(trees = Finder.findGobs(insa.getRCArea(),pattern)).isEmpty()) {
            trees.sort(NUtils.y_min_comp);

            if(prop.ngrowth)
            {
                ArrayList<Gob> for_remove = new ArrayList<>();
                for (Gob tree: trees)
                {
                    if(tree.getattr(TreeScale.class)!=null)
                    {
                        for_remove.add(tree);
                    }
                }
                trees.removeAll(for_remove);
                if(trees.isEmpty())
                    break;
            }

            Gob tree = trees.get(0);

            PathFinder pf = new PathFinder(tree);
            pf.setMode(PathFinder.Mode.Y_MAX);
            pf.run(gui);

            while (Finder.findGob(tree.id) != null) {
                boolean chopped = false;
                if (NParser.isIt(tree, new NAlias("stump"))) {
                    if(!new Equip(new NAlias(prop.shovel)).run(gui).IsSuccess())
                        return Results.ERROR("Equipment not found: " + prop.shovel);
                    new Destroy(tree,"gfx/borka/shoveldig").run(gui);
                } else {
                    chopped = true;
                    if(tree.getattr(TreeScale.class)!=null)
                    {
                        chopped = false;
                    }
                    if(tree.ngob.name.startsWith("gfx/terobjs/bushes"))
                        chopped = false;
                    if(!new Equip(new NAlias(prop.tool)).run(gui).IsSuccess())
                        return Results.ERROR("Equipment not found: " + prop.tool);
                    new SelectFlowerAction("Chop", tree).run(gui);
                    NUtils.getUI().core.addTask(new WaitPoseOrNoGob(NUtils.player(), tree, "gfx/borka/treechop"));
                }
                WaitChopperState wcs = new WaitChopperState(tree, prop);
                NUtils.getUI().core.addTask(wcs);
                switch (wcs.getState()) {
                    case TREENOTFOUND:
                        break;
                    case TIMEFORDRINK: {
                        if (prop.autorefill) {
                            if (FillWaterskins.checkIfNeed())
                                if (!(new FillWaterskins(true).run(gui).IsSuccess()))
                                    return Results.FAIL();
                            pf = new PathFinder(tree);
                            pf.setMode(PathFinder.Mode.Y_MAX);
                            pf.run(gui);
                        }
                        if(!(new Drink(0.9, false).run(gui).IsSuccess()))
                        {
                            if (prop.autorefill) {
                                if (!(new FillWaterskins(true).run(gui).IsSuccess()))
                                    return Results.FAIL();
                            }
                            else
                                return Results.FAIL();
                        }
                        break;
                    }
                    case TIMEFOREAT: {
                        if(!(new AutoEater().run(gui).IsSuccess()))
                            return Results.FAIL();
                        break;
                    }
                    case DANGER:
                        return Results.ERROR("SOMETHING WRONG, STOP WORKING");

                }
                if(chopped && Finder.findGob(tree.id) == null) {
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return Finder.findGob(tree.rc)!=null;
                        }
                    });
                }
            }

        }

        return Results.SUCCESS();
    }
}
