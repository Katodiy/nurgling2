package nurgling.widgets;

import haven.*;
import mapv4.StatusWdg;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.tools.ExploredArea;

import java.net.MalformedURLException;

public class NMiniMapWnd extends Widget{
    NMapView map;
    public Map miniMap;
    public IButton geoloc;
    public static final KeyBinding kb_night = KeyBinding.get("mwnd_night", KeyMatch.nil);
    public static final KeyBinding kb_fog = KeyBinding.get("mwnd_fog", KeyMatch.nil);
    public static final KeyBinding kb_nature = KeyBinding.get("mwnd_nature", KeyMatch.nil);
    public static final KeyBinding kb_resourcetimers = KeyBinding.get("mwnd_resourcetimers", KeyMatch.nil);
    public static class NMenuCheckBox extends ICheckBox {
        public NMenuCheckBox(String base, KeyBinding gkey, String tooltip) {
            super(base, "/u", "/d", "/h", "/dh");
            setgkey(gkey);
            settip(tooltip);
        }
    }
    
    /**
     * Special checkbox for explored area toggle that supports right-click menu
     * for session layer management.
     */
    public class ExploredAreaCheckBox extends NMenuCheckBox {
        private ExploredAreaMenu menu = null;
        
        public ExploredAreaCheckBox(String base, KeyBinding gkey, String tooltip) {
            super(base, gkey, tooltip);
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 3 && checkhit(ev.c)) {
                // Right-click - show session menu
                showSessionMenu();
                return true;
            }
            return super.mousedown(ev);
        }
        
