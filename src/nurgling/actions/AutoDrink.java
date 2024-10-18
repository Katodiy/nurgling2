package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.MenuGrid;
import nurgling.NCore;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.NParser;

import java.util.concurrent.atomic.AtomicBoolean;

import static haven.OCache.posres;

public class AutoDrink implements Action
{
    public final static AtomicBoolean waitRefil = new AtomicBoolean(false);
    public final static AtomicBoolean waitBot = new AtomicBoolean(false);
    public final static AtomicBoolean stop = new AtomicBoolean(false);

    public AutoDrink()
    {
        waitRefil.set(false);
        waitBot.set(false);
        stop.set(false);
    }
    double lvl;
    boolean withStop;

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
                    return (!waitBot.get() && NUtils.getStamina()<0.51) || stop.get();
                }
            });
            if(stop.get()) {
                NUtils.getUI().core.autoDrink = null;
                return Results.SUCCESS();
            }

            NUtils.getUI().dropLastError();
            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae)
            {
                if(pag.button()!=null && pag.button().name().equals("Drink"))
                {

                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    WaitPoseOrMsg wops = new WaitPoseOrMsg(NUtils.player(),"gfx/borka/drinkan","You have nothing on your hotbelt to drink.");
                    NUtils.getUI().core.addTask(wops);
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return !NParser.checkName(NUtils.player().pose(),"gfx/borka/drinkan");
                        }
                    });
                    if (wops.isError())
                    {
                        waitRefil.set(true);
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return !waitRefil.get();
                            }
                        });
                        break;
                    }

                    break;
                }
            }
        }
        NUtils.getUI().core.autoDrink = null;
        return Results.SUCCESS();
    }
}
