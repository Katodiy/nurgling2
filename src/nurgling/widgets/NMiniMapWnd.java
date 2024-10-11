package nurgling.widgets;

import haven.*;
import mapv4.StatusWdg;
import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;

import java.net.MalformedURLException;

public class NMiniMapWnd extends Widget{
    NMapView map;
    public Map miniMap;
    public IButton geoloc;
    public static final KeyBinding kb_claim = KeyBinding.get("ol-claim", KeyMatch.nil);
    public static final KeyBinding kb_vil = KeyBinding.get("ol-vil", KeyMatch.nil);

    public static class NMenuCheckBox extends ICheckBox {
        public NMenuCheckBox(String base, KeyBinding gkey, String tooltip) {
            super(base, "/u", "/d", "/h", "/dh");
            setgkey(gkey);
            settip(tooltip);
        }
    }

    ACheckBox map_box;
    Widget toggle_panel;
    public StatusWdg swdg;
    public static final IBox pbox = Window.wbox;
    public static final KeyBinding kb_eye = KeyBinding.get("ol-eye", KeyMatch.nil);
    public static final KeyBinding kb_grid = KeyBinding.get("ol-mgrid", KeyMatch.nil);
    public static final KeyBinding kb_path = KeyBinding.get("ol-mgrid", KeyMatch.nil);
    public static final KeyBinding kb_hidenature = KeyBinding.get("ol-hidenature", KeyMatch.nil);
    public static final KeyBinding kb_minesup = KeyBinding.get("ol-minesup", KeyMatch.nil);
    final Coord marg = UI.scale(new Coord(5,5));
    public NMiniMapWnd(String name, NMapView map, MapFile file) {
        super(new Coord(UI.scale(133),UI.scale(133)));
        this.map = map;
        ResCache mapstore = ResCache.global;
        if(MapFile.mapbase.get() != null) {
            try {
                mapstore = HashDirCache.get(String.valueOf(MapFile.mapbase.get().toURL()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        if(mapstore != null) {
//            try {
//
//            } catch (java.io.IOException e) {
//                /* XXX: Not quite sure what to do here. It's
//                 * certainly not obvious that overwriting the
//                 * existing mapfile with a new one is better. */
//                throw (new RuntimeException("failed to load mapfile", e));
//            }

            miniMap = add(new Map(new Coord(UI.scale(133), UI.scale(133)), file, map));
            miniMap.lower();
        }

        toggle_panel = new Widget();
        ACheckBox first = toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/claim", GameUI.kb_claim, "Display personal claims"), 0, 0).changed(a -> NUtils.getGameUI().toggleol("cplot", a));
        toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/vil", GameUI.kb_vil, "Display village claims"), (first.sz.x+UI.scale(3)), 0).changed(a -> NUtils.getGameUI().toggleol("vlg", a));
        int shift = 2;
        toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/rlm", GameUI.kb_rlm, "Display realms"), (first.sz.x+UI.scale(3))*shift++, 0).changed(a -> NUtils.getGameUI().toggleol("realm", a));
        toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/ico", GameUI.kb_ico, "Icon settings"), (first.sz.x+UI.scale(3))*shift++, 0).state(() -> NMiniMapWnd.this.ui.gui.wndstate(NMiniMapWnd.this.ui.gui.iconwnd)).click(() -> {
            if(NUtils.getGameUI() == null || NUtils.getGameUI().iconconf == null)
                return;
            if(NUtils.getGameUI().iconwnd == null) {
                NUtils.getGameUI().iconwnd = new GobIcon.SettingsWindow(NUtils.getGameUI().iconconf);
                NUtils.getGameUI().fitwdg(NUtils.getGameUI().add(NUtils.getGameUI().iconwnd, Utils.getprefc("wndc-icon", new Coord(200, 200))));
            } else {
                ui.destroy(NUtils.getGameUI().iconwnd);
                NUtils.getGameUI().iconwnd = null;
            }
        });
        ACheckBox eye = toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/vis", kb_eye, "Display vision area"), (first.sz.x+UI.scale(3))*shift++, 0).changed(a -> switchStatus("eye", a));
        eye.a = (Boolean)NConfig.get(NConfig.Key.showView);

        ACheckBox grid = toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/grid", kb_grid, "Display grid"), (first.sz.x+UI.scale(3))*shift++, 0).changed(a -> switchStatus("grid", a));
        grid.a = (Boolean) NConfig.get(NConfig.Key.showGrid);
        ACheckBox minesup = toggle_panel.add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/minesup", kb_grid, "Display mining overlay"), (first.sz.x+UI.scale(3))*shift++, 0).changed(a -> switchStatus("miningol", a));
        minesup.a = (Boolean) NConfig.get(NConfig.Key.miningol);
        geoloc = toggle_panel.add(new IButton(Resource.loadsimg("nurgling/hud/buttons/toggle_panel/geoloc/d"), Resource.loadsimg("nurgling/hud/buttons/toggle_panel/geoloc/u"), Resource.loadsimg("nurgling/hud/buttons/toggle_panel/geoloc/h"), new Runnable() {
            @Override
            public void run() {
                NUtils.getUI().core.mappingClient.OpenMap();
            }
        }), (first.sz.x+UI.scale(3))*shift++, 0);

//        ACheckBox path = toggle_panel.add(new NMenuCheckBox("lbtn-path", kb_path, "Display objects paths"), (first.sz.x+UI.scale(3))*6, 0).changed(a -> NUtils.getGameUI().mmapw.miniMap.toggleol("path", a));
//        path.a = NConfiguration.getInstance().isPaths;

        map_box = add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/map", GameUI.kb_map, "Map"), miniMap.sz.x-(first.sz.x), 0).state(() -> NMiniMapWnd.this.ui.gui.wndstate(NMiniMapWnd.this.ui.gui.mapfile)).click(() -> {
            NUtils.getGameUI().togglewnd(NUtils.getGameUI().mapfile);
            if(NUtils.getGameUI().mapfile != null)
                Utils.setprefb("wndvis-map", NUtils.getGameUI().mapfile.visible());
        });
//        ACheckBox naturalobj = toggle_panel.add(new NMenuCheckBox("lbtn-naturalobj", kb_hidenature, "Hide/show natural objects"), (first.sz.x+UI.scale(3))*8, 0).changed(a -> NUtils.getGameUI().mmapw.miniMap.toggleol("hidenature", a));
//        naturalobj.a = NConfiguration.getInstance().hideNature;

        toggle_panel.pack();
        add(toggle_panel);
        swdg = add(new StatusWdg(UI.scale(32,32)), UI.scale(4,4));
        pack();
    }

