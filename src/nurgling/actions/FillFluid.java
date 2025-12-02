package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.routes.RoutePoint;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitTargetSize;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

import static nurgling.tools.Finder.findLiftedbyPlayer;

public class FillFluid implements Action
{
    ArrayList<Container> conts;
    Pair<Coord2d, Coord2d> area;
    NAlias content;
    int mask;
    String target = null;
    NContext context = null;
    Specialisation.SpecName targetAreaName = null;

    public FillFluid(ArrayList<Container> conts, Pair<Coord2d, Coord2d> area, NAlias content, int mask)
    {
        this.conts = conts;
        this.area = area;
        this.content = content;
        this.mask = mask;
    }

    public FillFluid(Gob target, NContext context, Specialisation.SpecName targetAreaName, NAlias content, int mask)
    {
        this.conts = null;
        this.content = content;
        this.context = context;
        this.mask = mask;
        this.target = target.ngob.hash;
        this.targetAreaName = targetAreaName;
    }

    // Constructor for garden pots - no mask, fills until pot stops accepting water
    public FillFluid(ArrayList<Gob> gobs, NContext context, Specialisation.SpecName targetAreaName, NAlias content)
    {
        this.conts = null;
        this.content = content;
        this.context = context;
        this.mask = -1; // Sentinel value: no mask mode
        this.targetAreaName = targetAreaName;
        this.gobsToFill = gobs;
    }

    private ArrayList<Gob> gobsToFill = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        // Handle garden pots mode (no mask, fill until pot stops accepting)
        if (gobsToFill != null) {
            return runGardenPotMode(gui);
        }

        // Проверка, нужно ли вообще наполнять контейнеры
        boolean needToFill = false;
        if (target == null)
        {
            for (Container cont : conts)
            {
                if ((Finder.findGob(cont.gobid).ngob.getModelAttribute() & mask) != mask)
                {
                    needToFill = true;
                    break;
                }
            }
        }
        else
        {
            Gob gt = Finder.findGob(target);
            if (gt!=null && (gt.ngob.getModelAttribute() & mask) != mask)
            {
                needToFill = true;
            }
        }
        // Если наполнять не нужно, возвращаем успех без взаимодействия с бочкой
        if (!needToFill)
        {
            return Results.SUCCESS();
        }
        NArea waterArea = null;
        if (area == null)
        {
            waterArea = NContext.findSpec(Specialisation.SpecName.water.toString());
            if (waterArea == null && context != null)
            {
                waterArea = context.getSpecArea(Specialisation.SpecName.water);
                if (waterArea != null)
                {
                    context.navigateToAreaIfNeeded(Specialisation.SpecName.water.toString());
                }
            }
            if (waterArea == null)
            {
                return Results.ERROR("Water area not found");
            }
            area = waterArea.getRCArea();
        }


        Gob barrel = Finder.findGob(area, new NAlias("barrel"));
        if (barrel == null)
        {
            return Results.ERROR("barrel not found");
        }

        Coord2d barrelOriginalPos = barrel.rc;

        new LiftObject(barrel).run(gui);
        if (!NUtils.isOverlay(barrel, content))
        {
            if (!new RefillInCistern(area, content).run(gui).IsSuccess())
            {
                new PlaceObject(barrel, barrelOriginalPos, 0).run(gui);
                return Results.FAIL();
            }
        }


        if (target == null)
        {
            for (Container cont : conts)
            {
                while ((Finder.findGob(cont.gobid).ngob.getModelAttribute() & mask) != mask)
                {
                    new PathFinder(Finder.findGob(cont.gobid)).run(gui);
                    NUtils.activateGob(Finder.findGob(cont.gobid));
                    NUtils.addTask(new NTask()
                    {
                        @Override
                        public boolean check()
                        {
                            if (!NUtils.isOverlay(barrel, content))
                                return true;
                            return (Finder.findGob(cont.gobid).ngob.getModelAttribute() & mask) == mask;
                        }
                    });
                    if (!NUtils.isOverlay(barrel, content))
                    {
                        if (!new RefillInCistern(area, content).run(gui).IsSuccess())
                        {
                            new PlaceObject(barrel, barrelOriginalPos, 0).run(gui);
                            return Results.FAIL();
                        }
                    }
                }
            }
        }
        else
        {
            context.navigateToAreaIfNeeded(targetAreaName.toString());

            while (((Finder.findGob(target).ngob.getModelAttribute() & mask) != mask))
            {
                Gob gt = Finder.findGob(target);
                new PathFinder(gt).run(gui);
                NUtils.activateGob(gt);
                NUtils.addTask(new NTask()
                {
                    @Override
                    public boolean check()
                    {
                        if (!NUtils.isOverlay(barrel, content))
                            return true;
                        return (gt.ngob.getModelAttribute() & mask) == mask;
                    }
                });
                if (!NUtils.isOverlay(barrel, content))
                {
                    context.navigateToAreaIfNeeded(Specialisation.SpecName.water.toString());
                    if (!new RefillInCistern(area, content).run(gui).IsSuccess())
                    {
                        Gob placed = findLiftedbyPlayer();
                        if ( placed != null )
                        {
                            Coord2d pos = Finder.getFreePlace(area, placed);
                            new PlaceObject(placed, pos, 0).run(gui);
                        }

                        return Results.FAIL();
                    }
                    context.navigateToAreaIfNeeded(targetAreaName.toString());
                }
            }
        }

