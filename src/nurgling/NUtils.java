package nurgling;

import haven.*;
import haven.Button;
import haven.Window;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import mapv4.StatusWdg;
import nurgling.areas.*;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;
import nurgling.widgets.options.AutoSelection;
import nurgling.widgets.options.QuickActions;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.OCache.posres;

public class NUtils
{
    public static long getTickId()
    {
        if(NUtils.getGameUI()!= null )
            return  NUtils.getUI().tickId;
        return -1;
    }

    public static NGameUI getGameUI(){
        return getUI().gui;
    }

    public static NUI getUI(){
        return (NUI)UI.getInstance();
    }

    public static String timestamp() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    public static String timestamp(String text) {
        return String.format("[%s] %s", timestamp(), text);
    }

    public static Gob findGob(long id)
    {
        return NUtils.getGameUI().ui.sess.glob.oc.getgob( id );
    }

    public static NArea findArea(String name){
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            return ((NMapView)NUtils.getGameUI().map).findArea(name);
        }
        return null;
    }

    public static void showHideNature() {
        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            if((Boolean) NConfig.get(NConfig.Key.hideNature))
                for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                    if (gob.ngob.name!=null && isNatureObject(gob.ngob.name))
                    {
                        gob.show();
                    }
                }
            else
                for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                    if (gob.ngob.name!=null && isNatureObject(gob.ngob.name))
                    {
                        gob.hide();
                    }
                }
        }
    }

    public static boolean isNatureObject(String name)
    {
        return NParser.checkName(name, new NAlias(new ArrayList<>(Arrays.asList("gfx/terobjs/tree", "gfx/terobjs/bumlings","gfx/terobjs/bushes","gfx/terobjs/stonepillar")), new ArrayList<>(Arrays.asList("log", "oldtrunk"))));
    }

    public static WItem takeItemToHand(WItem item) throws InterruptedException
    {
        if(item == null)
            return null;
        item.item.wdgmsg("take", Coord.z);
        WaitItemInHand tith = new WaitItemInHand(item);
        getUI().core.addTask(tith);
        return getGameUI().vhand;
    }

    public static WItem takeItemToHand(GItem item) throws InterruptedException
    {
        item.wdgmsg("take", Coord.z);
        WaitItemInHand tith = new WaitItemInHand(item);
        getUI().core.addTask(tith);
        return getGameUI().vhand;
    }

    public static NFlowerMenu getFlowerMenu() throws InterruptedException
    {
        FindNFlowerMenu fnf = new FindNFlowerMenu();
        getUI().core.addTask(fnf);
        return fnf.getResult();
    }

    public static NFlowerMenu findFlowerMenu() throws InterruptedException
    {
        FindOrWaitNFlowerMenu fnf = new FindOrWaitNFlowerMenu();
        getUI().core.addTask(fnf);
        return fnf.getResult();
    }

    public static NArea getArea(int id)
    {
        return getGameUI().map.glob.map.areas.get(id);
    }

    public static Gob player()
    {
        if(getGameUI()== null || getGameUI().map ==null)
            return null;
        return getGameUI().map.player();
    }

    public static long playerID()
    {
        if(getGameUI()== null || getGameUI().map ==null )
            return -1;
        return getGameUI().map.plgob;
    }

    public static double getStamina()
    {
        IMeter.Meter stam = getGameUI().getmeter ( "stam", 0 );
        return stam.a;
    }

    public static double getEnergy()
    {
        IMeter.Meter stam = getGameUI().getmeter ( "nrj", 0 );
        return stam.a;
    }

    public static NEquipory getEquipment(){
        if ( getGameUI()!=null && getGameUI().equwnd != null ) {
            for ( Widget w = getGameUI().equwnd.lchild ; w != null ; w = w.prev ) {
                if ( w instanceof Equipory ) {
                    return ( NEquipory ) w;
                }
            }
        }
        return null;
    }

    public static void clickGob(Gob gob) {
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres),1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static void rclickGob(Gob gob) {
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres),3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static void lclick(Coord2d pos) {
        getGameUI().map.wdgmsg("click", Coord.z, pos.floor(posres),1, 0);
    }



    public static void activateItem(Gob gob, boolean shift) {
        getGameUI().map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), shift ? 1 : 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static void activateItem(Coord2d pos) {
        getGameUI().map.wdgmsg("itemact", Coord.z, pos.floor(posres),0);
    }

    public static void activateGob(Gob gob) {
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static void takeAllGob(Gob gob) {
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 3, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }


    public static void activateItem(Gob gob) {
        activateItem(gob, false);
    }

    public static void dropsame(Gob gob) {
        getGameUI().map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 3, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static void dropToInv() throws InterruptedException {
        if(NUtils.getGameUI().vhand!=null) {
            getGameUI().getInventory().dropOn(getGameUI().getInventory().findFreeCoord(NUtils.getGameUI().vhand));
        }
    }

    public static void lift(
            Gob gob
    )
            throws InterruptedException {
        getGameUI().ui.rcvr.rcvmsg(getUI().getMenuGridId(), "act", "carry");
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres),
                0, -1);
        getUI().core.addTask(new WaitLifted(gob));
    }

    public static void attack(
            Gob gob, boolean noTask
    )
            throws InterruptedException {
        getGameUI().ui.rcvr.rcvmsg(getUI().getMenuGridId(), "act", "aggro");
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres),
                0, -1);
        if(!noTask)
            getUI().core.addTask(new WaitBattleWindow(gob.id, noTask));
    }

    public static void mine(
            Coord2d pos
    )
            throws InterruptedException {
        getGameUI().ui.rcvr.rcvmsg(getUI().getMenuGridId(), "act", "mine");
        getGameUI().map.wdgmsg("click", Coord.z, pos.floor(posres), 1, 0, 0);

    }

    public static void destroy(
            Gob gob
    )
            throws InterruptedException {
        getGameUI().ui.rcvr.rcvmsg(getUI().getMenuGridId(), "act", "destroy");
        getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres),
                0, -1);
    }


    public static String getCursorName()
    {
        return NUtils.getUI().root.cursorRes;
    }

    public static void getDefaultCur() throws InterruptedException {
        Gob player = NUtils.player();
        if (player!=null && !NParser.checkName(NUtils.getCursorName(), "arw")) {
            NUtils.getGameUI().map.wdgmsg("click", Coord.z, player.rc.floor(posres), 3, 0);
            NUtils.getUI().core.addTask(new GetCurs("arw"));
        }
    }

    public static void dig()
            throws InterruptedException {
        getGameUI().ui.rcvr.rcvmsg(getUI().getMenuGridId(), "act", "dig");
        NUtils.addTask(new GetCurs("dig"));
        getGameUI().map.wdgmsg("click", Coord.z, player().rc.floor(posres), 1, 0);
//        getUI().core.addTask(new WaitLifted(gob));
    }

    public static void place(Gob gob, Coord2d coord2d, double a) throws InterruptedException {
        NUtils.activateGob(gob);
        getUI().core.addTask(new WaitPlob());
        getGameUI().map.wdgmsg("place", coord2d.floor(posres), (int)Math.round(a * 32768 / Math.PI), 1, 1);
        getUI().core.addTask(new WaitPlaced(gob));
    }

    public static RosterWindow getRosterWindow(Class<? extends Entry> cattleRoster) throws InterruptedException {
        RosterWindow w;
        if((w = (RosterWindow)NUtils.getGameUI().getWindow("Cattle Roster")) == null) {
            getGameUI().ui.rcvr.rcvmsg(getUI().getMenuGridId(), "act", "croster");
            getUI().core.addTask(new WaitRosterLoad(cattleRoster));
            w = (RosterWindow)NUtils.getGameUI().getWindow("Cattle Roster");
        }

        w.show(cattleRoster);
        return w;
    }

    public static Comparator<Gob> d_comp = new Comparator<Gob>() {
        @Override
        public int compare(Gob o1, Gob o2) {
            return Double.compare(o1.rc.dist(NUtils.getGameUI().map.player().rc),o2.rc.dist(NUtils.getGameUI().map.player().rc));
        }
    };

    public static Comparator<Gob> y_min_comp = new Comparator<Gob>() {
        @Override
        public int compare(Gob o1, Gob o2) {
            return Double.compare(o1.rc.y,o2.rc.y);
        }
    };

    public static Comparator<Gob> x_min_comp = new Comparator<Gob>() {
        @Override
        public int compare(Gob o1, Gob o2) {
            return Double.compare(o1.rc.x,o2.rc.x);
        }
    };

    public static Comparator<Gob> grid_comp = new Comparator<Gob>() {
        @Override
        public int compare(Gob o1, Gob o2) {
            int res = Double.compare(o1.rc.x, o2.rc.x);
            return res == 0 ? Double.compare(o1.rc.y, o2.rc.y) : res;
        }
    };

    public static List<Gob> sortByNearest(List<Gob> gobs, Coord2d start) {
        List<Gob> sorted = new ArrayList<>();
        List<Gob> remaining = new ArrayList<>(gobs);

        Coord2d current = start;

        while (!remaining.isEmpty()) {
            Coord2d finalCurrent = current;
            Gob closest = remaining.stream()
                    .min(Comparator.comparingDouble(g -> g.rc.dist(finalCurrent)))
                    .orElse(null);

            if (closest != null) {
                sorted.add(closest);
                remaining.remove(closest);
                current = closest.rc;
            } else {
                break;
            }
        }

        return sorted;
    }

    public static Entry getAnimalEntity(Gob gob, Class<? extends Entry> cattleRoster ){
        GetAnimalEntry gae = new GetAnimalEntry(gob,cattleRoster);
        try {
            NUtils.getUI().core.addTask(gae);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return gae.getResult();
    }

    public static void takeFromEarth(Gob gob) throws InterruptedException {
        if(gob!=null)
        {
            int size = NUtils.getGameUI().getInventory().getItems().size();
            getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 1, (int) gob.id,
                    gob.rc.floor(posres), 0, -1);
            getUI().core.addTask(new NoGob(gob.id));
            if(NUtils.getGameUI().getInventory().getFreeSpace()<5)
                getUI().core.addTask(new WaitAnotherSize(NUtils.getGameUI().getInventory(), size));
        }
    }

    public static void transferToBelt() {
        if(NUtils.getEquipment()!=null && NUtils.getEquipment().quickslots[NEquipory.Slots.BELT.idx]!=null)
            NUtils.getEquipment().quickslots[NEquipory.Slots.BELT.idx].item.wdgmsg("itemact",0);
    }

    public static double getFuelLvl(String cap, Color c) {
        Window inv;
        if ((inv = getGameUI().getWindow(cap)) != null) {
            for (Widget w = inv.lchild; w != null; w = w.prev) {
                if (w instanceof VMeter) {
                    for (LayerMeter.Meter meter : ((VMeter) w).meters) {
                        if (meter.c.getRed() == c.getRed() && meter.c.getBlue() == c.getBlue() && meter.c.getGreen() == c.getGreen())
                            return meter.a;
                    }
                }
            }
        }
        return 0;
    }

    public static void drop(WItem item) throws InterruptedException {
        item.item.wdgmsg("drop", item.sz, getGameUI().map.player().rc, 0);
    }

    public static void itemact(WItem item) throws InterruptedException {
        item.item.wdgmsg ( "itemact", 0 );
    }

    public static void addTask(NTask task) throws InterruptedException {
        if(NUtils.getUI()!=null)
            NUtils.getUI().core.addTask(task);
    }

    public static void setQuestConds(int id, Object... args)
    {
        NGameUI gui = getGameUI();
        if(gui!=null)
        {
            gui.questinfo.updateConds(id, args);
        }
    }

    public static void removeQuest(int id) {
        NGameUI gui = getGameUI();
        if(gui!=null) {
            gui.questinfo.removeQuest(id);
        }
    }

    public static void addQuest(int id) {
        NGameUI gui = getGameUI();
        if(gui!=null) {
            gui.questinfo.addQuest(id);
        }
    }

    public static float getDeltaZ() {
        return getUI().getDeltaZ();
    }

    public static ArrayList<Pattern> getQAPatterns() {
        ArrayList<Pattern> patterns = new ArrayList<>();
        for(QuickActions.ActionsItem ai : ((OptWnd.NSettingsPanel)getGameUI ().opts.nqolwnd).settingsWindow.qa.patterns)
        {
            if(ai.isEnabled.a)
                patterns.add(Pattern.compile(ai.text()));
        }
        return patterns;
    }

    public static ArrayList<String> getPetals() {
        ArrayList<String> vals = new ArrayList<>();
        for(AutoSelection.AutoSelectItem ai : ((OptWnd.NSettingsPanel)getGameUI ().opts.nqolwnd).settingsWindow.as.petals)
        {
            if(ai.isEnabled.a)
                vals.add(ai.text());
        }
        return vals;
    }

    public static String getIconInfo(String name) {
        return NStyle.iconName.get(name);
    }

    public static void setAutoMapperState(boolean state)
    {
        StatusWdg.status.set(state);
    }


    public static Coord toGC(Coord2d c) {
        return new Coord(Math.floorDiv((int) c.x, 1100), Math.floorDiv((int) c.y, 1100));
    }

    public static Coord toGridUnit(Coord2d c) {
        return new Coord(Math.floorDiv((int) c.x, 1100) * 1100, Math.floorDiv((int) c.y, 1100) * 1100);
    }

    public static Coord2d gridOffset(Coord2d c) {
        Coord gridUnit = toGridUnit(c);
        return new Coord2d(c.x - gridUnit.x, c.y - gridUnit.y);
    }

    public static void startBuild(Window window) {
        for (Widget sp = window.lchild; sp != null; sp = sp.prev) {
            if (sp instanceof Button) {
                if(((Button) sp).text!=null && ((Button) sp).text.text.equals("Build"))
                    ((Button) sp).click();
            }
        }
    }

    public static boolean isOverlay(
            Gob gob,
            NAlias name
    ) {
        for (Gob.Overlay ol : gob.ols) {
            if(ol.spr instanceof StaticSprite) {
                if(NParser.checkName((ol.spr).res.name,name))
                    return true;
            }
        }
        return false;
    }

    public static Coord2d findMountain(Pair<Coord2d, Coord2d> rcArea)
    {
        Coord2d pos = new Coord2d ( rcArea.a.x, rcArea.a.y );
        int count = 0;
        while ( pos.x <= rcArea.b.x ) {
            while ( pos.y <= rcArea.b.y ) {
                Coord pltc = ( new Coord2d ( pos.x / 11, pos.y / 11 ) ).floor ();
                Resource res_beg = NUtils.getGameUI().ui.sess.glob.map.tilesetr ( NUtils.getGameUI().ui.sess.glob.map.gettile ( pltc ) );
                if ( NParser.checkName ( res_beg.name, new NAlias( "mountain" ) ) ) {
                    return new Coord2d(pos.x, pos.y);
                }
                pos.y += MCache.tilesz.y;
            }
            pos.y = rcArea.a.y;
            pos.x += MCache.tilesz.x;
        }
        return null;
    }

    public static void activateRoastspit(Gob.Overlay ol) {
        getGameUI().map.wdgmsg("itemact", Coord.z, ol.gob.rc.floor(posres), 0, 1, (int)  ol.gob.id,
                ol.gob.rc.floor(posres), ol.id, -1);
    }

    public static void stackSwitch(boolean state)
    {
        NInventory inv = (NInventory) NUtils.getGameUI().maininv;
        if (inv.bundle.a != state) {
            MenuGrid.PagButton but = inv.pagBundle;
            if (but != null) {
                but.use(new MenuGrid.Interaction(1, 0));
            }
        }
    }

    public static boolean barrelHasContent(Gob barrel) {
        for (Gob.Overlay ol : barrel.ols) {
            if(ol.spr instanceof StaticSprite) {
                return true;
            }
        }
        return false;
    }

    public static String getContentsOfBarrel(Gob barrel) {
        for (Gob.Overlay ol : barrel.ols) {
            if(ol.spr instanceof StaticSprite) {
                return ((StaticSprite)ol.spr).res.name;
            }
        }
        return null;
    }

    public static String getContentsOfBucket(WItem bucket) {
        for(NGItem.NContent content : ((NGItem) bucket.item).content()) {
            if (content.name().contains("l of")) {
                return content.name();
            }
        }

        return "";
    }

    public static boolean bucketIsFull(WItem bucket) {
        for(NGItem.NContent content : ((NGItem) bucket.item).content()) {
            if (content.name().contains("10")) {
                return true;
            }
        }

        return false;
    }

    public static void dropLastSfx() {
        getUI().root.lastSfx = null;
    }

    private static final Pattern RESID = Pattern.compile(".*\\[([^,]*),?.*]");

    public static Object prettyResName(String resname) {
        Matcher m = RESID.matcher(resname);
        if(m.matches()) {
            resname = m.group(1);
        }
        int k = resname.lastIndexOf("/");
        resname = resname.substring(k + 1);
        resname = resname.substring(0, 1).toUpperCase() + resname.substring(1);
        return resname;
    }

    public static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при вычислении хэша SHA-256", e);
        }
    }

    public static void openDoor(NGameUI gui) throws InterruptedException {
        Gob arch = Finder.findGob(player().rc, new NAlias("gfx/terobjs/arch/stonestead", "gfx/terobjs/arch/stonemansion", "gfx/terobjs/arch/greathall", "gfx/terobjs/arch/primitivetent", "gfx/terobjs/arch/windmill", "gfx/terobjs/arch/stonetower", "gfx/terobjs/arch/logcabin", "gfx/terobjs/arch/timberhouse", "gfx/terobjs/minehole", "gfx/terobjs/ladder"), null, 100);
        if (arch != null) {
            if (NParser.checkName(arch.ngob.name, "gfx/terobjs/arch/greathall")) {
                Coord2d A = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.begin.y).rot(arch.a).add(arch.rc);
                Coord2d B = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.end.y).rot(arch.a).add(arch.rc);
                Coord2d C = B.sub(A).div(2).add(A);
                double a = A.add(B.sub(A).div(4)).dist(player().rc);
                double b = B.add(A.sub(B).div(4)).dist(player().rc);
                double c = C.dist(player().rc);
                if (a < b && a < c)
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 18);
                else if (b < c && b < a)
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 16);
                else
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 17);
            } else {
                gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                        0, 16);
            }
        }
    }

    public static void openDoorOnAGob(NGameUI gui, Gob arch) throws InterruptedException {
        if (arch != null) {
            if (NParser.checkName(arch.ngob.name, "gfx/terobjs/arch/greathall")) {
                Coord2d A = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.begin.y).rot(arch.a).add(arch.rc);
                Coord2d B = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.end.y).rot(arch.a).add(arch.rc);
                Coord2d C = B.sub(A).div(2).add(A);
                double a = A.add(B.sub(A).div(4)).dist(player().rc);
                double b = B.add(A.sub(B).div(4)).dist(player().rc);
                double c = C.dist(player().rc);
                if (a < b && a < c)
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 18);
                else if (b < c && b < a)
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 16);
                else
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 17);
            } else {
                gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                        0, 16);
            }
        }
    }

    public static int calcStackSize(Widget stackf)
    {
        int res = 0;
        for (Widget widget = stackf; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                res++;
            }
        }
        return res;
    }

    public static void setSpeed(int value) {
        if ( getGameUI() != null && getGameUI().speedget!=null) {
            getGameUI().speedget.set(value);
        }
    }

    public static RoutePoint findNearestPoint()
    {
        return ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI());
    }

    public static boolean isWorkStationReady(String name, Gob workstation) {
        if (workstation == null) {
            return false;
        }

        // Crucible is ready when it has coal (bit 2 set in modelAttribute)
        if (name.contains("crucible")) {
            return (workstation.ngob.getModelAttribute() & 2) == 2;
        }
        // For pow (forges), they're ready when not burning (bit 48)
        else if (name.startsWith("gfx/terobjs/pow")) {
            return (workstation.ngob.getModelAttribute() & 48) == 0;
        }
        // For cauldrons, they must be burning (bit 2) and have liquid (bit 8)
        else if (name.startsWith("gfx/terobjs/cauldron")) {
            return (workstation.ngob.getModelAttribute() & 2) == 2 &&
                    (workstation.ngob.getModelAttribute() & 8) == 8;
        }

        // For all other workstations, assume they're ready if they exist
        return true;
    }
}
