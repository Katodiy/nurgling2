package nurgling.actions.bots;


import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.routes.RoutePoint;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitChipperState;
import nurgling.tools.NAlias;
import nurgling.tools.Finder;
import nurgling.tools.NParser;
import nurgling.widgets.FoodContainer;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MineAction implements Action {
    private class Tile {
        public Coord2d coord;
        public boolean isAvailible = true;

        public Tile (
                Coord2d coord,
                boolean isAvailible
        ) {
            this.coord = coord;
            this.isAvailible = isAvailible;
        }
    }


    @Override
    public ArrayList<Action> getSupp() {
        ArrayList<Action> supps = new ArrayList<>();
        supps.add(new Dropper(FoodContainer.getFoodNames()));
        return supps;
    }

    @Override
    public Results run (NGameUI gui )
            throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg("Please, select area for mining");
        (insa = new SelectArea(Resource.loadsimg("baubles/waterRefiller"))).run(gui);
        area = insa.getRCArea();

        Coord2d pos = new Coord2d ( area.a.x + MCache.tilesz.x / 2, area.a.y + MCache.tilesz.x / 2 );
        ArrayList<Tile> arrayList = new ArrayList<> ();

        for ( double x = pos.x ; x <= area.b.x ; x += MCache.tilesz.x ) {
            for ( double y = pos.y ; y <= area.b.y ; y += MCache.tilesz.y ) {
                arrayList.add ( new Tile ( new Coord2d ( x, y ), true ) );
            }
        }
        while (!arrayList.isEmpty()) {
            arrayList.sort ( new Comparator<Tile>() {
                @Override
                public int compare (
                        Tile lhs,
                        Tile rhs
                ) {
                    double dist1 = gui.map.player ().rc.dist ( lhs.coord );
                    double dist2 = gui.map.player ().rc.dist ( rhs.coord );
                    return Double.compare ( dist1, dist2 );
                }
            } );
            for ( Tile tile : arrayList ) {
                if ( tile.isAvailible ) {
                    Coord tile_pos = (new Coord2d(tile.coord.x / 11, tile.coord.y / 11)).floor();
                    Resource res_beg = gui.ui.sess.glob.map.tilesetr(gui.ui.sess.glob.map.gettile(tile_pos));
                    if (!NUtils.getGameUI().fv.lsrel.isEmpty()) {
                        System.out.println("FIGHT");
                        runToSafe(gui);
                        return Results.FAIL();
//                        return new Results(Results.Types.FIGHT);
                    }
                    Gob looserock;
                    if ((looserock = Finder.findGob(new NAlias("looserock"))) != null && looserock.rc.dist(NUtils.getGameUI().map.player().rc) < 93.5) {
                        //                   return new Results(Results.Types.FIGHT);
                        runToSafe(gui);
                        System.out.println("looserock");
                        return Results.FAIL();
                    }
                    ArrayList<Gob> mss = Finder.findGobs(new NAlias("minebeam", "column", "towercap", "ladder", "minesupport"));
                    for(Gob ms: mss) {

                        if ((NParser.isIt(ms, new NAlias("ladder", "minesupport", "towercap")) && ms.rc.dist(tile.coord) <= 100) ||
                                (NParser.isIt(ms, new NAlias("ladder", "minesupport", "minebeam")) && ms.rc.dist(tile.coord) <= 150) ||
                                (NParser.isIt(ms, new NAlias("column")) && ms.rc.dist(tile.coord) <= 125)
                        ) {
                            GobHealth attr = ms.getattr(GobHealth.class);
                            if(attr!=null) {
                                if (attr.hp <= 0.25) {
                                    System.out.println("attr.hp");
                                    return Results.FAIL();
                                }
                            }
                        }
                    }
                    if ( res_beg != null ) {
                        if ( NParser.checkName ( res_beg.name, new NAlias( "rock", "tiles/cave" ) ) ) {
                            Coord2d target_pos = new Coord2d ( tile.coord.x, tile.coord.y );
                            PathFinder pf = new PathFinder(NGob.getDummy(target_pos, 0, new NHitBox(new Coord2d(-5.5,-5.5),new Coord2d(5.5,5.5))), true);
                            pf.isHardMode = true;
                            pf.run(gui);
                            while(res_beg == gui.ui.sess.glob.map.tilesetr ( gui.ui.sess.glob.map.gettile ( tile_pos ) )) {
                                if(!new RestoreResources(target_pos).run(gui).isSuccess) {
                                    System.out.println("restoreResources111");
                                    return Results.FAIL();
                                }
                                NUtils.mine(tile.coord);
                                gui.map.wdgmsg("sel", tile_pos, tile_pos, 0);

                                if(NUtils.getStamina() > 0.4){
                                    Resource finalRes_beg = res_beg;
                                    NUtils.addTask(new NTask() {
                                        @Override
                                        public boolean check() {
                                            return (finalRes_beg != gui.ui.sess.glob.map.tilesetr ( gui.ui.sess.glob.map.gettile ( tile_pos ) ));
                                        }
                                    });
                                }
                            }
                            NUtils.getDefaultCur();
                            res_beg = gui.ui.sess.glob.map.tilesetr ( gui.ui.sess.glob.map.gettile ( tile_pos ) );
                            if ( res_beg != null ) {
                                if ( !NParser.checkName ( res_beg.name, new NAlias ( "rock", "tiles/cave" ) ) ) {
                                    tile.isAvailible = false;
                                }
                            }
                            Gob bolder = Finder.findGob ( new NAlias ( "bumlings" ) );

                            if ( bolder != null && bolder.rc.dist(gui.map.player().rc)<=15 ) {
                                new PathFinder(bolder).run(gui);
                                while ( Finder.findGob ( bolder.id ) != null ) {
                                    if(!new RestoreResources(bolder.rc).run(gui).isSuccess) {
                                        System.out.println("restoreResources140");
                                        return Results.FAIL();
                                    }
                                    while (Finder.findGob(bolder.id) != null) {
                                        new SelectFlowerAction("Chip stone", bolder).run(gui);
                                        WaitChipperState wcs = new WaitChipperState(bolder);
                                        NUtils.getUI().core.addTask(wcs);
                                        switch (wcs.getState()) {
                                            case BUMLINGNOTFOUND:
                                                break;
                                            case BUMLINGFORDRINK: {
                                                if(!new RestoreResources(bolder.rc).run(gui).isSuccess)
                                                    return Results.FAIL();
                                                break;
                                            }
                                            case DANGER: {
                                                return Results.ERROR("SOMETHING WRONG, STOP WORKING");
                                            }
                                            case TIMEFORPILE: {
                                                if(NUtils.getGameUI().vhand!=null) {
                                                    NUtils.drop(NUtils.getGameUI().vhand);
                                                }
                                                for (WItem item : NUtils.getGameUI().getInventory().getItems(Chipper.stones)) {
                                                    NUtils.drop(item);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            tile.isAvailible = false;
                        }
                    }
                }
                arrayList.remove(tile);
                break;
            }
        }
        System.out.println("NO Tiles" + arrayList.size());
        return Results.SUCCESS();
    }

    void runToSafe(NGameUI gui) throws InterruptedException {
        AutoDrink.waitBot.set(false);
        new RunToSafe().run(gui);
    }

    Pair<Coord2d,Coord2d> area;
}
