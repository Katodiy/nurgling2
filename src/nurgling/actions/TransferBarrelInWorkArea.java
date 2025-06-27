package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.areas.NGlobalCoord;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

public class TransferBarrelInWorkArea implements Action{
    NContext context;
    String item;

    public TransferBarrelInWorkArea(NContext context, String item)
    {
        this.context = context;
        this.item = item;
    }
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob barrel = context.getBarrelInArea(item);
        if(barrel == null)
            return Results.FAIL();
        new LiftObject(barrel).run(gui);
        long barrelId = barrel.id;
        if(context.workstation==null)
        {
            NArea area = context.getSpecArea(Specialisation.SpecName.barrelworkarea);
            if(area.getRCArea()!=null);
            {
                int count = Finder.findGobs(area,new NAlias("barrel")).size();

                Pair<Coord2d, Coord2d> rcarea = area.getRCArea();
                Coord2d center = rcarea.b.sub(rcarea.a).div(2).add(rcarea.a);
                Coord2d bshift = barrel.ngob.hitBox.end.sub(barrel.ngob.hitBox.begin).div(2);
                if(count == 0)
                {
                    new PlaceObject(Finder.findGob(barrelId),center.sub(4.5,4.5).sub(bshift),0).run(gui);
                }
                else if(count == 1)
                {
                    new PlaceObject(Finder.findGob(barrelId),center.sub(4.5,-4.5).sub(bshift.x,-bshift.y),0).run(gui);
                }
                else if(count == 2)
                {
                    new PlaceObject(Finder.findGob(barrelId),center.sub(4.5,4.5).sub(-bshift.x,bshift.y),0).run(gui);
                }
                else if(count == 3)
                {
                    new PlaceObject(Finder.findGob(barrelId),center.sub(4.5,-4.5).sub(-bshift.x,-bshift.y),0).run(gui);
                }
            }
        }
        return Results.SUCCESS();
    }
}
