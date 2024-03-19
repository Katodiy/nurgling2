package nurgling.widgets;

import haven.*;
import nurgling.NUtils;

import java.util.Map;
import java.util.TreeMap;

import static haven.MCache.cmaps;

public class NMiniMap extends MiniMap implements Console.Directory {
    public int scale = 1;
    public NMiniMap(Coord sz, MapFile file) {
        super(sz, file);
        follow(new MapLocator(NUtils.getGameUI().map));
    }

    public NMiniMap(MapFile file) {
        super(file);
    }

    public boolean dragp(int button) {
        return(false);
    }

    public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
        if(mark.m instanceof MapFile.SMarker) {
            Gob gob = MarkerID.find(ui.sess.glob.oc, (MapFile.SMarker)mark.m);
            if(gob != null)
                mvclick(NUtils.getGameUI().map, null, loc, gob, button);
        }
        return(false);
    }

    public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
        if(press) {
            mvclick(NUtils.getGameUI().map, null, loc, icon.gob, button);
            return(true);
        }
        return(false);
    }

    public boolean clickloc(Location loc, int button, boolean press) {
        if(press) {
            mvclick(NUtils.getGameUI().map, null, loc, null, button);
            return(true);
        }
        return(false);
    }

    public void draw(GOut g) {
        // TODO подложка для карты
        //g.image(bg, Coord.z, UI.scale(bg.sz()));
        super.draw(g);
    }
    @Override
    protected float scalef() {
        return(UI.unscale((float)(1 << dlvl))/scale);
    }

    @Override
    public Coord st2c(Coord tc) {
        return(UI.scale(tc.add(sessloc.tc).sub(dloc.tc).div(1 << dlvl)).mul(scale).add(sz.div(2)));
    }

    @Override
    public void drawmap(GOut g) {
        Coord hsz = sz.div(2);
        for(Coord c : dgext) {
            Coord ul = UI.scale(c.mul(cmaps).mul(scale)).sub(dloc.tc.div(scalef())).add(hsz);
            DisplayGrid disp = display[dgext.ri(c)];
            if(disp == null)
                continue;
            drawgrid(g, ul, disp);
        }
    }

    public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
        try {
            Tex img = disp.img();
            if(img != null)
                g.image(img, ul, UI.scale(img.sz()).mul(scale));
        } catch(Loading l) {
        }
    }

    @Override
    public void drawmarkers(GOut g) {
        Coord hsz = sz.div(2);
        for(Coord c : dgext) {
            DisplayGrid dgrid = display[dgext.ri(c)];
            if(dgrid == null)
                continue;
//            for(NDisplayMarker mark : dgrid.markers(true)) {
//                if(filter(mark))
//                    continue;
//                mark.draw(g, mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz), scale, ui, file, big);
//            }
        }
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        if(amount > 0) {
            if(scale > 1) {
                scale--;
            } else
            if(allowzoomout())
                zoomlevel = Math.min(zoomlevel + 1, dlvl + 1);
        } else {
            if(zoomlevel == 0 && scale < 4) {
                scale++;
            }
            zoomlevel = Math.max(zoomlevel - 1, 0);
        }
        return(true);
    }

    protected boolean allowzoomout() {
        if(zoomlevel >= 5)
            return(false);
        return(super.allowzoomout());
    }
    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
        cmdmap.put("rmseg", new Console.Command() {
            public void run(Console cons, String[] args) {
                MiniMap.Location loc = curloc;
                if(loc != null) {
                    try(Locked lk = new Locked(file.lock.writeLock())) {
                        file.segments.remove(loc.seg.id);
                    }
                }
            }
        });
    }
    public Map<String, Console.Command> findcmds() {
        return(cmdmap);
    }
}