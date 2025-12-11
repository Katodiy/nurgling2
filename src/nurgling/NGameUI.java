package nurgling;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.res.ui.rbuff.*;
import haven.res.ui.relcnt.RelCont;
import nurgling.conf.*;
import nurgling.notifications.*;
import nurgling.overlays.QualityOl;
import nurgling.tools.*;
import nurgling.widgets.*;
import nurgling.widgets.SwimmingStatusBuff;
import nurgling.widgets.TrackingStatusBuff;
import nurgling.widgets.CrimeStatusBuff;
import nurgling.widgets.AllowVisitingStatusBuff;
import nurgling.widgets.LocalizedResourceTimersWindow;
import nurgling.widgets.LocalizedResourceTimerDialog;
import nurgling.widgets.StudyDeskPlannerWidget;
import nurgling.widgets.FishingWindowExtension;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.Inventory.invsq;

public class NGameUI extends GameUI
{
    public boolean nomadMod = false;
    NBotsMenu botsMenu;
    public NAlarmWdg alarmWdg;
    public NQuestInfo questinfo;
    public NGUIInfo guiinfo;
    public NSearchItem itemsForSearch = null;
    public NCraftWindow craftwnd;
    public NEditAreaName nean;
    public NEditFolderName nefn;
    public NImportStrategyDialog importDialog;
    public Specialisation spec;
    public RouteSpecialization routespec;
    public BotsInterruptWidget biw;
    public NEquipProxy nep;
    public NBeltProxy nbp;
    private SwimmingStatusBuff swimmingBuff = null;
    private TrackingStatusBuff trackingBuff = null;
    private CrimeStatusBuff crimeBuff = null;
    private AllowVisitingStatusBuff allowVisitingBuff = null;
    public NRecentActionsPanel recentActionsPanel;
    public LocalizedResourceTimersWindow localizedResourceTimersWindow = null;
    private LocalizedResourceTimerDialog localizedResourceTimerDialog = null;
    public LocalizedResourceTimerService localizedResourceTimerService;
    public WaypointMovementService waypointMovementService;
    public FishLocationService fishLocationService;
    public FishSearchWindow fishSearchWindow = null;
    public final Map<String, FishLocationDetailsWindow> openFishDetailWindows = new HashMap<>();
    public TreeLocationService treeLocationService;
    public TreeSearchWindow treeSearchWindow = null;
    public final Map<String, TreeLocationDetailsWindow> openTreeDetailWindows = new HashMap<>();
    public LabeledMarkService labeledMarkService;
    public TerrainSearchWindow terrainSearchWindow = null;
    public StudyDeskPlannerWidget studyDeskPlanner = null;
    public NDraggableWidget studyReportWidget = null;
    
    // Local storage for ring settings
    public IconRingConfig iconRingConfig;
    private boolean ringSettingsApplied = false;
    
    // Temporary rings (session-only, for objects without GobIcon)
    // Maps resource name to ring enabled state
    public final Map<String, Boolean> tempRingResources = Collections.synchronizedMap(new HashMap<>());
    
    // Maps gob id to kin name for party member names on minimap
    public static Map<Long, String> gobIdToKinName = new ConcurrentHashMap<>();

    /**
     * Gets the genus (world identifier) for this game instance
     */
    public String getGenus() {
        return genus;
    }
    
    public NGameUI(String chrid, long plid, String genus, NUI nui)
    {
        super(chrid, plid, genus, nui);

        // Initialize world-specific profile
        nurgling.profiles.ConfigFactory.initializeProfile(genus);

        // Initialize local ring config
        iconRingConfig = new IconRingConfig(genus);

        add(new NDraggableWidget(botsMenu = new NBotsMenu(), "botsmenu", botsMenu.sz.add(NDraggableWidget.delta)));
    }
    
