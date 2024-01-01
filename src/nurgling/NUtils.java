package nurgling;

import haven.*;
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

    public static void place(Gob gob, Coord2d coord2d) throws InterruptedException {
        getGameUI().map.wdgmsg("click", Coord.z, coord2d.floor(posres), 3, 0);
        getUI().core.addTask(new WaitPlaced(gob));
    }


}
