package nurgling.actions;

import haven.Gob;
import haven.MenuGrid;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitNoItems;
import nurgling.tasks.WaitPoseOrMsg;
import nurgling.tools.*;
import nurgling.widgets.NEquipory;
import nurgling.widgets.NMakewindow;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class Forging implements Action
{
    ArrayList<Container> containers;
    NContext context;
    NMakewindow mwnd;
    public Forging(ArrayList<Container>containers, NContext context) {
        this.containers = containers;
        this.context = context;
    }

    static HashSet<String> targets = new HashSet<>();
    static {
        targets.add("Bloom");
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

        for(Container cont: containers)
        {
            Container.Space space = ((Container.Space)cont.getattr(Container.Space.class));
            while (space.getMaxSpace()!=space.getFreeSpace())
            {
                new PathFinder(Finder.findGob(cont.gobid)).run(gui);
                new OpenTargetContainer(cont).run(gui);
                new TakeItemsFromContainer(cont,targets,null).run(gui);
                new CloseTargetContainer(cont).run(gui);
                new Drink(0.9, false).run(gui);
                new UseWorkStation(context).run(gui);

                if(NUtils.getGameUI().craftwnd == null || NUtils.getGameUI().craftwnd .makeWidget == null || !NUtils.getGameUI().craftwnd.makeWidget.rcpnm.equals("Wrought Iron")) {
                    for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                        if (pag.button() != null && pag.button().name().equals("Wrought Iron")) {
                            pag.button().use(new MenuGrid.Interaction(1, 0));
                            break;
                        }
                    }
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            if (NUtils.getGameUI().craftwnd != null) {
                                mwnd = NUtils.getGameUI().craftwnd.makeWidget;
                                if (mwnd.rcpnm.equals("Wrought Iron"))
                                    return true;
                            }
                            return false;
                        }
                    });
                }
                else
                {
                    mwnd = NUtils.getGameUI().craftwnd.makeWidget;
                }
                mwnd.wdgmsg("make", 1);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return gui.prog != null && gui.prog.prog > 0;
                    }
                });
                NUtils.addTask(new WaitNoItems(NUtils.getGameUI().getInventory(),new NAlias("Bloom")));
            }
        }
        return Results.SUCCESS();
    }


}