    private void initHeavyWidgets() {
        itemsForSearch = new NSearchItem();
        // Replace Cal with NCal to keep calendar customizations in nurgling package
        Widget oldCalendarWidget = null;
        for(Widget wdg : children()) {
            if(wdg instanceof NDraggableWidget) {
                // Check if this draggable widget contains the Cal
                for(Widget child : wdg.children()) {
                    if(child instanceof Cal && !(child instanceof NCal)) {
                        oldCalendarWidget = wdg;
                        break;
                    }
                }
                if(oldCalendarWidget != null) break;
            }
        }
        if(oldCalendarWidget != null) {
            Coord calPos = oldCalendarWidget.c;
            oldCalendarWidget.destroy();
            calendar = new NCal();
            add(new NDraggableWidget(calendar, "Calendar", UI.scale(400,90)), calPos);
        }
        add(new NDraggableWidget(alarmWdg = new NAlarmWdg(),"alarm",NStyle.alarm[0].sz().add(NDraggableWidget.delta)));
        add(new NDraggableWidget(nep = new NEquipProxy(NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT, NEquipory.Slots.BELT), "EquipProxy",  UI.scale(138, 55)));
        add(new NDraggableWidget(nbp = new NBeltProxy(), "BeltProxy", UI.scale(825, 55)));
        for(int i = 0; i<(Integer)NConfig.get(NConfig.Key.numbelts); i++)
        {
            String name = "belt" + String.valueOf(i);
            NDraggableWidget belt = add(new NDraggableWidget(new NToolBelt(name, i * 12, 4, 12), name, UI.scale(new Coord(500, 56))));
            belt.setFlipped(true);
        }


        add(new NDraggableWidget(questinfo = new NQuestInfo(), "quests", questinfo.sz.add(NDraggableWidget.delta)));
        add(new NDraggableWidget(recentActionsPanel = new NRecentActionsPanel(), "recentactions", recentActionsPanel.sz.add(NDraggableWidget.delta)));
        add(guiinfo = new NGUIInfo(),new Coord(sz.x/2 - NGUIInfo.xs/2,sz.y/5 ));
        if(!(Boolean) NConfig.get(NConfig.Key.show_drag_menu))
            guiinfo.hide();
        // Position NEditAreaName relative to areas widget center
        add(nean = new NEditAreaName(), new Coord(sz.x/2 - nean.sz.x/2, sz.y/2 - nean.sz.y/2));
        nean.hide();
        // Position NImportStrategyDialog relative to areas widget center
        add(importDialog = new NImportStrategyDialog(), new Coord(sz.x/2 - importDialog.sz.x/2, sz.y/2 - importDialog.sz.y/2));
        importDialog.hide();
        // Position BotsInterruptWidget (observer with gears) in center of screen
        add(biw = new BotsInterruptWidget(), new Coord(sz.x/2 - biw.sz.x/2, sz.y/2 - biw.sz.y/2));
        waypointMovementService = new WaypointMovementService(this);
        fishLocationService = new FishLocationService(this, genus);
        treeLocationService = new TreeLocationService(this, genus);
        labeledMarkService = new LabeledMarkService(this, genus);
        // These widgets depend on areas which is created in GameUI constructor
        // Position NEditFolderName relative to areas widget
        add(nefn = new NEditFolderName(areas), new Coord(sz.x/2 - nefn.sz.x/2, sz.y/2 - nefn.sz.y/2));
        nefn.hide();
        // Position Specialisation relative to areas widget center
        add(spec = new Specialisation(), new Coord(sz.x/2 - spec.sz.x/2, sz.y/2 - spec.sz.y/2));
        spec.hide();
        // Position RouteSpecialization relative to routes widget center
        add(routespec = new RouteSpecialization(), new Coord(sz.x/2 - routespec.sz.x/2, sz.y/2 - routespec.sz.y/2));
        routespec.hide();
        
        // Heavy service widgets
        add(localizedResourceTimerDialog = new LocalizedResourceTimerDialog(), new Coord(200, 200));
        localizedResourceTimerService = new LocalizedResourceTimerService(this, genus);
        add(localizedResourceTimersWindow = new LocalizedResourceTimersWindow(localizedResourceTimerService), new Coord(100, 100));

        // Profile-aware components are now initialized in attached() before super.attached()
    }
    
    @Override
    protected void attached() {
        // Initialize profile-aware components BEFORE calling super.attached()
        // This ensures RouteGraphManager is available when RoutesWidget is created
        if (map instanceof NMapView) {
            ((NMapView) map).initializeWithGenus(genus);
        }

        // Update NCore to use profile-aware config (now that UI and core are available)
        if (ui != null && ui.core != null) {
            ui.core.updateConfigForProfile(genus);
        }

        // Reload explored area with profile-specific data
        if (mmap != null && mmap instanceof NCornerMiniMap) {
            NCornerMiniMap nmmap = (NCornerMiniMap) mmap;
            if (nmmap.exploredArea != null) {
                nmmap.exploredArea.reloadFromFile();
            }
        }

        // Load areas now that genus is available
        if (map != null && map.glob != null && map.glob.map != null) {
            map.glob.map.loadAreasIfNeeded();
        }

        super.attached();
        initHeavyWidgets();
        // Apply local ring settings to iconconf after it's loaded (only once)
        if (!ringSettingsApplied) {
            applyLocalRingSettings();
            ringSettingsApplied = true;
        }
    }
    
