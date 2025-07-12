package nurgling.actions.bots;


import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
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

import static haven.MCache.cmaps;
import static haven.OCache.posres;

public class MineAction implements Action {
    private class Tile {
        public Coord oldcoord;
        public long grid_id;
        public boolean isAvailible = true;

        public Tile (
                Coord2d coord,
                boolean isAvailible
        ) {
            Coord pltc = (new Coord2d(coord.x / MCache.tilesz.x, coord.y / MCache.tilesz.y)).floor();
            synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
                if (NUtils.getGameUI().ui.sess.glob.map.grids.containsKey(pltc.div(cmaps))) {
                    MCache.Grid g = NUtils.getGameUI().ui.sess.glob.map.getgridt(pltc);
                    this.oldcoord = (coord.sub(g.ul.mul(Coord2d.of(11, 11)))).floor(posres);
                    grid_id = g.id;
                }
            }
            this.isAvailible = isAvailible;
        }

        public Coord2d getCoord()
        {
            synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
                for (MCache.Grid g : NUtils.getGameUI().ui.sess.glob.map.grids.values()) {
                    if (g.id == grid_id) {
                        return oldcoord.mul(posres).add(g.ul.mul(Coord2d.of(11, 11)));
                    }
                }
            }
            return null;
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
        NContext context = new NContext(gui);
        String marea = context.createArea("Please, select area for mining", Resource.loadsimg("baubles/waterRefiller"));
        area = context.getRCArea(marea);

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
                    double dist1 = gui.map.player ().rc.dist ( lhs.getCoord() );
                    double dist2 = gui.map.player ().rc.dist ( rhs.getCoord() );
                    return Double.compare ( dist1, dist2 );
                }
            } );
            for ( Tile tile : arrayList ) {
                if ( tile.isAvailible ) {
                    Coord tile_pos = (new Coord2d(tile.getCoord().x / 11, tile.getCoord().y / 11)).floor();
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

                        if ((NParser.isIt(ms, new NAlias("ladder", "minesupport", "towercap")) && ms.rc.dist(tile.getCoord()) <= 100) ||
                                (NParser.isIt(ms, new NAlias("ladder", "minesupport", "minebeam")) && ms.rc.dist(tile.getCoord()) <= 150) ||
                                (NParser.isIt(ms, new NAlias("column")) && ms.rc.dist(tile.getCoord()) <= 125)
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
                            Coord2d target_pos = new Coord2d ( tile.getCoord().x, tile.getCoord().y );
                            PathFinder pf = new PathFinder(NGob.getDummy(target_pos, 0, new NHitBox(new Coord2d(-5.5,-5.5),new Coord2d(5.5,5.5))), true);
                            pf.isHardMode = true;
                            pf.run(gui);
                            while(res_beg == gui.ui.sess.glob.map.tilesetr ( gui.ui.sess.glob.map.gettile ( tile_pos ) )) {
                                context.setLastPos(target_pos);
                                if(!new RestoreResources().run(gui).isSuccess) {
                                    System.out.println("restoreResources111");
                                    return Results.FAIL();
                                }
                                new PathFinder(NGob.getDummy(context.getLastPosCoord(marea), 0, new NHitBox(new Coord2d(-5.5,-5.5),new Coord2d(5.5,5.5))),true).run(gui);
                                NUtils.mine(tile.getCoord());
                                tile_pos = (new Coord2d(tile.getCoord().x / 11, tile.getCoord().y / 11)).floor();
                                gui.map.wdgmsg("sel", tile_pos, tile_pos, 0);

                                if(NUtils.getStamina() > 0.4){
                                    Resource finalRes_beg = res_beg;
                                    Coord finalTile_pos = tile_pos;
                                    NUtils.addTask(new NTask() {
                                        @Override
                                        public boolean check() {
                                            return (finalRes_beg != gui.ui.sess.glob.map.tilesetr ( gui.ui.sess.glob.map.gettile (finalTile_pos) ));
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
                                while (bolder != null && Finder.findGob ( bolder.id ) != null ) {
                                    context.setLastPos(bolder.rc);
                                    if(!new RestoreResources().run(gui).isSuccess) {
                                        System.out.println("restoreResources140");
                                        return Results.FAIL();
                                    }
                                    new PathFinder(NGob.getDummy(context.getLastPosCoord(marea), 0, new NHitBox(new Coord2d(-5.5,-5.5),new Coord2d(5.5,5.5))),true).run(gui);
                                    while (bolder!=null && Finder.findGob(bolder.id) != null) {
                                        new SelectFlowerAction("Chip stone", bolder).run(gui);
                                        WaitChipperState wcs = new WaitChipperState(bolder);
                                        NUtils.getUI().core.addTask(wcs);
                                        switch (wcs.getState()) {
                                            case BUMLINGNOTFOUND:
                                                break;
                                            case BUMLINGFORDRINK: {
                                                context.setLastPos(bolder.rc);
                                                if(!new RestoreResources().run(gui).isSuccess)
                                                    return Results.FAIL();
                                                bolder = context.getGob(marea, bolder.id);
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
        NContext.waitBot.set(false);
        new RunToSafe().run(gui);
    }

    Pair<Coord2d,Coord2d> area;
}