        if(context!=null)
        {
            context.navigateToAreaIfNeeded(Specialisation.SpecName.water.toString());
            Gob placed = findLiftedbyPlayer();
            if ( placed != null )
            {
                Coord2d pos = Finder.getFreePlace(area, placed);
                new PlaceObject(placed, pos, 0).run(gui);
            }

            context.navigateToAreaIfNeeded(targetAreaName.toString());
        }
        else
        {
            new PlaceObject(barrel, barrelOriginalPos, 0).run(gui);
        }
        return Results.SUCCESS();
    }

    // Garden pot mode: fill each gob until it stops accepting water (timeout-based detection)
    private Results runGardenPotMode(NGameUI gui) throws InterruptedException {
        if (gobsToFill == null || gobsToFill.isEmpty()) {
            return Results.SUCCESS();
        }

        // Get water area and lift barrel
        NArea waterArea = context.getSpecArea(Specialisation.SpecName.water);
        if (waterArea == null) {
            return Results.ERROR("Water area not found");
        }
        area = waterArea.getRCArea();

        Gob barrel = Finder.findGob(area, new NAlias("barrel"));
        if (barrel == null) {
            return Results.ERROR("Barrel not found in water area");
        }

        new LiftObject(barrel).run(gui);
        if (!NUtils.isOverlay(barrel, content)) {
            if (!new RefillInCistern(area, content).run(gui).IsSuccess()) {
                Gob placed = findLiftedbyPlayer();
                if (placed != null) {
                    Coord2d pos = Finder.getFreePlace(area, placed);
                    new PlaceObject(placed, pos, 0).run(gui);
                }
                return Results.FAIL();
            }
        }

        // Navigate to target area
        context.getSpecArea(targetAreaName);

        // Fill each gob until it stops accepting water
        for (Gob gob : gobsToFill) {
            // Navigate to pot once
            PathFinder pf = new PathFinder(gob);
            pf.isHardMode = true;
            pf.run(gui);

            boolean gobFull = false;
            while (!gobFull) {
                long markerBefore = gob.ngob.getModelAttribute();

                // Check if pot already has water (marker 1 or 3)
                if (markerBefore == 1 || markerBefore == 3) {
                    break; // Pot already has water, move to next
                }

                NUtils.activateGob(gob);

                WaitMarkerChangeWithTimeout waitTask = new WaitMarkerChangeWithTimeout(gob, markerBefore);
                NUtils.addTask(waitTask);

                // Check if barrel is empty
                if (!NUtils.isOverlay(barrel, content)) {
                    // Barrel empty - refill
                    context.getSpecArea(Specialisation.SpecName.water);
                    if (!new RefillInCistern(area, content).run(gui).IsSuccess()) {
                        // No more water available
                        Gob placed = findLiftedbyPlayer();
                        if (placed != null) {
                            Coord2d pos = Finder.getFreePlace(area, placed);
                            new PlaceObject(placed, pos, 0).run(gui);
                        }
                        return Results.SUCCESS(); // Filled what we could
                    }
                    context.getSpecArea(targetAreaName);
                    continue; // Retry this gob
                }

                // Check if marker changed - if still the same, pot is full
                if (gob.ngob.getModelAttribute() == markerBefore) {
                    gobFull = true;
                }
                // If marker changed, continue filling (gob accepted water)
            }
        }

        // Put barrel back in water area
        context.getSpecArea(Specialisation.SpecName.water);
        Gob placed = findLiftedbyPlayer();
        if (placed != null) {
            Coord2d pos = Finder.getFreePlace(area, placed);
            new PlaceObject(placed, pos, 0).run(gui);
        }

        return Results.SUCCESS();
    }

    // Task that waits for marker to change (max 100 frames)
    private static class WaitMarkerChangeWithTimeout extends NTask {
        private final Gob gob;
        private final long originalMarker;
        private int counter = 0;

        public WaitMarkerChangeWithTimeout(Gob gob, long originalMarker) {
            this.gob = gob;
            this.originalMarker = originalMarker;
        }

        @Override
        public boolean check() {
            counter++;
            // Timeout after 100 frames
            if (counter >= 100) {
                return true;
            }
            return gob.ngob.getModelAttribute() != originalMarker;
        }
    }
}
