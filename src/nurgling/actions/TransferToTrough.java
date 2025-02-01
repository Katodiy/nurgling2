package nurgling.actions;

import haven.Coord2d;
import haven.GItem;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.ChangeModelAtrib;
import nurgling.tasks.FilledTrough;
import nurgling.tasks.WaitItems;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

public class TransferToTrough implements Action {
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {

            ArrayList<WItem> witems;

            while(!(witems = gui.getInventory ().getItems( items )).isEmpty()) {
                new PathFinder(trough).run(gui);
                if(trough.ngob.getModelAttribute()==7 )
                {
                    if(NUtils.getGameUI().vhand != null){
                        NUtils.drop(NUtils.getGameUI().vhand);
                        return Results.SUCCESS();
                    }
                    if(cistern!=null) {
                        if (NUtils.getGameUI().vhand != null) {
                            gui.getInventory().dropOn(gui.getInventory().findFreeCoord(NUtils.getGameUI().vhand));
                        }
                        Coord2d pos = trough.rc;
                        double a = trough.a;
                        new LiftObject(trough).run(gui);
                        new PathFinder ( cistern ).run(gui);
                        NUtils.activateGob ( cistern );
                        NUtils.getUI().core.addTask(new ChangeModelAtrib(trough, 7));
                        new PlaceObject(trough, pos, a).run(gui);
                    }
                }
                NUtils.takeItemToHand(witems.get(0));
                NUtils.dropsame(trough);
                FilledTrough ft = new FilledTrough(trough, items);
                NUtils.getUI().core.addTask(ft);
            }
        return Results.SUCCESS();
        }



    public TransferToTrough(
            Gob gob,
            NAlias items
    )
    {
        this.trough = gob;
        this.items = items;
    }

    public TransferToTrough(
            Gob gob,
            NAlias items,
            Gob cistern
    )
    {
        this(gob,items);
        this.cistern = cistern;
    }

    public boolean isFull(){
        if(trough != null){
            return (trough.ngob.getModelAttribute()==7 );
        }
        return false;
    }

    Gob trough;
    Gob cistern = null;
    NAlias items;
}