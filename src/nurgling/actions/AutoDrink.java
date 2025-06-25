package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.MenuGrid;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.tasks.*;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static haven.OCache.posres;

public class AutoDrink implements Action
{

    public final static AtomicBoolean stop = new AtomicBoolean(false);

    public AutoDrink()
    {
        stop.set(false);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        while(!stop.get())
        {
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    if(NUtils.getGameUI()==null || NUtils.getGameUI().getmeter ( "stam", 0 )==null)
                        return false;
                    return (!NContext.waitBot.get() && NUtils.getStamina()<0.51) || stop.get();
                }
            });
            if(stop.get()) {
                NUtils.getUI().core.autoDrink = null;
                return Results.SUCCESS();
            }

            if(checkWater()) {
                NUtils.getUI().dropLastError();
                for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                    if (pag.button() != null && pag.button().name().equals("Drink")) {

                        pag.button().use(new MenuGrid.Interaction(1, 0));
                        WaitPoseOrMsg wops = new WaitPoseOrMsg(NUtils.player(), "gfx/borka/drinkan", new NAlias("You have nothing on your hotbelt to drink."));
                        NUtils.getUI().core.addTask(wops);
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return NUtils.player() == null || !NParser.checkName(NUtils.player().pose(), "gfx/borka/drinkan");
                            }
                        });
                    }
                }
            }
        }
        NUtils.getUI().core.autoDrink = null;
        return Results.SUCCESS();
    }

    boolean checkWater() throws InterruptedException
    {
        WItem wbelt = NUtils.getEquipment().findItem (NEquipory.Slots.BELT.idx);
        if(wbelt.item.contents!=null) {
            ArrayList<WItem> witems = ((NInventory) wbelt.item.contents).getItems(new NAlias("Waterskin"));
            if (!witems.isEmpty()) {
                for (WItem item : witems) {
                    NGItem ngItem = ((NGItem) item.item);
                    if (!ngItem.content().isEmpty()) {
                        if (ngItem.content().get(0).name().contains("Water")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
