package nurgling.actions.bots;

import haven.*;
import haven.res.lib.tree.TreeScale;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NContext;
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

        NContext context = new NContext(gui);

        String treeArea = context.createArea("Please select area for deforestation", Resource.loadsimg("baubles/chopperArea"));

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
        while (!(trees = context.getGobs(treeArea,pattern)).isEmpty()) {
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
            long treeId = tree.id;
            context.setLastPos(tree.rc);
            PathFinder pf = new PathFinder(tree);
            pf.setMode(PathFinder.Mode.Y_MAX);
            pf.isHardMode = true;
            pf.run(gui);

            while (tree!=null && context.getGob(treeArea, treeId) != null) {
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
                    case TIMEFORDRINK:
                    case TIMEFOREAT: {
                        context.setLastPos(tree.rc);
                        if (!new RestoreResources().run(gui).IsSuccess()) {
                            return Results.ERROR("No Drink or Eat");
                        }
                        tree = context.getGob(treeArea, treeId);

                        break;
                    }
                    case DANGER:
                        return Results.ERROR("SOMETHING WRONG, STOP WORKING");

                }
                if(chopped && context.getGob(treeArea, treeId) == null) {
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            try {
                                return Finder.findGob(context.getLastPosCoord(treeArea))!=null;
                            } catch (InterruptedException e) {
                                return true;
                            }
                        }
                    });
                }
            }

        }
        new RunToSafe().run(gui);
        return Results.SUCCESS();
    }
}
