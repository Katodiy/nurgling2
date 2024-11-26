package nurgling.widgets;

import haven.*;
import nurgling.NUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class NMapWnd extends MapWnd {
    private boolean switching = true;

    public NMapWnd(MapFile file, MapView mv, Coord sz, String title) {
        super(file, mv, sz, title);
    }

    public class GobMarker extends MapFile.Marker {
        public final long gobid;
        public final Indir<Resource> res;
        private Coord2d rc = null;
        public final Color col;

        public GobMarker(Gob gob) {
            super(0, gob.rc.floor(tilesz), /*gob.tooltip()*/"");
            this.gobid = gob.id;
            GobIcon icon = gob.getattr(GobIcon.class);
            res = (icon == null) ? null : icon.res;
            col = color(gob);
        }

        private Color color(Gob gob) {
            return Color.LIGHT_GRAY;
        }

        public void update() {
            Gob gob = ui.sess.glob.oc.getgob(gobid);
            if(gob != null) {
                seg = view.sessloc.seg.id;
                try {
                    rc = gob.rc.add(view.sessloc.tc.mul(tilesz));
                    tc = rc.floor(tilesz);
                } catch (Exception ignore) {}
            }
        }

        public Coord2d rc() {
            try {
                return rc.sub(view.sessloc.tc.mul(tilesz));
            } catch (Exception ignore) {}
            return null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(gobid);
        }
    }

    public long playerSegmentId() {
        MiniMap.Location sessloc = view.sessloc;
        if(sessloc == null) {return 0;}
        return sessloc.seg.id;
    }

    public MiniMap.Location playerLocation() {
        return view.sessloc;
    }

    public Coord2d findMarkerPosition(String name) {
        MiniMap.Location sessloc = view.sessloc;
        if(sessloc == null) {return null;}
        for (Map.Entry<Long, MapFile.SMarker> e : file.smarkers.entrySet()) {
            MapFile.SMarker m = e.getValue();
            if(m.seg == sessloc.seg.id && m.nm!= null && name!=null && m.nm.contains(name)) {
                return m.tc.sub(sessloc.tc).mul(tilesz);
            }
        }
        return null;
    }



    public void addMarker(Coord at, String name) {
//        at = at.add(view.sessloc.tc);
//        MapFile.Marker nm = new MapFile.PMarker(view.sessloc.seg.id, at, name, BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)]);
//        file.add(nm);
//        focus(nm);
//        if(ui.modctrl) {
//            ui.gui.track(nm);
//        }
//        domark = false;
    }
}
