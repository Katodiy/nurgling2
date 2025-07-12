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

import java.util.ArrayList;

public class TransferBarrelToWorkstation implements Action{
    NContext context;
    String item;

    public TransferBarrelToWorkstation(NContext context, String item)
    {
        this.context = context;
        this.item = item;
    }
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob barrel = context.getBarrelInArea(item);
        if(barrel == null)
            return Results.FAIL();
        context.barrelsid.add(barrel.id);
        new LiftObject(barrel).run(gui);
        long barrelId = barrel.id;
        if(context.workstation!=null) {
            NArea area = context.getSpecArea(context.workstation);
            if (context.workstation.selected == -1) {
                if (area == null)
                    return Results.ERROR("NO BARREL");
                Gob ws = null;
                if (context.workstation.station.startsWith("gfx/terobjs/pow")) {
                    ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias("gfx/terobjs/pow"));
                    gobs.sort(NUtils.d_comp);
                    for (Gob gob : gobs) {
                        if ((gob.ngob.getModelAttribute() & 48) == 0) {
                            ws = gob;
                            break;
                        }
                    }
                } else {
                    ws = Finder.findGob(area, new NAlias(context.workstation.station));
                }
                if (ws != null)
                    context.workstation.selected = ws.id;
            }
            Gob ws = Finder.findGob(context.workstation.selected);
            ArrayList<Coord2d> points = PathFinder.getNearestHardPoints(ws);
            if (ws == null)
                return Results.ERROR("NO WORKSTATION in area");
            for (Coord2d point : points) {
                Coord2d dir = (ws.rc.sub(point)).norm();
                if (dir.x == 0) {
                    if (dir.y > 0) {
                        Coord2d targetpos = new Coord2d(ws.rc.x, ws.rc.y - ws.ngob.hitBox.end.y - barrel.ngob.hitBox.end.y);
                        Coord2d ul = targetpos.sub(4.5 + barrel.ngob.hitBox.end.x * 2, 0);

                        Coord2d pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                        if (pos != null) {
                            new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                        } else {
                            ul = targetpos.add(-4.5, 0);
                            pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                            if (pos != null) {
                                new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                            }
                        }
                        if (pos != null) {
                            context.workstation.targetPoint = new NGlobalCoord(targetpos);
                            break;
                        }
                    }
                    if (dir.y < 0) {
                        Coord2d targetpos = new Coord2d(ws.rc.x, ws.rc.y + ws.ngob.hitBox.end.y);
                        Coord2d ul = targetpos.sub(4.5 + barrel.ngob.hitBox.end.x * 2, 0);

                        Coord2d pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                        if (pos != null) {
                            new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                        } else {
                            ul = targetpos.add(4.5, 0);
                            pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                            if (pos != null) {
                                new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                            }
                        }
                        if (pos != null) {
                            context.workstation.targetPoint = new NGlobalCoord(targetpos);
                            break;
                        }
                    }
                } else if (dir.y == 0) {
                    if (dir.x > 0) {
                        Coord2d targetpos = new Coord2d(ws.rc.x - ws.ngob.hitBox.end.x - barrel.ngob.hitBox.end.x, ws.rc.y);
                        Coord2d ul = targetpos.sub(0, 4.5 + barrel.ngob.hitBox.end.y * 2);

                        Coord2d pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                        if (pos != null) {
                            new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                        } else {
                            ul = targetpos.add(0, -4.5);
                            pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                            if (pos != null) {
                                new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                            }
                        }
                        if (pos != null) {
                            context.workstation.targetPoint = new NGlobalCoord(targetpos);
                            break;
                        }
                    }
                    if (dir.x < 0) {
                        Coord2d targetpos = new Coord2d(ws.rc.x + ws.ngob.hitBox.end.x, ws.rc.y);
                        Coord2d ul = targetpos.sub(0, 4.5 + barrel.ngob.hitBox.end.y * 2);

                        Coord2d pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                        if (pos != null) {
                            new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                        } else {
                            ul = targetpos.add(0, 4.5);
                            pos = Finder.getFreePlace(new Pair<>(ul, ul.add(barrel.ngob.hitBox.end.mul(2))), barrel);
                            if (pos != null) {
                                new PlaceObject(Finder.findGob(barrelId), pos, 0).run(gui);
                            }
                        }
                        if (pos != null) {
                            context.workstation.targetPoint = new NGlobalCoord(targetpos);
                            break;
                        }
                    }
                }
            }
        }
        return Results.SUCCESS();
    }
}
