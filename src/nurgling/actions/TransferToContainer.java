package nurgling.actions;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TransferToContainer implements Action
{

    NAlias items;

    Container container;

    Integer th = -1;

    // When set, use exact name matching instead of NAlias substring matching
    String exactName = null;

    public TransferToContainer(Container container, NAlias items)
    {
        this.container = container;
        this.items = items;
    }

    public TransferToContainer(Container container, NAlias items, Integer th)
    {
        this.container = container;
        this.items = items;
        this.th = th;
    }

    public TransferToContainer(Container container, String exactName, Integer th)
    {
        this.container = container;
        this.exactName = exactName;
        this.items = new NAlias(exactName);
        this.th = th;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        ArrayList<WItem> witems;
        if (!(witems = getMatchingItems(gui)).isEmpty() && container.getattr(Container.Space.class) != null && (!container.getattr(Container.Space.class).isReady() || container.getattr(Container.Space.class).getFreeSpace() != 0))
        {
            Gob gcont = Finder.findGob(container.gobid);
            if (gcont == null)
                return Results.FAIL();
            PathFinder pf = new PathFinder(gcont);
            pf.isHardMode = true;
            pf.run(gui);
            witems = getMatchingItems(gui);

            for (WItem witem : witems)
            {
                if (NGItem.validateItem(witem))
                {
                }
            }

            if (container.cap != null)
            {
                new OpenTargetContainer(container.cap, Finder.findGob(container.gobid)).run(gui);
            }

            Container.Tetris tetris;
            if ((tetris = container.getattr(Container.Tetris.class)) != null)
            {
                for (Coord coord : (ArrayList<Coord>) tetris.getRes().get(Container.Tetris.TARGET_COORD))
                {
                    if (!(Boolean) tetris.getRes().get(Container.Tetris.DONE))
                    {
                        int numberFreeCoord = gui.getInventory(container.cap).getNumberFreeCoord(coord);
                        if (numberFreeCoord > 0)
                        {
                            ArrayList<WItem> coorditems = new ArrayList<>();
                            for (WItem witem : witems)
                            {
                                if (witem.item.spr != null && witem.item.spr.sz().div(UI.scale(32)).equals(coord.y, coord.x))
                                {
                                    coorditems.add(witem);
                                }
                            }
                            int target_size = Math.min(numberFreeCoord, coorditems.size());
                            while (Math.min(gui.getInventory(container.cap).getNumberFreeCoord(coord), coorditems.size()) > 0)
                            {
                                WItem cand = coorditems.get(0);
                                transfer(cand, gui.getInventory(container.cap), target_size);
                                witems = getMatchingItems(gui);
                                coorditems = new ArrayList<>();
                                for (WItem witem : witems)
                                {
                                    if (witem.item.spr != null && witem.item.spr.sz().div(UI.scale(32)).equals(coord.y, coord.x))
                                    {
                                        coorditems.add(witem);
                                    }
                                }
                            }
                            container.update();
                        }
                    }
                }
            } else
            {
                if (!witems.isEmpty())
                {
                    ArrayList<WItem> availableItems = new ArrayList<>();
                    for (WItem witem : witems)
                    {
                        if (NGItem.validateItem(witem))
                        {
                            availableItems.add(witem);
                        }
                    }

                    if (availableItems.isEmpty())
                    {
                        return Results.SUCCESS();
                    }

                    String itemName = ((NGItem) availableItems.get(0).item).name();

                    boolean hasTargetsToFill = (gui.getInventory(container.cap).findNotStack(itemName) != null || gui.getInventory(container.cap).findNotFullStack(itemName) != null);

                    if (hasTargetsToFill)
                    {
                        availableItems = sortItemsByPriority(availableItems, itemName, false); // Р­С‚Р°Рї Р·Р°РїРѕР»РЅРµРЅРёСЏ
                    } else
                    {
                        availableItems = sortItemsByPriority(availableItems, itemName, true); // Р­С‚Р°Рї РїРµСЂРµРЅРѕСЃР°
                    }

                    transfer_size = availableItems.size();

                    // Check ItemCount updater first (new system)
                    if (container.getattr(Container.ItemCount.class) != null)
                    {
                        Container.ItemCount itemCount = container.getattr(Container.ItemCount.class);
                        // Update ItemCount to get current state (container is now open)
                        itemCount.update();
                        int currentInContainer = itemCount.getCurrentCount();
                        int need = itemCount.getNeeded();
                        gui.msg("TransferToContainer: ItemCount current=" + currentInContainer + ", needed=" + need + ", available=" + transfer_size);
                        transfer_size = Math.min(transfer_size, need);
                        gui.msg("TransferToContainer: Will transfer max " + transfer_size + " items");
                    }
                    // Fall back to deprecated TargetItems if ItemCount not present
                    else if (container.getattr(Container.TargetItems.class) != null && container.getattr(Container.TargetItems.class).getRes().containsKey(Container.TargetItems.MAXNUM))
                    {
                        int need = (Integer) container.getattr(Container.TargetItems.class).getRes().get(Container.TargetItems.MAXNUM) - (Integer) container.getattr(Container.TargetItems.class).getTargets(items);
                        transfer_size = Math.min(transfer_size, need);
                    }


                    int oldSpace = gui.getInventory(container.cap).getItems(items).size();
                    int transferred = 0;

                    while (!availableItems.isEmpty() && transferred < transfer_size)
                    {
                        WItem currentItem = availableItems.get(0);

                        if (!NGItem.validateItem(currentItem))
                        {
                            availableItems.remove(0);
                            continue;
                        }

                        // Calculate remaining items we can transfer
                        int remainingToTransfer = transfer_size - transferred;
                        int itemsTransferred = transfer(currentItem, gui.getInventory(container.cap), remainingToTransfer);

                        if (itemsTransferred > 0)
                        {
                            transferred += itemsTransferred;

                            availableItems.remove(currentItem);
                        }
                        else
                        {
                            break;
                        }

                        witems = getMatchingItems(gui);

                        availableItems.clear();
                        for (WItem witem : witems)
                        {
                            if (NGItem.validateItem(witem))
                            {
                                availableItems.add(witem);
                            }
                        }

                        if (!availableItems.isEmpty())
                        {
                            boolean hasTargetsToFillRefresh = (gui.getInventory(container.cap).findNotStack(itemName) != null || gui.getInventory(container.cap).findNotFullStack(itemName) != null);

                            if (hasTargetsToFillRefresh)
                            {
                                availableItems = sortItemsByPriority(availableItems, itemName, false);
                            } else
                            {
                                availableItems = sortItemsByPriority(availableItems, itemName, true);
                            }

                        }
                    }

                    NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(container.cap), items, oldSpace + transferred));
                }
            }
            container.update();
        }
        return Results.SUCCESS();
    }

    int transfer_size = 0;

    public int getResult()
    {
        return transfer_size;
    }

    private static final Comparator<WItem> QUALITY_DESC = new Comparator<WItem>()
    {
        @Override
        public int compare(WItem a, WItem b)
        {
            Float qa = ((NGItem) a.item).quality;
            Float qb = ((NGItem) b.item).quality;
            if (qa == null && qb == null) return 0;
            if (qa == null) return 1;
            if (qb == null) return -1;
            return Float.compare(qb, qa);
        }
    };

    private static ArrayList<WItem> sortItemsByPriority(ArrayList<WItem> items, String itemName, boolean transferStage)
    {
        ArrayList<WItem> notFullStacks = new ArrayList<>();
        ArrayList<WItem> singleItems = new ArrayList<>();
        ArrayList<WItem> fullStacks = new ArrayList<>();

        for (WItem item : items)
        {
            if (!NGItem.validateItem(item)) continue;

            if (item.parent instanceof ItemStack)
            {
                ItemStack stack = (ItemStack) item.parent;
                int maxStackSize = StackSupporter.getFullStackSize(itemName);
                if (stack.wmap.size() < maxStackSize)
                {
                    notFullStacks.add(item);
                } else
                {
                    fullStacks.add(item);
                }
            } else
            {
                singleItems.add(item);
            }
        }

        Collections.sort(notFullStacks, QUALITY_DESC);
        Collections.sort(singleItems, QUALITY_DESC);
        Collections.sort(fullStacks, QUALITY_DESC);

        ArrayList<WItem> result = new ArrayList<>();

        if (transferStage)
        {
            result.addAll(fullStacks);
            result.addAll(notFullStacks);
            result.addAll(singleItems);
        } else
        {
            result.addAll(notFullStacks);
            result.addAll(singleItems);
            result.addAll(fullStacks);
        }

        return result;
    }


    /**
     * Ждёт освобождения руки. Если таймаут — возвращает предмет в основной инвентарь.
     * @return true если рука освободилась, false если таймаут (предмет возвращён)
     */
    private static boolean waitFreeHandOrReturn() throws InterruptedException
    {
        WaitFreeHand wfh = new WaitFreeHand();
        NUtils.addTask(wfh);
        if (wfh.criticalExit)
        {
            NUtils.dropToInv();
            NUtils.addTask(new WaitFreeHand());
            return false;
        }
        return true;
    }

    public static int transfer(WItem item, NInventory targetInv, int transfer_size) throws InterruptedException
    {
        if (!NGItem.validateItem(item))
        {
            return 0;
        }

        String itemName = ((NGItem) item.item).name();

        // Check if stacking is disabled (bundle.a == false) OR item is not stackable
        boolean stackingDisabled = !((NInventory) NUtils.getGameUI().maininv).bundle.a;
        if (stackingDisabled || !StackSupporter.isStackable(targetInv, itemName))
        {
            if(targetInv.getFreeSpace() == 0)
            {
                return 0;
            }
            if (item.parent instanceof ItemStack)
            {
                ItemStack sourceStack = (ItemStack) item.parent;
                int originalStackSize = sourceStack.wmap.size();
                item.parent.wdgmsg("invxf", targetInv.wdgid(), 1);
                int id = ((GItem.ContentsWindow) sourceStack.parent).cont.wdgid();
                if (originalStackSize <= 2)
                {
                    NUtils.addTask(new ISRemoved(id));
                } else
                {
                    NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                }
                return 1;
            } else
            {
                int id = item.item.wdgid();
                item.item.wdgmsg("transfer", Coord.z);
                NUtils.addTask(new NTask()
                {
                    int count = 0;
                    @Override
                    public boolean check()
                    {
                        return NUtils.getUI().getwidget(id)==null || (targetInv.calcFreeSpace() == 0 && count++>200);
                    }
                });
                return 1;
            }
        }
        else
        {
            if (item.parent instanceof ItemStack)
            {
                ItemStack sourceStack = (ItemStack) item.parent;
                int sourceStackSize = sourceStack.wmap.size();

                WItem targetSingleItem = targetInv.findNotStack(itemName);
                ItemStack targetNotFullStack = targetInv.findNotFullStack(itemName);

                if (targetSingleItem != null)
                {
                    int originalStackSize = sourceStack.wmap.size();

                    NUtils.takeItemToHand(item);
                    NUtils.itemact(targetSingleItem);
                    if (!waitFreeHandOrReturn())
                        return 0;

                    if (originalStackSize <= 2)
                    {
                        if(((GItem.ContentsWindow) sourceStack.parent!=null))
                            NUtils.addTask(new ISRemovedLoftar(((GItem.ContentsWindow) sourceStack.parent).cont.wdgid(), sourceStack, originalStackSize));
                    } else
                    {
                        NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                    }
                    return 1;
                }
                else if (targetNotFullStack != null)
                {
                    int targetStackSize = targetNotFullStack.wmap.size();
                    int originalStackSize = sourceStack.wmap.size();

                    NUtils.takeItemToHand(item);
                    NUtils.itemact(((NGItem) ((GItem.ContentsWindow) targetNotFullStack.parent).cont).wi);
                    if (!waitFreeHandOrReturn())
                        return 0;

                    if (originalStackSize <= 2)
                    {
                        if(sourceStack.parent!=null)
                            NUtils.addTask(new ISRemovedLoftar(((GItem.ContentsWindow) sourceStack.parent).cont.wdgid(), sourceStack, originalStackSize));
                    } else
                    {
                        NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                    }
                    NUtils.addTask(new StackSizeChanged(targetNotFullStack, targetStackSize));

                    return 1;
                }
                else
                {
                    int oldstacksize = sourceStack.wmap.size();
                    if (targetInv.getFreeSpace() > 0)
                    {
                        if (oldstacksize > transfer_size)
                        {
                            int originalStackSize = sourceStack.wmap.size();

                            NUtils.takeItemToHand(item);
                            NUtils.dropToInv(targetInv);
                            if (!waitFreeHandOrReturn())
                                return 0;

                            if (originalStackSize <= 2)
                            {
                                if(((GItem.ContentsWindow) sourceStack.parent!=null))
                                    NUtils.addTask(new ISRemovedLoftar(((GItem.ContentsWindow) sourceStack.parent).cont.wdgid(), sourceStack, originalStackSize));
                            } else
                            {
                                NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                            }
                            return 1;
                        }
                        else
                        {
                            ((GItem.ContentsWindow) sourceStack.parent).cont.wdgmsg("transfer", Coord.z);
                            if(((GItem.ContentsWindow) sourceStack.parent!=null))
                                NUtils.addTask(new ISRemoved( ((GItem.ContentsWindow) sourceStack.parent).cont.wdgid()));
                            return oldstacksize;
                        }
                    }
                    return 0;
                }
            } else
            {
                ItemStack targetNotFullStack = targetInv.findNotFullStack(itemName);
                WItem targetSingleItem = null;

                if (targetNotFullStack != null)
                {
                    int targetStackSize = targetNotFullStack.wmap.size();
                    NUtils.takeItemToHand(item);
                    NUtils.itemact(((NGItem) ((GItem.ContentsWindow) targetNotFullStack.parent).cont).wi);
                    if (!waitFreeHandOrReturn())
                        return 0;
                    NUtils.addTask(new StackSizeChanged(targetNotFullStack, targetStackSize));
                    return 1;
                } else if ((targetSingleItem = targetInv.findNotStack(itemName)) != null)
                {
                    NUtils.takeItemToHand(item);
                    NUtils.itemact(targetSingleItem);
                    if (!waitFreeHandOrReturn())
                        return 0;
                    NUtils.addTask(new GetNotFullStack(targetInv, new NAlias(itemName)));
                    return 1;
                }
                else
                {
                    // When no stacks or single items to merge with, transfer to free space if available
                    if (targetInv.getFreeSpace() > 0)
                    {
                        item.item.wdgmsg("transfer", Coord.z);
                        int id = item.item.wdgid();
                        NUtils.addTask(new ISRemoved(id));
                        return 1;
                    }
                    else
                    {
                        return 0;
                    }
                }
            }
        }
    }

    /**
     * Gets items from inventory, using exact name match if exactName is set,
     * otherwise uses NAlias substring matching.
     */
    private ArrayList<WItem> getMatchingItems(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> allItems;
        if (th == -1) {
            allItems = gui.getInventory().getItems(items);
        } else {
            allItems = gui.getInventory().getItems(items, th);
        }
        if (exactName == null) {
            return allItems;
        }
        ArrayList<WItem> exactMatches = new ArrayList<>();
        for (WItem witem : allItems) {
            if (((NGItem) witem.item).name().equals(exactName)) {
                exactMatches.add(witem);
            }
        }
        return exactMatches;
    }
}