    private void applyLocalRingSettings() {
        if (iconRingConfig == null || iconconf == null) {
            return;
        }
        
        for (Map.Entry<String, Boolean> entry : iconRingConfig.getAllSettings().entrySet()) {
            String iconResName = entry.getKey();
            boolean ringEnabled = entry.getValue();
            
            // Find matching settings in iconconf
            for (GobIcon.Setting setting : iconconf.settings.values()) {
                if (setting.res != null && setting.res.name.equals(iconResName)) {
                    setting.ring = ringEnabled;
                }
            }
        }
    }

    private void initializeInventoryVisibility() {
        Object setting = NConfig.get(NConfig.Key.openInventoryOnLogin);
        boolean shouldOpenInventory = setting instanceof Boolean ? (Boolean) setting : false;

        if (shouldOpenInventory) {
            // Get the inventory window by its caption
            Window inventoryWindow = getWindow("Inventory");
            if (inventoryWindow != null) {
                // Use togglewnd to properly show the inventory window
                togglewnd(inventoryWindow);
            }
        }
        // If shouldOpenInventory is false, inventory stays hidden (default behavior)
    }

    @Override
    public void dispose() {
        if(localizedResourceTimerService != null)
            localizedResourceTimerService.dispose();
        if(fishLocationService != null)
            fishLocationService.dispose();
        if(labeledMarkService != null)
            labeledMarkService.dispose();
        if(nurgling.NUtils.getUI().core!=null)
            NUtils.getUI().core.dispose();
        super.dispose();
    }

    public int getMaxBase(){
        return chrwdg.battr.attrs.stream().max(new Comparator<BAttrWnd.Attr>() {
                    @Override
                    public int compare(BAttrWnd.Attr o1, BAttrWnd.Attr o2) {
                        return Integer.compare(o1.attr.base,o2.attr.base);
                    }
                }).get().attr.base;
    }

    public NCharacterInfo getCharInfo() {
        return ((NUI)ui).sessInfo.characterInfo;
    }

    public Window getWindow ( String cap ) {
        for ( Widget w = lchild ; w != null ; w = w.prev ) {
            if ( w instanceof Window ) {
                Window wnd = ( Window ) w;
                if ( wnd.cap != null && wnd.cap.equals(cap)) {
                    return wnd;
                }
            }
        }
        return null;
    }

    public int getWindowsNum(String name) {
        int count = 0;
        for ( Widget w = lchild ; w != null ; w = w.prev ) {
            if ( w instanceof Window ) {
                Window wnd = ( Window ) w;
                if ( wnd.cap != null && wnd.cap.equals(name)) {
                    count++;
                }
            }
        }
        return count;
    }

    public ArrayList<Window> getWindows(String name) {
        ArrayList<Window> windows = new ArrayList<>();
        for ( Widget w = lchild ; w != null ; w = w.prev ) {
            if ( w instanceof Window ) {
                Window wnd = ( Window ) w;
                if ( wnd.cap != null && wnd.cap.equals(name)) {
                    windows.add(wnd);
                }
            }
        }
        return windows;
    }

