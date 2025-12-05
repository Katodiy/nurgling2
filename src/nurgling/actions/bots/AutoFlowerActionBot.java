package nurgling.actions.bots;

import haven.*;
import nurgling.NFlowerMenu;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.conf.NAutoFlowerActionProp;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.widgets.bots.AutoFlowerAction;

import java.util.ArrayList;

import static haven.OCache.posres;

public class AutoFlowerActionBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        AutoFlowerAction w = null;
        NAutoFlowerActionProp prop = null;
        
        try {
            NUtils.getUI().core.addTask(new WaitCheckable(NUtils.getGameUI().add((w = new AutoFlowerAction()), UI.scale(200, 200))));
            prop = w.prop;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            if (w != null)
                w.destroy();
        }
        
        if (prop == null) {
            return Results.ERROR("No config");
        }
        
        if (prop.action == null || prop.action.isEmpty()) {
            return Results.ERROR("Action name is empty");
        }

        NContext context = new NContext(gui);
        
        SelectArea selectArea;
        NUtils.getGameUI().msg("Please select area for objects");
        (selectArea = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);
        
        SelectGob selgob;
        NUtils.getGameUI().msg("Please select target object");
        (selgob = new SelectGob(Resource.loadsimg("baubles/selectobject"))).run(gui);
        Gob targetGob = selgob.result;
        
        if (targetGob == null) {
            return Results.ERROR("Target object not selected");
        }

        ArrayList<Gob> gobs = new ArrayList<>();
        for (Gob gob : Finder.findGobs(selectArea.getRCArea(), null)) {
            if (gob.ngob.name.equals(targetGob.ngob.name)) {
                gobs.add(gob);
            }
        }


        if (gobs.isEmpty()) {
            NUtils.getGameUI().msg("No objects found in selected area");
            return Results.SUCCESS();
        }
        
        NUtils.getGameUI().msg("Found " + gobs.size() + " objects to process");

        gobs.sort(NUtils.y_min_comp);
        
        for (Gob gob : gobs) {
            if (!PathFinder.isAvailable(gob)) {
                continue;
            }
            
            while (Finder.findGob(gob.id) != null) {
                if (prop.transfer && NUtils.getGameUI().getInventory().getFreeSpace() < 3) {
                    new FreeInventory2(context).run(gui);
                    
                    if (NUtils.getGameUI().getInventory().getFreeSpace() < 3) {
                        return Results.ERROR("Not enough free space in inventory");
                    }
                }
                
                new PathFinder(gob).run(gui);
                
                NUtils.rclickGob(gob);
                NFlowerMenu fm = NUtils.getFlowerMenu();
                
                if (fm == null) {
                    break;
                }
                
                if (fm.nopts == null || fm.nopts.length == 0) {
                    fm.wdgmsg("cl", -1);
                    NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                    break;
                }
                
                boolean actionFound = false;
                for (NFlowerMenu.NPetal petal : fm.nopts) {
                    if (petal.name.equals(prop.action)) {
                        actionFound = true;
                        fm.wdgmsg("cl", -1);
                        NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                        break;
                    }
                }
                
                if (!actionFound) {
                    fm.wdgmsg("cl", -1);
                    NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                    break;
                }
                
                new SelectFlowerAction(prop.action, gob).run(gui);
                
                Gob player = NUtils.player();
                if (player != null) {
                    NUtils.getUI().core.addTask(new WaitPoseExclude(player, "idle"));
                    
                    WaitAutoFlowerActionState waitState = new WaitAutoFlowerActionState(player, prop.transfer, 3);
                    NUtils.getUI().core.addTask(waitState);
                    
                    if (waitState.getState() == WaitAutoFlowerActionState.State.NEED_TRANSFER) {
                        new FreeInventory2(context).run(gui);
                        
                        if (NUtils.getGameUI().getInventory().getFreeSpace() < 3) {
                            return Results.ERROR("Not enough free space in inventory after transfer");
                        }
                    }
                }
            }
        }
        
        if (prop.transfer) {
            new FreeInventory2(context).run(gui);
        }
        
        NUtils.getGameUI().msg("Auto flower action completed for all objects");
        return Results.SUCCESS();
    }
}
