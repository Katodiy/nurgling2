package nurgling.actions;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;

public class TransferToContainer implements Action
{

    NAlias items;

    Container container;

    Integer th = -1;

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


    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        ArrayList<WItem> witems;
        if (!(witems = gui.getInventory().getItems(items)).isEmpty() && container.getattr(Container.Space.class) != null && (!container.getattr(Container.Space.class).isReady() || container.getattr(Container.Space.class).getFreeSpace() != 0))
        {
            Gob gcont = Finder.findGob(container.gobid);
            if (gcont == null)
                return Results.FAIL();
            PathFinder pf = new PathFinder(gcont);
            pf.isHardMode = true;
            pf.run(gui);
            if (th == -1)
                witems = gui.getInventory().getItems(items);
            else
                witems = gui.getInventory().getItems(items, th);

            // Проверяем готовность данных и логируем перед началом
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
                                if (witem.item.spr.sz().div(UI.scale(32)).equals(coord.y, coord.x))
                                {
                                    coorditems.add(witem);
                                }
                            }
                            int target_size = Math.min(numberFreeCoord, coorditems.size());
                            while (Math.min(gui.getInventory(container.cap).getNumberFreeCoord(coord), coorditems.size()) > 0)
                            {
                                WItem cand = coorditems.get(0);
                                transfer(cand, gui.getInventory(container.cap), target_size);
                                NUtils.addTask(new ISRemoved(cand.item.wdgid()));
                                if (th == -1)
                                    witems = gui.getInventory().getItems(items);
                                else
                                    witems = gui.getInventory().getItems(items, th);
                                coorditems = new ArrayList<>();
                                for (WItem witem : witems)
                                {
                                    if (witem.item.spr.sz().div(UI.scale(32)).equals(coord.y, coord.x))
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
                    // Получаем начальный список доступных предметов
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

                    // Определяем этап работы и сортируем предметы по соответствующему приоритету
                    String itemName = ((NGItem) availableItems.get(0).item).name();

                    // Проверяем, есть ли в целевом инвентаре цели для заполнения
                    boolean hasTargetsToFill = (gui.getInventory(container.cap).findNotStack(itemName) != null || gui.getInventory(container.cap).findNotFullStack(itemName) != null);

                    if (hasTargetsToFill)
                    {
                        availableItems = sortItemsByPriority(availableItems, itemName, false); // Этап заполнения
                    } else
                    {
                        availableItems = sortItemsByPriority(availableItems, itemName, true); // Этап переноса
                    }

                    // Для заполнения стаков продолжаем пока есть доступные предметы и цели для заполнения
                    transfer_size = availableItems.size(); // Переносим все доступные предметы

                    if (container.getattr(Container.TargetItems.class) != null && container.getattr(Container.TargetItems.class).getRes().containsKey(Container.TargetItems.MAXNUM))
                    {
                        int need = (Integer) container.getattr(Container.TargetItems.class).getRes().get(Container.TargetItems.MAXNUM) - (Integer) container.getattr(Container.TargetItems.class).getTargets(items);
                        transfer_size = Math.min(transfer_size, need);
                    }


                    int oldSpace = gui.getInventory(container.cap).getItems(items).size();
                    int transferred = 0;

                    while (!availableItems.isEmpty())
                    {
                        WItem currentItem = availableItems.get(0);

                        // Проверяем что предмет всё ещё валиден
                        if (!NGItem.validateItem(currentItem))
                        {
                            availableItems.remove(0);
                            continue;
                        }


                        int itemsTransferred = transfer(currentItem, gui.getInventory(container.cap), transfer_size);

                        if (itemsTransferred > 0)
                        {
                            transferred += itemsTransferred;

                            // Удаляем использованный предмет из списка
                            availableItems.remove(currentItem);
                        }
                        else
                        {
                            break;
                        }

                        // Обновляем общий список предметов после каждого переноса
                        if (th == -1)
                            witems = gui.getInventory().getItems(items);
                        else
                            witems = gui.getInventory().getItems(items, th);

                        // Полностью обновляем availableItems вместо фильтрации, так как новые предметы могут появиться
                        availableItems.clear();
                        for (WItem witem : witems)
                        {
                            if (NGItem.validateItem(witem))
                            {
                                availableItems.add(witem);
                            }
                        }

                        // Пересортируем по приоритету с учетом текущего этапа
                        if (!availableItems.isEmpty())
                        {
                            // Проверяем этап еще раз после обновления
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

    /**
     * Сортируем предметы по приоритету в зависимости от этапа
     *
     * @param items         список предметов
     * @param itemName      имя предмета
     * @param transferStage true - этап переноса (приоритет полным стакам), false - этап заполнения
     */
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
                int maxStackSize = StackSupporter.getMaxStackSize(itemName);
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

        ArrayList<WItem> result = new ArrayList<>();

        if (transferStage)
        {
            // ЭТАП 2: Перенос в новые слоты - приоритет полным стакам
            result.addAll(fullStacks);     // 1. Полные стаки
            result.addAll(notFullStacks);  // 2. Неполные стаки
            result.addAll(singleItems);    // 3. Отдельные предметы
        } else
        {
            // ЭТАП 1: Заполнение целевых стаков - приоритет неполным стакам
            result.addAll(notFullStacks);  // 1. Неполные стаки
            result.addAll(singleItems);    // 2. Отдельные предметы
            result.addAll(fullStacks);     // 3. Полные стаки
        }

        return result;
    }


    public static int transfer(WItem item, NInventory targetInv, int transfer_size) throws InterruptedException
    {
        // Проверяем валидность предмета перед транспортировкой
        if (!NGItem.validateItem(item))
        {
            return 0;
        }

        String itemName = ((NGItem) item.item).name();

        if (!StackSupporter.isStackable(targetInv, itemName))
        {
            if(targetInv.getFreeSpace() == 0)
                return 0;
            if (item.parent instanceof ItemStack)
            {
                ItemStack sourceStack = (ItemStack) item.parent;
                int originalStackSize = sourceStack.wmap.size();
                item.parent.wdgmsg("invxf", targetInv.wdgid(), 1);
                int id = item.parent.wdgid();
                if (originalStackSize <= 2)
                {
                    NUtils.addTask(new ISRemoved(id));
                } else
                {
                    NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                }
                return 1; // Переносим 1 предмет из стака
            } else
            {
                item.item.wdgmsg("transfer", Coord.z);
                int id = item.item.wdgid();
                NUtils.addTask(new ISRemoved(id));
                return 1; // Переносим 1 одиночный предмет
            }
        } else
        {
            // Обрабатываем стакуемые предметы с приоритетом на заполнение не полных стаков
            // Проверяем, является ли предмет изначально частью ItemStack
            if (item.parent instanceof ItemStack)
            {
                ItemStack sourceStack = (ItemStack) item.parent;

                // Приоритет целей: одиночные предметы, затем заполняемые стаки, затем новые слоты
                WItem targetSingleItem = targetInv.findNotStack(itemName);
                ItemStack targetNotFullStack = targetInv.findNotFullStack(itemName);

                if (targetSingleItem != null)
                {
                    // Сохраняем исходный размер стека ДО взятия предмета
                    int originalStackSize = sourceStack.wmap.size();

                    NUtils.takeItemToHand(item);
                    NUtils.itemact(targetSingleItem);
                    NUtils.addTask(new WaitFreeHand());

                    // Для стака размером 2 используем ISRemovedLoftar
                    if (originalStackSize <= 2)
                    {
                        NUtils.addTask(new ISRemovedLoftar(sourceStack.wdgid(), sourceStack, originalStackSize));
                    } else
                    {
                        NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                    }
                    return 1; // Переносим 1 предмет из стака к одиночному предмету
                } else if (targetNotFullStack != null)
                {
                    // Если нет одиночных предметов, заполняем неполные стаки
                    // Сохраняем исходный размер стека ДО взятия предмета
                    int originalStackSize = sourceStack.wmap.size();

                    NUtils.takeItemToHand(item);
                    NUtils.itemact(((NGItem) ((GItem.ContentsWindow) targetNotFullStack.parent).cont).wi);
                    NUtils.addTask(new WaitFreeHand());

                    // Для стака размером 2 используем ISRemovedLoftar
                    if (originalStackSize <= 2)
                    {
                        NUtils.addTask(new ISRemovedLoftar(sourceStack.wdgid(), sourceStack, originalStackSize));
                    } else
                    {
                        NUtils.addTask(new StackSizeChanged(sourceStack, originalStackSize));
                    }
                    return 1; // Переносим 1 предмет из стака к стаку
                } else
                {
                    int oldstacksize = sourceStack.wmap.size();
                    // Если НЕТ неполных стаков в целевом инвентаре и есть свободное место - переносим полный стак целиком
                    if (targetInv.calcFreeSpace() > 0)
                    {
                        ((GItem.ContentsWindow) sourceStack.parent).cont.wdgmsg("transfer", Coord.z);
                        NUtils.addTask(new ISRemoved(sourceStack.wdgid()));
                        return oldstacksize;
                    }
                    return 0;
                }
            } else
            {
                // Обрабатываем отдельные предметы: приоритет заполнения не полных стаков в целевом инвентаре
                ItemStack targetNotFullStack = targetInv.findNotFullStack(itemName);
                WItem targetSingleItem = null;

                if (targetNotFullStack != null)
                {
                    NUtils.takeItemToHand(item);
                    NUtils.itemact(((NGItem) ((GItem.ContentsWindow) targetNotFullStack.parent).cont).wi);
                    NUtils.addTask(new WaitFreeHand());
                } else if ((targetSingleItem = targetInv.findNotStack(itemName)) != null)
                {
                    NUtils.takeItemToHand(item);
                    NUtils.itemact(targetSingleItem);
                    NUtils.addTask(new WaitFreeHand());
                }
                else
                {
                    if (targetInv.calcFreeSpace() > 0)
                    {
                        item.item.wdgmsg("transfer", Coord.z);
                        int id = item.item.wdgid();
                        NUtils.addTask(new ISRemoved(id));
                    }
                    else
                        return 0;
                }
                return 1;
            }
        }
    }
}
