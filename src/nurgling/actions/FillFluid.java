package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitTargetSize;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class FillFluid implements Action
{
    ArrayList<Container> conts;
    Pair<Coord2d,Coord2d> area;
    NAlias content;
    int mask;
    boolean forced = false;
    Gob target = null;

    public FillFluid(ArrayList<Container> conts, Pair<Coord2d, Coord2d> area, NAlias content, int mask) {
        this.conts = conts;
        this.area = area;
        this.content = content;
        this.mask = mask;
    }

    public FillFluid(Gob target,  Pair<Coord2d, Coord2d> area, NAlias content, int mask) {
        this.conts = null;
        this.area = area;
        this.content = content;
        this.mask = mask;
        this.target = target;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob barrel = Finder.findGob(area, new NAlias("barrel"));
        if(barrel == null) {
            return Results.ERROR("barrel not found");
        }

        // Проверка, нужно ли вообще наполнять контейнеры
        boolean needToFill = false;
        if (target == null) {
            for (Container cont : conts) {
                if ((cont.gob.ngob.getModelAttribute() & mask) != mask) {
                    needToFill = true;
                    break;
                }
            }
        } else {
            if ((target.ngob.getModelAttribute() & mask) != mask) {
                needToFill = true;
            }
        }

        // Если наполнять не нужно, возвращаем успех без взаимодействия с бочкой
        if (!needToFill) {
            return Results.SUCCESS();
        }

        Coord2d pos = barrel.rc;
        new LiftObject(barrel).run(gui);
        if(!NUtils.isOverlay(barrel,content))
        {
            if(!new RefillInCistern(area,content).run(gui).IsSuccess())
                return Results.FAIL();
        }

        if(!forced) {
            if(target==null) {
                for (Container cont : conts) {
                    while ((cont.gob.ngob.getModelAttribute() & mask) != mask) {
                        new PathFinder(cont.gob).run(gui);
                        NUtils.activateGob(cont.gob);
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                if (!NUtils.isOverlay(barrel, content))
                                    return true;
                                return (cont.gob.ngob.getModelAttribute() & mask) == mask;
                            }
                        });
                        if (!NUtils.isOverlay(barrel, content)) {
                            if (!new RefillInCistern(area, content).run(gui).IsSuccess())
                                return Results.FAIL();
                        }
                    }
                }
            }
            else
            {
                while ((target.ngob.getModelAttribute() & mask) != mask) {
                    new PathFinder(target).run(gui);
                    NUtils.activateGob(target);
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            if (!NUtils.isOverlay(barrel, content))
                                return true;
                            return (target.ngob.getModelAttribute() & mask) == mask;
                        }
                    });
                    if (!NUtils.isOverlay(barrel, content)) {
                        if (!new RefillInCistern(area, content).run(gui).IsSuccess())
                            return Results.FAIL();
                    }
                }
            }
        }
        new PlaceObject(barrel,pos,0).run(gui);
        return Results.SUCCESS();
    }
}