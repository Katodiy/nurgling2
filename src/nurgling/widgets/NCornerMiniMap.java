package nurgling.widgets;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.FogArea;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class NCornerMiniMap extends NMiniMap implements Console.Directory {

    public NCornerMiniMap(Coord sz, MapFile file) {
        super(sz, file);
        follow(new MapLocator(NUtils.getGameUI().map));
    }

    public NCornerMiniMap(MapFile file) {
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

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (dloc != null) {
            DisplayIcon icon = iconat(c);
            if (icon != null) {
                Resource res = icon.icon.res;
                if(res == null)
                    return null;
                Resource.Tooltip tt = res.layer(Resource.tooltip);
                if (tt != null) {
                    String name = tt.t;
                    return Text.render(name).tex();
                }
            }
            Coord tc = c.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
            DisplayMarker mark = markerat(tc);
            if (mark != null) {
                return (mark.tip);
            }
        }
        return (super.tooltip(c, prev));
    }
}