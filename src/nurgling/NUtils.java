package nurgling;

import haven.*;
import haven.Button;
import haven.Window;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import mapv4.StatusWdg;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;
import nurgling.widgets.options.AutoSelection;
import nurgling.widgets.options.QuickActions;

import java.awt.*;
import java.text.*;
import java.util.*;
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
        return NParser.checkName(name, "gfx/terobjs/tree", "gfx/terobjs/bumlings","gfx/terobjs/bushes","gfx/terobjs/stonepillar" );
    }

    public static WItem takeItemToHand(WItem item) throws InterruptedException
    {
        item.item.wdgmsg("take", Coord.z);
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

    public static String getCursorName()
    {
        return NUtils.getUI().root.cursorRes;
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
        for(QuickActions.ActionsItem ai : ((OptWnd.NQuickActionsPanel)getGameUI ().opts.nquickAct).qol_p.patterns)
        {
            if(ai.isEnabled.a)
                patterns.add(Pattern.compile(ai.text()));
        }
        return patterns;
    }

    public static ArrayList<String> getPetals() {
        ArrayList<String> vals = new ArrayList<>();
        for(AutoSelection.AutoSelectItem ai : ((OptWnd.NAutoSelectPanel)getGameUI ().opts.autosel).autosel_p.petals)
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

    public static void CheckGridCoord(Coord2d c) {
        Coord gc = NUtils.toGC(c);
        if(((NMapView)NUtils.getGameUI().map).lastGC == null || !gc.equals(((NMapView)NUtils.getGameUI().map).lastGC)) {
            EnterGrid(gc);
        }
    }

    public static void EnterGrid(Coord gc) {
        ((NMapView)NUtils.getGameUI().map).lastGC = gc;
        if (NUtils.getGameUI().areas.visible) {
            ((NMapView) NUtils.getGameUI().map).destroyDummys();
            ((NMapView) NUtils.getGameUI().map).initDummys();
        }
    }

    public static void startBuild(Window window) {
        for (Widget sp = window.lchild; sp != null; sp = sp.prev) {
            if (sp instanceof Button) {
                if(((Button) sp).text!=null && ((Button) sp).text.text.equals("Build"))
                    ((Button) sp).click();
            }
        }
    }
}