        private void showSessionMenu() {
            ExploredArea exploredArea = null;
            if (miniMap instanceof NCornerMiniMap) {
                exploredArea = ((NCornerMiniMap) miniMap).exploredArea;
            } else if (miniMap instanceof Map) {
                exploredArea = ((Map) miniMap).exploredArea;
            }
            
            if (exploredArea == null) return;
            
            // Close existing menu if any
            if (menu != null) {
                ui.destroy(menu);
                menu = null;
            }
            
            // Create new menu (always fresh to reflect current session state)
            final ExploredArea ea = exploredArea;
            menu = new ExploredAreaMenu(ea) {
                @Override
                public void destroy() {
                    menu = null;
                    super.destroy();
                }
            };
            
            // Add menu at mouse cursor position
            ui.root.add(menu, ui.mc);
        }
    }
    public ACheckBox nightvision;
    public ACheckBox fog;
    public ACheckBox natura;
    public ACheckBox minesup;
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
        java.util.List<Widget> buttons = new java.util.ArrayList<>();
        
        ACheckBox first = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/claim", GameUI.kb_claim, "Display personal claims");
        first.changed(a -> switchStatus("cplot", a));
        first.a = (Boolean) NConfig.get(NConfig.Key.claimol);
        switchStatus("cplot", first.a);
        buttons.add(first);

        ACheckBox vilol = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/vil", GameUI.kb_vil, "Display village claims");
        vilol.changed(a -> switchStatus("vlg", a));
        vilol.a = (Boolean) NConfig.get(NConfig.Key.vilol);
        switchStatus("vlg", vilol.a);
        buttons.add(vilol);

        ACheckBox realmol = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/rlm", GameUI.kb_rlm, "Display realms");
        realmol.changed(a -> switchStatus("realm", a));
        realmol.a = (Boolean) NConfig.get(NConfig.Key.realmol);
        switchStatus("realm", realmol.a);
        buttons.add(realmol);

        ACheckBox ico = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/ico", GameUI.kb_ico, "Icon settings");
        ico.state(() -> NMiniMapWnd.this.ui.gui.wndstate(NMiniMapWnd.this.ui.gui.iconwnd));
        ico.click(() -> {
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
        buttons.add(ico);

        ACheckBox eye = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/vis", kb_eye, "Display vision area");
        eye.changed(a -> switchStatus("eye", a));
        eye.a = (Boolean)NConfig.get(NConfig.Key.showView);
        buttons.add(eye);

        ACheckBox grid = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/grid", kb_grid, "Display grid");
        grid.changed(a -> switchStatus("grid", a));
        grid.a = (Boolean) NConfig.get(NConfig.Key.showGrid);
        buttons.add(grid);

        minesup = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/minesup", kb_minesup, "Display mining overlay");
        minesup.changed(a -> switchStatus("miningol", a));
        minesup.a = (Boolean) NConfig.get(NConfig.Key.miningol);
        buttons.add(minesup);

        geoloc = new IButton(Resource.loadsimg("nurgling/hud/buttons/toggle_panel/geoloc/d"), Resource.loadsimg("nurgling/hud/buttons/toggle_panel/geoloc/u"), Resource.loadsimg("nurgling/hud/buttons/toggle_panel/geoloc/h"), new Runnable() {
            @Override
            public void run() {
                NUtils.getUI().core.mappingClient.OpenMap();
            }
        });
        buttons.add(geoloc);

        natura = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/natura", kb_nature, "Show/hides natural objects");
        natura.changed(a -> switchStatus("natura", !a));
        natura.a = !(Boolean) NConfig.get(NConfig.Key.hideNature);
        buttons.add(natura);

        nightvision = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/daynight", kb_night, "Night vision");
        nightvision.changed(a -> switchStatus("night", a));
        nightvision.a = (Boolean) NConfig.get(NConfig.Key.nightVision);
        buttons.add(nightvision);

        fog = new ExploredAreaCheckBox("nurgling/hud/buttons/toggle_panel/fog", kb_fog, "Explored area (RMB for session)");
        fog.changed(a -> {
            NConfig.set(NConfig.Key.exploredAreaEnable, a);
            NConfig.needUpdate();
        });
        fog.a = (Boolean) NConfig.get(NConfig.Key.exploredAreaEnable);
        buttons.add(fog);

        ACheckBox timer = new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/timer", kb_resourcetimers, "Resource Timers");
        timer.state(() -> {
            NGameUI gui = NUtils.getGameUI();
            return gui != null && gui.localizedResourceTimersWindow != null && gui.localizedResourceTimersWindow.visible();
        });
        timer.click(() -> {
            NGameUI gui = NUtils.getGameUI();
            if (gui != null) {
                gui.toggleResourceTimerWindow();
            }
        });
        buttons.add(timer);

        // Layout buttons with wrapping
        layoutButtons(buttons);

        toggle_panel.pack();
        add(toggle_panel);
        
        map_box = add(new NMenuCheckBox("nurgling/hud/buttons/toggle_panel/map", GameUI.kb_map, "Map"), miniMap.sz.x-(first.sz.x), 0).state(() -> NMiniMapWnd.this.ui.gui.wndstate(NMiniMapWnd.this.ui.gui.mapfile)).click(() -> {
            NUtils.getGameUI().togglewnd(NUtils.getGameUI().mapfile);
            if(NUtils.getGameUI().mapfile != null)
                Utils.setprefb("wndvis-map", NUtils.getGameUI().mapfile.visible());
        });
        swdg = add(new StatusWdg(UI.scale(32,32)), UI.scale(4,4));
        pack();
    }

    public void switchStatus(String val, Boolean a) {
        switch (val){
            case "cplot": {
                NUtils.getGameUI().toggleol("cplot", a);
                NConfig.set(NConfig.Key.claimol,a);
                break;
            }
            case "vlg": {
                NUtils.getGameUI().toggleol("vlg", a);
                NConfig.set(NConfig.Key.vilol,a);
                break;
            }
            case "realm": {
                NUtils.getGameUI().toggleol("realm", a);
                NConfig.set(NConfig.Key.realmol,a);
                break;
            }
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
                
                // Sync with QoL panel
                if (NUtils.getGameUI() != null && NUtils.getGameUI().opts != null && NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                    OptWnd.NSettingsPanel panel = (OptWnd.NSettingsPanel) NUtils.getGameUI().opts.nqolwnd;
                    if (panel.settingsWindow != null && panel.settingsWindow.qol != null) {
                        panel.settingsWindow.qol.syncMiningOverlay();
                    }
                }
                
                break;
            }
            case "natura": {
                NConfig.set(NConfig.Key.hideNature,a);
                NUtils.showHideNature();
                
                // Sync with World settings panel
                if (NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                    ((OptWnd.NSettingsPanel)NUtils.getGameUI().opts.nqolwnd).settingsWindow.world.setNatureStatus(a);
                }
                
                // Sync with QoL panel
                if (NUtils.getGameUI() != null && NUtils.getGameUI().opts != null && NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                    OptWnd.NSettingsPanel panel = (OptWnd.NSettingsPanel) NUtils.getGameUI().opts.nqolwnd;
                    if (panel.settingsWindow != null && panel.settingsWindow.qol != null) {
                        panel.settingsWindow.qol.syncHideNature();
                    }
                }
                
                break;
            }
            case "night":
            {
                NConfig.set(NConfig.Key.nightVision, a);
                if (ui.sess != null && ui.sess.glob != null)
                {
                    ui.sess.glob.brighten();
                }
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

    public static class Map extends NCornerMiniMap {
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
            // Handle shift+right-click on resource markers for timer functionality
            if(button == 3 && ui.modshift && mark.m instanceof MapFile.SMarker) {
                MapFile.SMarker smarker = (MapFile.SMarker) mark.m;
                
                // Check if this is a localized resource (map resource) and handle through service
                NGameUI gui = (NGameUI) NUtils.getGameUI();
                if(gui != null && gui.localizedResourceTimerService != null &&
                   gui.localizedResourceTimerService.handleResourceClick(smarker)) {
                    return true;
                }
            }
            
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
            // Handle alt+left-click for waypoint queueing
            if(!press && button == 1 && ui.modmeta && sessloc != null && loc.seg.id == sessloc.seg.id) {
                NGameUI gui = (NGameUI) NUtils.getGameUI();
                if(gui != null && gui.waypointMovementService != null) {
                    gui.waypointMovementService.addWaypoint(loc, sessloc);
                    return true;
                }
            }

            // Right-click to clear waypoint queue
            if(!press && button == 3) {
                NGameUI gui = (NGameUI) NUtils.getGameUI();
                if(gui != null && gui.waypointMovementService != null) {
                    gui.waypointMovementService.clearQueue();
                }
            }

            // Handle press events normally
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

    private void layoutButtons(java.util.List<Widget> buttons) {
        if(buttons.isEmpty()) return;
        
        int btnSpacing = UI.scale(3);
        int maxWidth = miniMap.sz.x;
        int currentX = 0;
        int currentY = 0;
        int rowHeight = 0;
        
        for(Widget btn : buttons) {
            // Get button size
            int btnWidth = btn.sz.x;
            int btnHeight = btn.sz.y;
            
            // Check if button fits in current row
            if(currentX > 0 && currentX + btnWidth > maxWidth) {
                // Move to next row
                currentX = 0;
                currentY += rowHeight + btnSpacing;
                rowHeight = 0;
            }
            
            // Add button to toggle_panel
            toggle_panel.add(btn, currentX, currentY);
            
            // Update position and row height
            currentX += btnWidth + btnSpacing;
            rowHeight = Math.max(rowHeight, btnHeight);
        }
    }
    
    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        miniMap.resize(sz.x - UI.scale(15), sz.y );
        
        // Re-layout buttons when resizing
        if(toggle_panel != null) {
            // Collect all buttons
            java.util.List<Widget> buttons = new java.util.ArrayList<>();
            for(Widget w : toggle_panel.children()) {
                buttons.add(w);
            }
            
            // Remove all buttons from panel
            for(Widget w : buttons) {
                w.unlink();
            }
            
            // Re-layout
            layoutButtons(buttons);
            toggle_panel.pack();
        }
        
        map_box.move(new Coord(miniMap.sz.x-(map_box.sz.x), 0));
        toggle_panel.move(new Coord(0, miniMap.sz.y-(toggle_panel.sz.y)));
    }
}
