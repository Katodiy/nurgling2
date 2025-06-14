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

public class NMiniMap extends MiniMap implements Console.Directory {
    public int scale = 1;
    public final FogArea fogArea = new FogArea(this);

    private final List<TempMark> tempMarkList = new ArrayList<TempMark>();

    private static final Coord2d sgridsz = new Coord2d(new Coord(100,100));
    public static class TempMark {
        String name;
        long start;
        final long id;
        Coord2d rc;
        Coord gc;
        BufferedImage icon;

        MiniMap.Location loc;

        public TempMark(String name, MiniMap.Location loc, long id, Coord2d rc, Coord gc, BufferedImage icon) {
            start = System.currentTimeMillis();
            this.name = name;
            this.id = id;
            this.rc = rc;
            this.gc = gc;
            this.icon = icon;
            this.loc = loc;
        }
    }




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

    void drawgrid(GOut g) {
        int zmult = 1 << zoomlevel;
        Coord offset = sz.div(2).sub(dloc.tc.div(scalef()));
        Coord zmaps = cmaps.div( (float)zmult).mul(scale);

        double width = UI.scale(1f);
        Color col = g.getcolor();
        g.chcolor(Color.RED);
        for (int x = dgext.ul.x * zmult; x < dgext.br.x * zmult; x++) {
            Coord a = UI.scale(zmaps.mul(x, dgext.ul.y * zmult)).add(offset);
            Coord b = UI.scale(zmaps.mul(x, dgext.br.y * zmult)).add(offset);
            if(a.x >= 0 && a.x <= sz.x) {
                a.y = Utils.clip(a.y, 0, sz.y);
                b.y = Utils.clip(b.y, 0, sz.y);
                g.line(a, b, width);
            }
        }
        for (int y = dgext.ul.y * zmult; y < dgext.br.y * zmult; y++) {
            Coord a = UI.scale(zmaps.mul(dgext.ul.x * zmult, y)).add(offset);
            Coord b = UI.scale(zmaps.mul(dgext.br.x * zmult, y)).add(offset);
            if(a.y >= 0 && a.y <= sz.y) {
                a.x = Utils.clip(a.x, 0, sz.x);
                b.x = Utils.clip(b.x, 0, sz.x);
                g.line(a, b, width);
            }
        }
        g.chcolor(col);
    }
    public static final Coord _sgridsz = new Coord(100, 100);
    public static final Coord VIEW_SZ = UI.scale(_sgridsz.mul(9).div(tilesz.floor()));
    public static final Color VIEW_BG_COLOR = new Color(255, 255, 255, 60);
    public static final Color VIEW_FOG_COLOR = new Color(255, 255, 0 , 120);
    public static final Color VIEW_BORDER_COLOR = new Color(0, 0, 0, 128);

