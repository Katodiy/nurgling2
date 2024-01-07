package nurgling;

import haven.*;
import haven.res.gfx.hud.rosters.cow.Ochs;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.*;

import java.text.*;
import java.util.*;

import static haven.OCache.posres;

public class NUtils
{
    public static long getTickId()
    {
        if(GameUI.getInstance()!= null )
            return  ((NUI)GameUI.getInstance().ui).tickId;
        return -1;
    }

    public static NGameUI getGameUI(){
        return (NGameUI) GameUI.getInstance();
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
        WaitItemInHand tith = new WaitItemInHand(item, getGameUI().getInventory());
        getUI().core.addTask(tith);
        return getGameUI().vhand;
    }

    public static NFlowerMenu getFlowerMenu() throws InterruptedException
    {
        FindNFlowerMenu fnf = new FindNFlowerMenu();
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
        if ( getGameUI().equwnd != null ) {
            for ( Widget w = getGameUI().equwnd.lchild ; w != null ; w = w.prev ) {
                if ( w instanceof Equipory ) {
                    return ( NEquipory ) w;
                }
            }
        }
        return null;
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
            getGameUI().map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 1, (int) gob.id,
                    gob.rc.floor(posres), 0, -1);
            getUI().core.addTask(new NoGob(gob.id));
        }
    }

    public static void transferToBelt() {
        if(NUtils.getEquipment()!=null && NUtils.getEquipment().quickslots[NEquipory.Slots.BELT.idx]!=null)
            NUtils.getEquipment().quickslots[NEquipory.Slots.BELT.idx].item.wdgmsg("itemact",0);
    }
}
