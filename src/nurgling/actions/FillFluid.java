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

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
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
            if (!new RefillInCistern(barrel, area, content).run(gui).IsSuccess())
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
                        if (!new RefillInCistern(barrel, area, content).run(gui).IsSuccess())
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
                    if (!new RefillInCistern(barrel, area, content).run(gui).IsSuccess())
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
}