    public static Coord2d TEMP_VIEW_SZ = new Coord2d(VIEW_SZ).mul(tilesz).div(2).sub(1,1);
    void drawview(GOut g) {
        if(ui.gui.map==null)
            return;
        int zmult = 1 << zoomlevel;
        Coord2d sgridsz = new Coord2d(_sgridsz);
        Gob player = ui.gui.map.player();
        if(player != null) {
            Coord rc = p2c(player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz));
            Coord viewsz = VIEW_SZ.div(zmult).mul(scale);
            g.chcolor(VIEW_BG_COLOR);
            g.frect(rc, viewsz);
            g.chcolor(VIEW_BORDER_COLOR);
            g.rect(rc, viewsz);
            g.chcolor();
        }
    }

    @Override
    public void drawparts(GOut g){
        if(NUtils.getGameUI()==null)
            return;
        drawmap(g);
        drawmarkers(g);
        if(dlvl == 0)
            drawicons(g);
        drawparty(g);
        boolean playerSegment = (sessloc != null) && ((curloc == null) || (sessloc.seg.id == curloc.seg.id));
        if(zoomlevel <= 2 && (Boolean)NConfig.get(NConfig.Key.showGrid)) {drawgrid(g);}
        if(playerSegment && zoomlevel <= 1 && (Boolean)NConfig.get(NConfig.Key.showView)) {drawview(g);}

        if((Boolean) NConfig.get(NConfig.Key.fogEnable)) {
            g.chcolor(VIEW_FOG_COLOR);
            try (Locked lk = new Locked(file.lock.readLock())) {
                for (FogArea.Rectangle rect : fogArea.getCoveredAreas()) {
                    MapFile.GridInfo gi = file.gridinfo.get(rect.ul_id);
                    if (gi != null && curloc.seg.id == rect.seg_id && rect.ul != null && rect.br != null) {
                        g.frect2( p2c(rect.ul.sub(sessloc.tc).mul(tilesz)), p2c(rect.br.sub(sessloc.tc).mul(tilesz)));
                    }
                }
            }
            g.chcolor();
        }
        drawtempmarks(g);
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(ui.gui.map==null)
            return;
        if((Boolean) NConfig.get(NConfig.Key.fogEnable)) {
            if ((sessloc != null) && ((curloc == null) || (sessloc.seg.id == curloc.seg.id))) {
                fogArea.tick(dt);
                int zmult = 1 << zoomlevel;

                Gob player = ui.gui.map.player();
                if (player != null && dloc != null) {
                    Coord ul = player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz).floor(tilesz).add(sessloc.tc);
                    Coord br = ul.add(VIEW_SZ);
                    fogArea.addWithoutOverlaps(ul, br, curloc.seg.id);
                }
            }
        }

        checkTempMarks();
    }

    @Override
    public float scalef() {
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
            for(DisplayMarker mark : dgrid.markers(true)) {
                if(filter(mark))
                    continue;
                mark.draw(g, mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz));
            }
        }
    }

    @Override
    public boolean mousewheel(MouseWheelEvent ev) {
        if(ev.a > 0) {
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

    private void drawtempmarks(GOut g) {
        if((Boolean)NConfig.get(NConfig.Key.tempmark)) {
            Gob player = NUtils.player();
            if (player != null) {
                double zmult = 1 << zoomlevel;
                Coord rc = p2c(player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz));
                Coord viewsz = VIEW_SZ.div(zmult).mul(scale);

                for (TempMark cm : tempMarkList) {
                    if (cm.loc!=null && ui.gui.mmap.curloc.seg.id == cm.loc.seg.id) {
                        if (cm.icon != null) {
                            if (!cm.gc.equals(Coord.z)) {
                                Coord gc = p2c(cm.gc.sub(sessloc.tc).mul(tilesz));

                                int dsz = Math.max(cm.icon.getHeight(), cm.icon.getWidth());
                                if (!gc.isect(rc, viewsz)) {
                                    g.aimage(new TexI(cm.icon), gc, 0.5, 0.5, UI.scale(18 * cm.icon.getWidth() / dsz, 18 * cm.icon.getHeight() / dsz));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void checkTempMarks() {
        if((Boolean)NConfig.get(NConfig.Key.tempmark)) {
            final Coord2d cmap = new Coord2d(cmaps);
            if (NUtils.player() != null) {
                Coord2d pl = NUtils.player().rc;
                final List<TempMark> marks = new ArrayList<>(tempMarkList);
                for (TempMark cm : marks) {
                    Gob g = Finder.findGob(cm.id);
                    if (g == null) {
                        if (System.currentTimeMillis() - cm.start > (Integer) NConfig.get(NConfig.Key.temsmarktime) * 1000 * 60 ) {
                            tempMarkList.remove(cm);
                        } else {
                            if (!cm.rc.isect(pl.sub(cmap.mul((Integer) NConfig.get(NConfig.Key.temsmarkdist)).mul(tilesz)), pl.add(cmap.mul((Integer) NConfig.get(NConfig.Key.temsmarkdist)).mul(tilesz)))) {
                                tempMarkList.remove(cm);
                            }else if (cm.rc.isect(pl.sub(TEMP_VIEW_SZ), pl.add(TEMP_VIEW_SZ))) {
                                tempMarkList.remove(cm);
                            }
                        }
                    } else {
                        cm.start = System.currentTimeMillis();;
                        cm.rc = g.rc;
                        cm.gc = g.rc.floor(tilesz).add(sessloc.tc);
                    }
                }
                synchronized (ui.sess.glob.oc) {
                    for (Gob gob : ui.sess.glob.oc) {
                        if (tempMarkList.stream().noneMatch(m -> m.id == gob.id)) {
                            BufferedImage tex = setTex(gob);
                            MiniMap.Location loc = NUtils.getGameUI().mmap.curloc;
                            if (tex != null && sessloc!=null) {
                                tempMarkList.add(new TempMark(gob.ngob.name, loc, gob.id, gob.rc, gob.rc.floor(tilesz).add(sessloc.tc), tex));
                            }
                        }
                    }
                }
            }
        }
    }

    BufferedImage setTex(Gob gob)
    {
        GobIcon icon = gob.getattr(GobIcon.class);
        if (icon!=null && ui.gui.mmap.iconconf != null && icon.res.isReady() && icon.icon!=null) {
            if (icon.icon().image() != null) {
                GobIcon.Setting conf = ui.gui.mmap.iconconf.get(icon.icon());
                if (conf != null && conf.show)
                {
                    return icon.icon().image();
                }
            }
        }
        return null;
    }
}