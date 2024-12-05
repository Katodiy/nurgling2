package nurgling.actions;


import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import static haven.OCache.posres;

public class PatrolArea implements Action {


    public enum Type {
        Center, Back
    }

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        Gob vehicle =  Finder.findGob ( name );
        if ( !(new LiftObject ( vehicle ).run ( gui ).IsSuccess())) {
            return Results.FAIL();
        }

        Coord2d pos = new Coord2d ( area.a.x + MCache.tilesz.x / 2, area.a.y + MCache.tilesz.x / 2 );

        new PlaceObject(vehicle, pos, 0).run(gui);

        if( NUtils.getStamina() <= 0.3) {
            if(!new Drink ( 0.9, false ).run ( gui ).IsSuccess())
                return Results.FAIL();
        }

        new TakeVehicle ( vehicle ).run ( gui );
        gui.map.wdgmsg ( "click", Coord.z, pos.floor ( posres ), 1, 0 );
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return Math.sqrt ( Math.pow ( vehicle.rc.x - pos.x, 2 ) +
                        Math.pow ( vehicle.rc.y - pos.y, 2 ) ) <= 1 ;
            }
        });

        while ( pos.x <= area.b.x ) {
            while ( pos.y < area.b.y - MCache.tilesz.x / 2 ) {
                pos.y += MCache.tilesz.y;
                gui.map.wdgmsg ( "click", Coord.z, pos.floor ( posres ), 1, 0 );
                NUtils.addTask(new NTask() {
                    int count = 0;
                    @Override
                    public boolean check() {
                        if(count++ > 200 && !NUtils.player().pose().contains("walking")) {
                            return true;
                        }
                        return Math.sqrt(Math.pow(vehicle.rc.x - pos.x, 2) +
                                Math.pow(vehicle.rc.y - pos.y, 2)) <= 1;
                    }
                });
                if (NUtils.getStamina() <= 0.3) {
                    if (!new Drink(0.9, false).run(gui).IsSuccess())
                        return Results.FAIL();
                    new TakeVehicle(vehicle).run(gui);
                }
            }

            pos.x += MCache.tilesz.x;
            if ( pos.x < area.b.x ) {
                gui.map.wdgmsg ( "click", Coord.z, pos.floor ( posres ), 1, 0 );
                NUtils.addTask(new NTask() {
                    int count = 0;
                    @Override
                    public boolean check() {
                        if(count++ > 200 && !NUtils.player().pose().contains("walking")) {
                            return true;
                        }
                        return Math.sqrt ( Math.pow ( vehicle.rc.x - pos.x, 2 ) +
                                Math.pow ( vehicle.rc.y - pos.y, 2 ) ) <= 1 ;
                    }
                });

                if( NUtils.getStamina() <= 0.3) {
                    if(!new Drink ( 0.9, false ).run ( gui ).IsSuccess())
                        return Results.FAIL();
                    new TakeVehicle ( vehicle ).run ( gui );
                }
            }
            else {
                break;
            }
            while ( pos.y > area.a.y + MCache.tilesz.y / 2 ) {
                pos.y -= MCache.tilesz.y;
                gui.map.wdgmsg ( "click", Coord.z, pos.floor ( posres ), 1, 0 );
                NUtils.addTask(new NTask() {
                    int count = 0;
                    @Override
                    public boolean check() {
                        if(count++ > 200 && !NUtils.player().pose().contains("walking")) {
                            return true;
                        }
                        return  Math.sqrt ( Math.pow ( vehicle.rc.x - pos.x, 2 ) +
                                Math.pow ( vehicle.rc.y - pos.y, 2 ) ) <= 1 ;
                    }
                });
                if( NUtils.getStamina() <= 0.3) {
                    if(!new Drink ( 0.9, false ).run ( gui ).IsSuccess())
                        return Results.FAIL();
                    new TakeVehicle ( vehicle ).run ( gui );
                }
            }
            pos.x += MCache.tilesz.x;
            if ( pos.x < area.b.x ) {
                gui.map.wdgmsg ( "click", Coord.z, pos.floor ( posres ), 1, 0 );
                NUtils.addTask(new NTask() {
                    int count = 0;
                    @Override
                    public boolean check() {
                        if(count++ > 200 && !NUtils.player().pose().contains("walking")) {
                            return true;
                        }
                        return  Math.sqrt ( Math.pow ( vehicle.rc.x - pos.x, 2 ) +
                                Math.pow ( vehicle.rc.y - pos.y, 2 ) ) <= 1 ;
                    }
                });
                if( NUtils.getStamina() <= 0.3) {
                    if(!new Drink ( 0.9, false ).run ( gui ).IsSuccess())
                        return Results.FAIL();
                    new TakeVehicle ( vehicle ).run ( gui );
                }
            }
            else {
                break;
            }
        }

        return Results.SUCCESS();
    }

    public PatrolArea(
            NAlias name,
            Pair<Coord2d,Coord2d> area
    ) {
        this.name = name;
        this.area = area;
    }

    NAlias name;
    Pair<Coord2d,Coord2d> area;
}
