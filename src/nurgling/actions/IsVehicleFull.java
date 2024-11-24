package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.Img;
import haven.Widget;
import haven.res.ui.invsq.InvSquare;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.Container;

import static haven.OCache.posres;

public class IsVehicleFull implements Action
{

    Gob gob;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        int obj = 0;
        if(gob.ngob.name!=null)
        {
            if(gob.ngob.name.contains("cart"))
            {
                int mul = 4;
                for(int i = 0;i<6;i++)
                {
                    if((gob.ngob.getModelAttribute()&mul)==mul)
                        obj++;
                    mul*=2;
                }
                count = 6 - obj;
            }
            else if(gob.ngob.name.contains("snekkja")) {
                new PathFinder(gob).run(gui);
                new SelectFlowerAction("Cargo", gob).run(gui);
                NUtils.addTask(new WaitWindow("Snekkja"));
                for (Widget widget : NUtils.getGameUI().getWindow("Snekkja").children()) {
                    if (widget.children().size() >= 16) {
                        for (Widget child : widget.children()) {
                            if (!(child instanceof InvSquare)) {
                                obj++;
                            }
                        }
                    }
                }
                count = 16 - obj;
            }
            else if(gob.ngob.name.contains("wagon")) {
                new PathFinder(gob).run(gui);
                new SelectFlowerAction("Open", gob).run(gui);
                NUtils.addTask(new WaitWindow("Wagon"));
                for (Widget widget : NUtils.getGameUI().getWindow("Wagon").children()) {
                    if (widget.children().size() >= 20) {
                        for (Widget child : widget.children()) {
                            if (!(child instanceof InvSquare)) {
                                obj++;
                            }
                        }
                    }
                }
                count = 20 - obj;
            }
        }
        return Results.SUCCESS();
    }

    public IsVehicleFull(Gob gob)
    {
        this.gob = gob;
    }

    public int getCount() {
        return count;
    }

    int count = 0;
}
