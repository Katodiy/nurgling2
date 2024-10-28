package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.GoTo;
import nurgling.actions.Results;
import nurgling.conf.NPrepBlocksProp;
import nurgling.conf.NWorldExplorerProp;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import static haven.Coord.of;

public class WorldExplorer implements Action {
    public static Coord[] counterclockwise = {of(1, 0), of(0, 1), of(-1, 0), of(0, -1)};
    public static Coord[][] counternearest = {{of(0, 1),of(1, 1),of(2, 1)}, {of(-1, 0),of(-1, 1),of(-1, 2)}, {of(0, -1),of(-1, -1),of(-2, -1)}, {of(1, 0),of(1, -1),of(1, -2)}};

    public static Coord[] clockwise = {of(1, 0), of(0, -1), of(-1, 0), of(0, 1)};
    public static Coord[][] nearest = {{of(0, -1),of(1, -1),of(2, -1)}, {of(-1, 0),of(-1, -1),of(-1, -2)}, {of(0, 1),of(-1, 1),of(-2, 1)}, {of(1, 0),of(1, 1),of(1, 2)}};

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        nurgling.widgets.bots.WorldExplorerWnd w = null;
        NWorldExplorerProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.WorldExplorerWnd()), UI.scale(200,200))));
            prop = w.prop;
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        finally {
            if(w!=null)
                w.destroy();
        }
        if(prop == null)
        {
            return Results.ERROR("No config");
        }

        Coord[] dirs = (prop.clockwise)?clockwise:counterclockwise;
        Coord[][] neardirs = (prop.clockwise)?nearest:counternearest;
        String targetTile = "odeep";
        String nearestTile = (prop.deeper)?"odeeper":"owater";

        if(!prop.deeper)
        {
            dirs = (prop.clockwise)?counterclockwise:clockwise;
            neardirs = (prop.clockwise)?counternearest:nearest;
        }

        boolean deepFound = false;

        Coord[] buffer = new Coord[100];
        int counter = 0;
        Coord  pltc = NUtils.player().rc.div(MCache.tilesz).floor();
        boolean isStart = false;
        for(int j = 0; j<50;j++) {
            for (int i = 0; i < 4; i++) {
                Coord cand = pltc.add(dirs[i].mul(j));
                Resource res_beg = NUtils.getGameUI().ui.sess.glob.map.tilesetr(NUtils.getGameUI().ui.sess.glob.map.gettile(cand));
                if (res_beg != null) {
                    if (res_beg.name.endsWith(targetTile)) {
                        boolean isCorrect = false;
                        for (Coord test : neardirs[i]) {
                            Resource testr = NUtils.getGameUI().ui.sess.glob.map.tilesetr(NUtils.getGameUI().ui.sess.glob.map.gettile(cand.add(test)));
                            if (testr != null && testr.name.endsWith(nearestTile)) {
                                deepFound = true;
                                isCorrect = true;
                            }
                        }
                        if (isCorrect) {
                            new GoTo(cand.mul(MCache.tilesz).add(MCache.tilehsz)).run(gui);
                            isStart = true;
                        }
                        if (isStart)
                            break;
                    }
                }
            }
            if(isStart)
                break;
        }

        Coord last = null;
        while (true) {
            pltc = NUtils.player().rc.div(MCache.tilesz).floor();
            boolean isFound = false;

            for (int i = 0; i < 4; i++) {
                Coord cand = pltc.add(dirs[i]);
                boolean skip = false;
                for (Coord check : buffer) {
                    if (check != null && cand.equals(check.x, check.y)) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }
                Resource res_beg = NUtils.getGameUI().ui.sess.glob.map.tilesetr(NUtils.getGameUI().ui.sess.glob.map.gettile(cand));
                if (res_beg != null) {
                    if (res_beg.name.endsWith(targetTile)) {
                        if (last == null || !cand.equals(last.x, last.y)) {
                            boolean isCorrect = false;
                            for(Coord test : neardirs[i])
                            {
                                Resource testr = NUtils.getGameUI().ui.sess.glob.map.tilesetr(NUtils.getGameUI().ui.sess.glob.map.gettile(pltc.add(test)));
                                if(testr != null && testr.name.endsWith(nearestTile))
                                {
                                    deepFound = true;
                                    isCorrect = true;
                                }
                            }
                            if(!deepFound||isCorrect) {
                                new GoTo(cand.mul(MCache.tilesz).add(MCache.tilehsz)).run(gui);
                                buffer[counter++ % 100] = last;
                                last = pltc;
                                isFound = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!isFound && last != null) {
                new GoTo(last.mul(MCache.tilesz).add(MCache.tilehsz)).run(gui);
                buffer[counter++ % 100] = pltc;
            }
        }
    }
}