    public void switchStatus(String val, Boolean a) {
        switch (val){
            case "eye": {
                NConfig.set(NConfig.Key.showView,a);
                break;
            }
            case "grid": {
                NConfig.set(NConfig.Key.showGrid,a);
                break;
            }
            case "miningol": {
                NConfig.set(NConfig.Key.miningol,a);
                break;
            }
        }
    }

    public void draw(GOut g, boolean strict)
    {
        drawWidget(g,strict,miniMap);
        pbox.draw(g, miniMap.c.sub(marg), miniMap.sz.add(marg.mul(2)));
        drawWidget(g,strict,toggle_panel);
        drawWidget(g,strict,map_box);
        drawWidget(g,strict,swdg);
    }

    void drawWidget(GOut g, boolean strict, Widget wdg)
    {
        Coord cc = xlate(wdg.c, true);
        GOut g2;
        if(strict)
            g2 = g.reclip(cc, wdg.sz);
        else
            g2 = g.reclipl(cc, wdg.sz);
        wdg.draw(g2);
    }

    public static class Map extends NMiniMap {
        NMapView map;
        public Map(Coord sz, MapFile file,NMapView map) {
            super(sz, file);
            follow(new MapLocator(map));
            c = new Coord(0,0);
            this.map = map;
        }

        public boolean dragp(int button) {
            return(false);
        }

        public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
            if(mark.m instanceof MapFile.SMarker) {
                Gob gob = MarkerID.find(ui.sess.glob.oc, mark.m);
                if(gob != null)
                    mvclick(map, null, loc, gob, button);
            }
            return(false);
        }

        public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
            if(press) {
                mvclick(map, null, loc, icon.gob, button);
                return(true);
            }
            return(false);
        }

        public boolean clickloc(Location loc, int button, boolean press) {
            if(press) {
                mvclick(map, null, loc, null, button);
                return(true);
            }
            return(false);
        }

        public void draw(GOut g) {
            super.draw(g);
        }

        protected boolean allowzoomout() {
            if(zoomlevel >= 5)
                return(false);
            return(super.allowzoomout());
        }
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        miniMap.resize(sz.x - UI.scale(15), sz.y );
        map_box.move(new Coord(miniMap.sz.x-(map_box.sz.x), 0));
        toggle_panel.move(new Coord(0, miniMap.sz.y-(map_box.sz.y)));
    }
}
