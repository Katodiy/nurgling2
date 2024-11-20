package nurgling.actions;

import haven.*;
import haven.res.ui.invsq.InvSquare;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitLifted;
import nurgling.tasks.WaitWindow;
import nurgling.tools.Finder;

import static haven.OCache.posres;
import static nurgling.tools.Finder.findLiftedbyPlayer;

public class TakeFromVehicle implements Action {
    private final Gob vehicle;

    public TakeFromVehicle(Gob vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        new PathFinder(vehicle).run(gui);
        if (vehicle.ngob.name.contains("cart")) {
            int mul = 4;
            for (int i = 0; i < 6; i++) {
                if ((vehicle.ngob.getModelAttribute() & mul) == mul) {
                    gui.map.wdgmsg("click", Coord.z, vehicle.rc.floor(posres), 3, 0, 0, (int) vehicle.id, vehicle.rc.floor(posres),
                            0, i+2);
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return Finder.findLiftedbyPlayer() != null;
                        }
                    });
                    return Results.SUCCESS();
                }
                mul *= 2;
            }
        }
        else if(vehicle.ngob.name.contains("snekkja")) {
            new PathFinder(vehicle).run(gui);
            new SelectFlowerAction("Cargo", vehicle).run(gui);
            NUtils.addTask(new WaitWindow("Snekkja"));
            for (Widget widget : NUtils.getGameUI().getWindow("Snekkja").children()) {
                if (widget.children().size() >= 16) {
                    for (Widget child : widget.children()) {
                        if (!(child instanceof InvSquare)) {
                            child.wdgmsg("click", UI.scale(15,15),1,0);
                            NUtils.addTask(new NTask() {
                                @Override
                                public boolean check() {
                                    return Finder.findLiftedbyPlayer() != null;
                                }
                            });
                            return Results.SUCCESS();
//                            for(Widget target : widget.children()) {
//                                if(target instanceof InvSquare) {
//                                    if(target.c.equals(child.c.x-1,child.c.y-1)) {
//                                        target.wdgmsg("click", UI.scale(15,15),1,0);
//                                        NUtils.addTask(new NTask() {
//                                            @Override
//                                            public boolean check() {
//                                                return Finder.findLiftedbyPlayer() != null;
//                                            }
//                                        });
//                                        return Results.SUCCESS();
//                                    }
//                                }
//                            }

                        }
                    }
                }
            }
        }
        return Results.FAIL();
    }
}