    public Window getWindowWithButton ( String cap, String button ) {
        for ( Widget w = lchild ; w != null ; w = w.prev ) {
            if ( w instanceof Window ) {
                Window wnd = ( Window ) w;
                if ( wnd.cap != null && wnd.cap.equals(cap)) {
                    for(Widget w2 = wnd.lchild ; w2 !=null ; w2= w2.prev )
                    {
                        if ( w2 instanceof Button ) {
                            Button b = ((Button)w2);
                            if(b.text!=null && b.text.text.equals(button)){
                                return (Window)w;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean isWindowExist ( Window twnd )
    {
        for (Widget w = lchild; w != null; w = w.prev)
        {
            if (w instanceof Window)
            {
                Window wnd = (Window) w;
                if (wnd.equals(twnd))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public double getTableMod() {
        double table_mod = 1;
        Window table = getWindowWithButton("Table", "Feast!");
        if(table!=null)
        {
            for (Widget wdg = table.child; wdg != null; wdg = wdg.next) {
                if (wdg instanceof Label) {
                    Label text = (Label) wdg;
                    if (text.text().contains("Food")) {
                        table_mod = table_mod + Double.parseDouble(text.text().substring(text.text().indexOf(":") + 1, text.text().indexOf("%"))) / 100.;
                        break;
                    }
                }
            }
        }
        return table_mod;
    }

    public double getRealmMod()
    {
        double realmBuff = 0;

        for (Widget wdg1 = child; wdg1 != null; wdg1 = wdg1.next)
        {
            if (wdg1 instanceof Bufflist)
            {
                for (Widget pbuff = wdg1.child; pbuff != null; pbuff = pbuff.next)
                {
                    if (pbuff instanceof RealmBuff)
                    {
                        if (((Buff) pbuff).info!=null)
                        {
                            ArrayList<ItemInfo> realm = new ArrayList<>(((Buff) pbuff).info);
                            for (Object data : realm)
                            {
                                if (data instanceof ItemInfo.AdHoc)
                                {
                                    ItemInfo.AdHoc ah = ((ItemInfo.AdHoc) data);
                                    if (NParser.checkName(ah.str.text, new NAlias("Food event")))
                                    {
                                        realmBuff = realmBuff + Double.parseDouble(ah.str.text.substring(ah.str.text.indexOf("+") + 1, ah.str.text.indexOf("%"))) / 100.;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return realmBuff;
    }

    /**
     * Called when swimming toggle state changes (event-driven)
     */
    public void onSwimmingStateChanged(boolean isSwimmingEnabled) {
        if (isSwimmingEnabled && swimmingBuff == null) {
            // Create and add swimming status buff
            swimmingBuff = new SwimmingStatusBuff();
            buffs.addchild(swimmingBuff);
        } else if (!isSwimmingEnabled && swimmingBuff != null) {
            // Remove swimming status buff
            swimmingBuff.reqdestroy();
            swimmingBuff = null;
        }
    }

    /**
     * Called when tracking toggle state changes (event-driven)
     */
    public void onTrackingStateChanged(boolean isTrackingEnabled) {
        if (isTrackingEnabled && trackingBuff == null) {
            // Create and add tracking status buff
            trackingBuff = new TrackingStatusBuff();
            buffs.addchild(trackingBuff);
        } else if (!isTrackingEnabled && trackingBuff != null) {
            // Remove tracking status buff
            trackingBuff.reqdestroy();
            trackingBuff = null;
        }
    }

    /**
     * Called when crime toggle state changes (event-driven)
     */
    public void onCrimeStateChanged(boolean isCrimeEnabled) {
        if (isCrimeEnabled && crimeBuff == null) {
            // Create and add crime status buff
            crimeBuff = new CrimeStatusBuff();
            buffs.addchild(crimeBuff);
        } else if (!isCrimeEnabled && crimeBuff != null) {
            // Remove crime status buff
            crimeBuff.reqdestroy();
            crimeBuff = null;
        }
    }

    /**
     * Called when allow visiting toggle state changes (event-driven)
     */
    public void onAllowVisitingStateChanged(boolean isAllowVisitingEnabled) {
        if (isAllowVisitingEnabled && allowVisitingBuff == null) {
            // Create and add allow visiting status buff
            allowVisitingBuff = new AllowVisitingStatusBuff();
            buffs.addchild(allowVisitingBuff);
        } else if (!isAllowVisitingEnabled && allowVisitingBuff != null) {
            // Remove allow visiting status buff
            allowVisitingBuff.reqdestroy();
            allowVisitingBuff = null;
        }
    }

    @Override
    public void addchild(Widget child, Object... args)
    {
        String place = ((String) args[0]).intern();
        if (place == "craft") {
            if (craftwnd == null) {
                craftwnd = add(new NCraftWindow(), new Coord(400, 200));
            }
            craftwnd.add(child);
            craftwnd.pack();
            craftwnd.raise();
            craftwnd.show();
        }
        else
        {
            super.addchild(child, args);

            // Apply preferred movement speed when Speedget widget is loaded
            if (place != null && place.equals("meter") && child instanceof haven.Speedget) {
                applyUserPreferredSpeed();
            }

            // Add fishing extension if this is the "This is bait" window
            if (child instanceof Window) {
                Window wnd = (Window) child;
                if ("This is bait".equals(wnd.cap)) {
                    FishingWindowExtension.addSaveFishButton(wnd, this);
                }
            }

            if (maininv != null && ((NInventory) maininv).searchwdg == null)
            {
                ((NInventory) maininv).installMainInv();
            }

            // Check if this is the inventory being added
            if (place != null && place.equals("inv")) {
                // Inventory window was just created, now check the setting
                initializeInventoryVisibility();
            }
        }
    }

    public void tickmsg(String msg) {
        msg("TICK#" + NUtils.getTickId() + " MSG: " + msg);
    }

    public NInventory getInventory ( String name ) {
        Window spwnd = getWindow ( name );
        if(spwnd == null){
            return null;
        }
        for ( Widget sp = spwnd.lchild ; sp != null ; sp = sp.prev ) {
            if ( sp instanceof Inventory ) {
                return ( ( NInventory ) sp );
            }
        }
        return null;
    }

    public NInventory getInventory () {
        return (NInventory) maininv;
    }

    public NISBox getStockpile () {
        Window spwnd = getWindow ( "Stockpile" );
        if(spwnd == null){
            return null;
        }
        for ( Widget sp = spwnd.lchild ; sp != null ; sp = sp.prev ) {
            if ( sp instanceof NISBox ) {
                return ( ( NISBox ) sp );
            }
        }
        return null;
    }

    @Override
    public void resize(Coord sz)
    {
        super.resize(sz);
        if(guiinfo != null)
            guiinfo.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 5));
        if(areas != null)
            areas.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 5));
        if(cookBook != null)
            cookBook.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 5));
        if(nean != null)
            nean.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 7));
        if(spec != null)
            spec.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 7));
        if(biw != null)
            biw.move(new Coord(sz.x / 2 - biw.sz.x / 2, sz.y / 2 - biw.sz.y / 2));
        if(blueprintWidget != null)
            blueprintWidget.move(new Coord(sz.x / 2 - NGUIInfo.xs / 2, sz.y / 5));
    }

    public List<IMeter.Meter> getmeters (String name ) {
        synchronized (meters) {
            try {
                for (Widget meter : new ArrayList<>(meters)) {
                    if (meter instanceof IMeter) {
                        IMeter im = (IMeter) meter;
                        Resource res = im.bg.get();
                        if (res != null) {
                            if (res.basename().equals(name)) {
                                return im.meters;
                            }
                        }
                    }
                }
            } catch (IndexOutOfBoundsException | ConcurrentModificationException e) {
                // Handle concurrent modification or index errors gracefully
                return null;
            }
        }
        return null;
    }

    public IMeter.Meter getmeter (
            String name,
            int midx
    ) {
        List<IMeter.Meter> meters = getmeters ( name );
        if ( meters != null && midx < meters.size () ) {
            return meters.get ( midx );
        }
        return null;
    }

    public double getBarrelContent()
    {
        return getBarrelContent(new NAlias(""));
    }

    public double getBarrelContent(NAlias content){
        Window spwnd = getWindow ( "Barrel" );
        if(spwnd!=null) {
            for (Widget sp = spwnd.lchild; sp != null; sp = sp.prev) {
                /// Выбираем внутренний контейнер
                if (sp instanceof RelCont) {
                    for(Pair<Widget, Supplier<Coord>> pair:((RelCont) sp).childpos) {
                        if (pair.a.getClass().getName().contains("TipLabel")) {
                            try {
                                ///TODO
                                for (ItemInfo inf : (Collection<ItemInfo>) (pair.a.getClass().getField("info").get(pair.a))) {
                                    if (inf instanceof ItemInfo.Name) {
                                        String name = ((ItemInfo.Name) inf).str.text;
                                        if (NParser.checkName(name.toLowerCase(), content))
                                            return Double.parseDouble(name.substring(0, name.indexOf(' ')));
                                        // Handle seed name format difference: "Flax Seeds" vs "1234 seeds of Flax"
                                        if (name.toLowerCase().contains(" seeds of ")) {
                                            int ofIndex = name.toLowerCase().indexOf(" seeds of ");
                                            String seedType = name.substring(ofIndex + 10).trim(); // Extract "Flax" from "1234 seeds of Flax"
                                            String inventoryFormat = seedType + " seeds"; // Convert to "Flax seeds"
                                            if (NParser.checkName(inventoryFormat.toLowerCase(), content))
                                                return Double.parseDouble(name.substring(0, name.indexOf(' ')));
                                        }
                                    } else if (inf instanceof ItemInfo.AdHoc) {
                                        if (NParser.checkName(((ItemInfo.AdHoc) inf).str.text, "Empty")) {
                                            return 0;
                                        }
                                    }
                                }
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    public double findBarrelContent(ArrayList<Window> windows, NAlias content){
        for (Window spwnd: windows) {
            if (spwnd != null) {
                for (Widget sp = spwnd.lchild; sp != null; sp = sp.prev) {
                    if (sp instanceof RelCont) {
                        for (Pair<Widget, Supplier<Coord>> pair : ((RelCont) sp).childpos) {
                            if (pair.a.getClass().getName().contains("TipLabel")) {
                                try {
                                    for (ItemInfo inf : (Collection<ItemInfo>) (pair.a.getClass().getField("info").get(pair.a))) {
                                        if (inf instanceof ItemInfo.Name) {
                                            String name = ((ItemInfo.Name) inf).str.text;
                                            if (NParser.checkName(name.toLowerCase(), content))
                                                return Double.parseDouble(name.substring(0, name.indexOf(' ')));
                                            // Handle seed name format difference: "Flax Seeds" vs "1234 seeds of Flax"
                                            if (name.toLowerCase().contains(" seeds of ")) {
                                                int ofIndex = name.toLowerCase().indexOf(" seeds of ");
                                                String seedType = name.substring(ofIndex + 10).trim(); // Extract "Flax" from "1234 seeds of Flax"
                                                String inventoryFormat = seedType + " seeds"; // Convert to "Flax seeds"
                                                if (NParser.checkName(inventoryFormat.toLowerCase(), content))
                                                    return Double.parseDouble(name.substring(0, name.indexOf(' ')));
                                            }
                                        }
                                    }
                                } catch (NoSuchFieldException | IllegalAccessException e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }


    public void msgToDiscord(NDiscordNotification settings, String message)
    {
        if (message != null && !message.isEmpty())
        {
            if (settings != null)
            {
                DiscordHookObject webhook = new DiscordHookObject(settings.webhookUrl);

                webhook.setContent(message);

                webhook.setAvatarUrl(settings.webhookIcon);
                webhook.setUsername(settings.webhookUsername);
                webhook.addEmbed(new nurgling.notifications.DiscordHookObject.EmbedObject()
                        .setColor(java.awt.Color.RED)
                        .setThumbnail(settings.webhookIcon)
                        .setAuthor("Nurgling2", "https://github.com/Katodiy/nurgling2", "https://raw.githubusercontent.com/Katodiy/nurgling2/master/resources/src/nurgling/hud/dragmode/title.res/image/image_0.png")
                        .setUrl("https://github.com/Katodiy/nurgling2"));
                new Thread(webhook).start();

            }
            else
            {
                error("No discord wrapper settings");
            }
        }
    }

    public void toggleol(String tag, boolean a) {
        if(map != null) {
            if(a)
                map.enol(tag);
            else
                map.disol(tag);
        }
    }



    public class NToolBelt extends Belt implements KeyBinding.Bindable{

        public static final int GAP = 10;
        public static final int PAD = 2;
        public static final int BTNSZ = 17;
        public final Coord INVSZ = invsq.sz();

        final int group;
        final int start;
        final int size;
        final String name;
        private boolean vertical = false;
        ArrayList<NKeyBinding> beltkeys = new ArrayList<>();
        public NToolBelt(String name, int start, int group, int size) {
            super( new Coord(0,0) );
            this.start = start;
            this.group = group;
            this.size = size;
            this.name = name;
            sz = beltc(size - 1).add(INVSZ);
            NToolBeltProp prop = NToolBeltProp.get(name);
            for(KeyBinding kb: prop.getKb())
            {
                beltkeys.add(new NKeyBinding(kb));
            }
        }

        @Override
        public void flip(boolean val) {
            vertical = val;
            resize();
        }

        private void resize() {
            sz = beltc(size - 1).add(INVSZ);
        }

        @Override
        public int beltslot(Coord c) {
            for (int i = 0; i < size; i++) {
                if(c.isect(beltc(i), invsq.sz())) {
                    return slot(i);
                }
            }
            return (-1);
        }

        @Override
        public KeyBinding getbinding(Coord cc) {
            int slot = beltslot(cc);
            if(slot!=-1)
                return beltkeys.get(slot - start).kb;
            return null;
        }

        @Override
        public void draw(GOut g) {
            for (int i = 0; i < size; i++) {
                Coord c = beltc(i);
                int slot = slot(i);
                g.image(invsq, c);
                try {
                    Object item = belt(slot);
                    if (item != null) {
                        if(item instanceof BeltSlot)
                            ((BeltSlot)item).draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
                        else if (item instanceof NBotsMenu.NButton)
                            ((NBotsMenu.NButton)item).btn.draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
                        else if (item instanceof NScenarioButton)
                            ((NScenarioButton)item).draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
                    }
                } catch (Loading ignored) {
                }
                if (beltkeys.get(i).tex != null) {
                    g.aimage(beltkeys.get(i).tex, c.add(INVSZ.sub(2, 0)), 1, 1);
                }
            }
            super.draw(g);
        }

        @Override
        public void keyact(int slot) {
            if(map != null) {
                NToolBeltProp prop = NToolBeltProp.get(name);
                String path;
                if((path = prop.custom.get(slot))!=null) {
                    if(path.startsWith("scenario:")) {
                        // Handle scenario button execution
                        String scenarioName = path.substring("scenario:".length());
                        ui.core.scenarioManager.executeScenarioByName(scenarioName, ui.gui);
                        return;
                    } else {
                        // Handle regular bot button
                        NBotsMenu.NButton btn = NUtils.getGameUI().botsMenu.find(path);
                        if(btn!=null) {
                            btn.btn.click();
                            return;
                        }
                    }
                }
                super.keyact(slot);
            }
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            NToolBeltProp prop = NToolBeltProp.get(name);
            int slot = beltslot(ev.c);
            if(ev.b == 3)
            {
                if(prop.custom.get(slot)!=null) {
                    prop.custom.remove(slot);
                    NToolBeltProp.set(name, prop);
                    return true;
                }
            }
            else if (ev.b == 1)
            {
                String path;
                if((path = prop.custom.get(slot))!=null) {
                    if(path.startsWith("scenario:")) {
                        // Handle scenario button execution
                        String scenarioName = path.substring("scenario:".length());
                        ui.core.scenarioManager.executeScenarioByName(scenarioName, ui.gui);
                        return true;
                    } else {
                        // Handle regular bot button
                        NBotsMenu.NButton btn = NUtils.getGameUI().botsMenu.find(path);
                        if(btn!=null) {
                            btn.btn.click();
                            return true;
                        }
                    }
                }
            }
            return super.mousedown(ev);
        }


        private Object belt(int slot) {
            if(slot < 0) {return null;}
            String path;
            if((path = NToolBeltProp.get(name).custom.get(slot) )== null) {
                GameUI.BeltSlot res = null;
                if (ui != null && belt[slot] != null)
                    res = belt[slot];
                return res;
            }
            else
            {
                if(path.startsWith("scenario:")) {
                    String scenarioName = path.substring("scenario:".length());
                    for(nurgling.scenarios.Scenario scenario : ui.core.scenarioManager.getScenarios().values()) {
                        if(scenario.getName().equals(scenarioName)) {
                            return new NScenarioButton(scenario);
                        }
                    }
                    return null;
                } else {
                    return botsMenu.find(path);
                }
            }
        }

        private int slot(int i) {return i + start;}

        private Coord beltc(int i) {
            return vertical ?
                    new Coord(0, BTNSZ + ((INVSZ.y + PAD) * i) + (GAP * (i / group))) :
                    new Coord(BTNSZ + ((INVSZ.x + PAD) * i) + (GAP * (i /group )), 0);
        }

        @Override
        public void tick(double dt) {
            super.tick(dt);
            boolean res = false;
            for(NKeyBinding kb : beltkeys)
                res |= kb.tick();
            if(res)
            {
                NToolBeltProp.set(name,NToolBeltProp.get(name));
            }
        }

        @Override
        public boolean globtype(GlobKeyEvent ev) {
            if (!visible) {
                return false;
            }
            for (int i = 0; i < beltkeys.size(); i++) {
                if ((beltkeys.get(i).key != null && ev.code == beltkeys.get(i).key.code && ui.modflags() == beltkeys.get(i).key.modmatch)) {
                    keyact(slot(i));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean dropthing(Coord c, Object thing) {
            boolean res = super.dropthing(c,thing);
            int slot = beltslot(c);
            if(res) {
                if(slot!=-1)
                {
                    NToolBeltProp prop = NToolBeltProp.get(name);
                    prop.custom.remove(slot);
                    NToolBeltProp.set(name,prop);
                }
                return true;
            }

            if(slot != -1) {
                if(thing instanceof NBotsMenu.NButton) {
                    NBotsMenu.NButton pag = (NBotsMenu.NButton)thing;
                    NToolBeltProp prop = NToolBeltProp.get(name);
                    prop.custom.put(slot,pag.path);
                    NToolBeltProp.set(name,prop);
                    return(true);
                } else if(thing instanceof nurgling.widgets.NScenarioButton) {
                    nurgling.widgets.NScenarioButton scenarioBtn = (nurgling.widgets.NScenarioButton)thing;
                    NToolBeltProp prop = NToolBeltProp.get(name);
                    // Use scenario name as the identifier for scenarios
                    prop.custom.put(slot, "scenario:" + scenarioBtn.getScenario().getName());
                    NToolBeltProp.set(name,prop);
                    return(true);
                }
            }
            return(false);
        }

    }

    public static class NKeyBinding
    {
        public int modign;
        public KeyMatch key;
        KeyBinding kb;
        public NKeyBinding(KeyBinding old) {
            this.kb = old;
            this.key = old.key;
            this.modign = old.modign;
            updateTex();
        }

        Tex tex;
        public void set(KeyMatch key) {
            kb.set(key);
            updateTex();
        }

        void updateTex()
        {
            String hotKey;
            int mode  = 0;
            if( key != null)
            {
                hotKey = KeyEvent.getKeyText(key.code);
                mode = key.modmatch;

                if (NParser.checkName(hotKey, new NAlias("Num")))
                {
                    hotKey = "N" + hotKey.substring(hotKey.indexOf("-") + 1);
                }
                if (NParser.checkName(hotKey, new NAlias("inus")))
                {
                    hotKey = "-";
                }
                else if (NParser.checkName(hotKey, new NAlias("quals")))
                {
                    hotKey = "=";
                }
                if ((mode & KeyMatch.C) != 0)
                    hotKey = "C" + hotKey;
                if ((mode & KeyMatch.S) != 0)
                    hotKey = "S" + hotKey;
                if ((mode & KeyMatch.M) != 0)
                    hotKey = "A" + hotKey;
                tex = NStyle.hotkey.render(hotKey).tex();
            } else {
                tex = null;
            }
        }

        boolean tick()
        {
            if(kb.key!=key || kb.modign!=modign)
            {
                key = kb.key;
                modign = kb.modign;
                updateTex();
                return true;
            }
            return false;
        }


    }



    public boolean msg(UI.Notice msg) {
        if (msg.message().contains("Quality")) {
            if(map.clickedGob!=null)
            {
                Matcher m = Pattern.compile("Quality: (\\d+)").matcher(msg.message());
                if(m.matches()) {
                    try {
                        map.clickedGob.gob.addcustomol(new QualityOl(map.clickedGob.gob, Integer.parseInt(m.group(1))));
                    } catch (NumberFormatException ignored) {
                    } finally {
                        map.clickedGob = null;
                    }
                }
            }
        }
        return super.msg(msg);
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
        nurgling.tasks.WaitKeyPress.setLastKeyPressed(ev.code);
        return super.keydown(ev);
    }

    public void toggleResourceTimerWindow() {
        if(localizedResourceTimerService != null) {
            localizedResourceTimerService.showTimerWindow();
        }
    }
    
    public LocalizedResourceTimerDialog getAddResourceTimerWidget() {
        return localizedResourceTimerDialog;
    }

    /**
     * Apply user's preferred movement speed from config
     */
    private void applyUserPreferredSpeed() {
        try {
            Object speedPref = NConfig.get(NConfig.Key.preferredMovementSpeed);
            if (speedPref instanceof Number) {
                int preferredSpeed = ((Number) speedPref).intValue();
                if (preferredSpeed >= 0 && preferredSpeed <= 3) { // Valid range
                    // Small delay to ensure speedget is fully initialized
                    new Thread(() -> {
                        try {
                            Thread.sleep(100); // Brief pause
                            NUtils.setSpeed(preferredSpeed);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            System.err.println("[NGameUI] Failed to set preferred speed: " + e.getMessage());
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            System.err.println("[NGameUI] Failed to apply preferred movement speed: " + e.getMessage());
        }
    }
}
