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
    public String searchPattern = "";
    public Resource.Image searchRes = null;
    public boolean needUpdate = false;
    TextEntry te;
    public NMapWnd(MapFile file, MapView mv, Coord sz, String title) {
        super(file, mv, sz, title);
        searchRes = Resource.local().loadwait("alttex/selectedtex").layer(Resource.imgc);
        add(te = new TextEntry(200,""){
            @Override
            public void done(ReadLine buf) {
                super.done(buf);
                searchPattern = text();
                view.needUpdate = true;
                NUtils.getGameUI().mmap.needUpdate = true;
            }
        }, view.pos("br").sub(UI.scale(200,20)));
    }

    public long playerSegmentId() {
        MiniMap.Location sessloc = view.sessloc;
        if(sessloc == null) {return 0;}
        return sessloc.seg.id;
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

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        if(te!=null)
            te.c = view.pos("br").sub(UI.scale(200,20));
    }
}
