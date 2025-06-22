package nurgling.actions;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;

public class TransferToContainer implements Action{

    NAlias items;

    Container container;
    Context context;

    Integer th = -1;
    public TransferToContainer(Context context, Container container, NAlias items) {
        this.container = container;
        this.items = items;
        this.context = context;
    }

    public TransferToContainer(Context context, Container container, NAlias items, Integer th) {
        this.container = container;
        this.items = items;
        this.context = context;
        this.th = th;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> witems;
        if (!(witems = gui.getInventory().getItems(items)).isEmpty() && (!container.getattr(Container.Space.class).isReady() || container.getattr(Container.Space.class).getFreeSpace()!=0)) {
            PathFinder pf = new PathFinder(container.gob);
            pf.isHardMode = true;
            pf.run(gui);
            if (th == -1)
                witems = gui.getInventory().getItems(items);
            else
                witems = gui.getInventory().getItems(items, th);
            if (container.cap != null) {
                new OpenTargetContainer(container.cap, container.gob).run(gui);
            }

            Container.Tetris tetris;
            if ((tetris = container.getattr(Container.Tetris.class)) != null) {

                for (Coord coord : (ArrayList<Coord>) tetris.getRes().get(Container.Tetris.TARGET_COORD)) {
                    if (!(Boolean) tetris.getRes().get(Container.Tetris.DONE)) {
                        int numberFreeCoord = gui.getInventory(container.cap).getNumberFreeCoord(coord);
                        if (numberFreeCoord > 0) {
                            ArrayList<WItem> coorditems = new ArrayList<>();
                            for (WItem witem : witems) {
                                if (witem.item.spr.sz().div(UI.scale(32)).equals(coord.y, coord.x)) {
                                    coorditems.add(witem);
                                }
                            }
                            int target_size = Math.min(numberFreeCoord, coorditems.size());
                            while(Math.min(gui.getInventory(container.cap).getNumberFreeCoord(coord),coorditems.size())>0) {
                                WItem cand = coorditems.get(0);
                                transfer(cand, gui.getInventory(container.cap), target_size);
                                NUtils.addTask(new ISRemoved(cand.item.wdgid()));
                                if (th == -1)
                                    witems = gui.getInventory().getItems(items);
                                else
                                    witems = gui.getInventory().getItems(items, th);
                                coorditems = new ArrayList<>();
                                for (WItem witem : witems) {
                                    if (witem.item.spr.sz().div(UI.scale(32)).equals(coord.y, coord.x)) {
                                        coorditems.add(witem);
                                    }
                                }

                            }
                            container.update();
                        }
                    }
                }

            } else {
                if(!witems.isEmpty()) {
                    transfer_size = Math.min(gui.getInventory().getItems(items).size(), Math.min(witems.size(), gui.getInventory(container.cap).getNumberFreeCoord(witems.get(0))));
                    if (container.getattr(Container.TargetItems.class) != null && container.getattr(Container.TargetItems.class).getRes().containsKey(Container.TargetItems.MAXNUM)) {
                        int need = (Integer) container.getattr(Container.TargetItems.class).getRes().get(Container.TargetItems.MAXNUM) - (Integer) container.getattr(Container.TargetItems.class).getTargets(items);
                        transfer_size = Math.min(transfer_size, need);
                    }

                    int oldSpace = gui.getInventory(container.cap).getItems(items).size();

                    int temptr = transfer_size;
                    for (int i = 0; i < temptr; i++) {
                        boolean fs = transfer(witems.get(i), gui.getInventory(container.cap), transfer_size);
                        temptr-=fs?(i+1):(i+StackSupporter.getMaxStackSize(items.getDefault()));
                        i = -1;
                        if (th == -1)
                            witems = gui.getInventory().getItems(items);
                        else
                            witems = gui.getInventory().getItems(items, th);

                    }
                    NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(container.cap), items, oldSpace + transfer_size));
                }
            }
            container.update();
        }
        return Results.SUCCESS();
    }
    int transfer_size = 0;

    public int getResult() {
        return transfer_size;
    }


    public static boolean transfer(WItem item, NInventory targetInv, int transfer_size) throws InterruptedException {
        if(!StackSupporter.isStackable(targetInv,((NGItem)item.item).name()))
        {
            if(item.parent instanceof ItemStack)
            {
                item.parent.wdgmsg("invxf", targetInv.wdgid(), 1);
                int id = item.parent.wdgid();
                if(((ItemStack)item.parent).wmap.size()<=2)
                {
                    NUtils.addTask(new ISRemoved(id));
                }
                else
                {
                    NUtils.addTask(new StackSizeChanged(((ItemStack)item.parent)));
                }
            }
            else
            {
                item.item.wdgmsg("transfer", Coord.z);
                int id = item.item.wdgid();
                NUtils.addTask(new ISRemoved(id));
            }
        }
        else
        {
            String name = ((NGItem)item.item).name();
            if(item.parent instanceof ItemStack)
            {
                ItemStack is = ((ItemStack)item.parent);
                if(StackSupporter.getMaxStackSize(name) == is.wmap.size() && transfer_size >= is.wmap.size() && targetInv.calcFreeSpace()!=0)
                {
                    ((GItem.ContentsWindow)is.parent).cont.wdgmsg("transfer", Coord.z);
                    NUtils.addTask(new ISRemoved(is.wdgid()));
                    return false;
                }
                else
                {
                    int id = is.wdgid();
                    is.wdgmsg("invxf", targetInv.wdgid(), 1);
                    if(is.wmap.size()<=2)
                    {
                        NUtils.addTask(new ISRemoved(id));
                    }
                    else
                    {
                        NUtils.addTask(new StackSizeChanged(is));
                    }
                }
            }
            else
            {
                ItemStack ois = targetInv.findNotFullStack(name);
                WItem targetForActivate = null;
                if(ois!=null)
                {
                    NUtils.takeItemToHand(item);
                    NUtils.itemact(((NGItem)((GItem.ContentsWindow)ois.parent).cont).wi);
                    NUtils.addTask(new WaitFreeHand());
                }
                else if((targetForActivate = targetInv.findNotStack(name))!=null)
                {
                    NUtils.takeItemToHand(item);
                    NUtils.itemact(targetForActivate);
                    NUtils.addTask(new WaitFreeHand());
                }
                else
                {
                    item.item.wdgmsg("transfer", Coord.z);
                    int id = item.item.wdgid();
                    NUtils.addTask(new ISRemoved(id));
                }
            }
        }
        return true;
    }
}
