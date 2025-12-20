package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class DFrameFishAction implements Action {

    NAlias fish = new NAlias("Fish");
    NAlias fillet = new NAlias("Fillet");
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation rdframe = new NArea.Specialisation(Specialisation.SpecName.dframe.toString());
        NArea.Specialisation rrawfish = new NArea.Specialisation(Specialisation.SpecName.rawfish.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rdframe);
        req.add(rrawfish);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        
        if(new Validator(req, opt).run(gui).IsSuccess()) {
            // Получаем все drying frames
            ArrayList<Container> containers = new ArrayList<>();

            for (Gob dframe : Finder.findGobs(NContext.findSpec(Specialisation.SpecName.dframe.toString()),
                    new NAlias("gfx/terobjs/dframe"))) {
                Container cand = new Container(dframe, "Frame");
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }
            
            // Сортируем контейнеры для последовательной работы
            Pair<Coord2d,Coord2d> rca = NContext.findSpec(Specialisation.SpecName.dframe.toString()).getRCArea();
            boolean dir = rca.b.x - rca.a.x > rca.b.y - rca.a.y;
            containers.sort(new Comparator<Container>() {
                @Override
                public int compare(Container o1, Container o2) {
                    Gob gob1 = Finder.findGob(o1.gobid);
                    Gob gob2 = Finder.findGob(o2.gobid);
                    if(dir) {
                        int res = Double.compare(gob1.rc.y, gob2.rc.y);
                        if(res == 0)
                            return Double.compare(gob1.rc.x, gob2.rc.x);
                        else
                            return res;
                    } else {
                        int res = Double.compare(gob1.rc.x, gob2.rc.x);
                        if(res == 0)
                            return Double.compare(gob1.rc.y, gob2.rc.y);
                        else
                            return res;
                    }
                }
            });

            // Освобождаем сушилки от готового филе
            new FreeContainers(containers, fillet).run(gui);
            
            // Основной цикл обработки рыбы
            NContext context = new NContext(gui);
            Pair<Coord2d, Coord2d> fishArea = NContext.findSpec(Specialisation.SpecName.rawfish.toString()).getRCArea();
            
            while (hasEmptySlots(containers)) {
                // Проверяем, есть ли рыба в пайлах
                ArrayList<Gob> piles = Finder.findGobs(fishArea, new NAlias("stockpile"));
                if (piles.isEmpty()) {
                    break; // Нет рыбы - выходим
                }
                
                // Берем рыбу из пайлов
                if (gui.getInventory().getFreeSpace() < 3) {
                    // Если инвентарь заполнен, переносим филе на сушилки
                    if (!gui.getInventory().getItems(fillet).isEmpty()) {
                        fillDryingFrames(gui, containers);
                    }
                    // Если все еще мало места, сбрасываем остальное
                    if (gui.getInventory().getFreeSpace() < 3) {
                        new FreeInventory2(context).run(gui);
                    }
                }
                
                // Берем рыбу из пайла
                piles.sort(NUtils.d_comp);
                Gob pile = piles.get(0);
                new PathFinder(pile).run(gui);
                new OpenTargetContainer("Stockpile", pile).run(gui);
                
                // Берем минимум между свободным местом и доступным количеством на сушилках
                int freeSlots = calculateFreeSlots(containers);
                int takeAmount = Math.min(freeSlots, gui.getInventory().getFreeSpace());
                takeAmount = Math.min(takeAmount, 10); // Не берем слишком много за раз
                
                if (takeAmount > 0) {
                    new TakeItemsFromPile(pile, gui.getStockpile(), takeAmount).run(gui);
                }
                new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                
                // Обрабатываем взятую рыбу - делаем Butcher для каждой рыбины
                ArrayList<WItem> fishItems = gui.getInventory().getItems(fish);
                for (WItem fishItem : fishItems) {
                    // Выполняем Butcher действие для рыбы
                    int oldFilletCount = gui.getInventory().getItems(fillet).size();
                    new SelectFlowerAction("Butcher", fishItem).run(gui);
                    
                    // Ждем появления филе
                    NUtils.addTask(new WaitItems((NInventory) gui.maininv, fillet, oldFilletCount + 1));
                    
                    // Если накопилось много филе или инвентарь заполнен, вешаем на сушилки
                    if (gui.getInventory().getItems(fillet).size() >= 5 || gui.getInventory().getFreeSpace() < 2) {
                        fillDryingFrames(gui, containers);
                    }
                }
                
                // После обработки всей взятой рыбы вешаем оставшееся филе на сушилки
                if (!gui.getInventory().getItems(fillet).isEmpty()) {
                    fillDryingFrames(gui, containers);
                }
            }
            
            // В конце переносим все оставшееся филе на сушилки
            if (!gui.getInventory().getItems(fillet).isEmpty()) {
                fillDryingFrames(gui, containers);
            }
            
            // Переносим все остальное (побочные продукты) в соответствующие зоны
            new FreeInventory2(context).run(gui);;
            
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
    
    /**
     * Проверяет, есть ли свободные слоты в сушилках
     */
    private boolean hasEmptySlots(ArrayList<Container> containers) throws InterruptedException {
        for (Container cont : containers) {
            Container.Space space = cont.getattr(Container.Space.class);
            if (space != null) {
                if (!space.isReady()) {
                    // Если не готово, обновляем
                    space.update();
                }
                Integer freeSpace = (Integer) space.getRes().get(Container.Space.FREESPACE);
                if (freeSpace != null && freeSpace > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Подсчитывает общее количество свободных слотов во всех сушилках
     */
    private int calculateFreeSlots(ArrayList<Container> containers) throws InterruptedException {
        int totalFreeSlots = 0;
        for (Container cont : containers) {
            Container.Space space = cont.getattr(Container.Space.class);
            if (space != null) {
                if (!space.isReady()) {
                    space.update();
                }
                Integer freeSpace = (Integer) space.getRes().get(Container.Space.FREESPACE);
                if (freeSpace != null) {
                    totalFreeSlots += freeSpace;
                }
            }
        }
        return totalFreeSlots;
    }
    
    /**
     * Заполняет сушилки филе из инвентаря
     */
    private void fillDryingFrames(NGameUI gui, ArrayList<Container> containers) throws InterruptedException {
        for (Container container : containers) {
            if (gui.getInventory().getItems(fillet).isEmpty()) {
                break; // Филе закончилось
            }
            
            Container.Space space = container.getattr(Container.Space.class);
            if (!space.isReady()) {
                space.update();
            }
            
            Integer freeSpace = (Integer) space.getRes().get(Container.Space.FREESPACE);
            if (freeSpace != null && freeSpace > 0) {
                // Переносим филе в эту сушилку
                Gob dframe = Finder.findGob(container.gobid);
                if (dframe != null && PathFinder.isAvailable(dframe)) {
                    new PathFinder(dframe).run(gui);
                    new OpenTargetContainer(container).run(gui);
                    
                    // Переносим филе
                    int toTransfer = Math.min(freeSpace, gui.getInventory().getItems(fillet).size());
                    for (int i = 0; i < toTransfer; i++) {
                        ArrayList<WItem> fillets = gui.getInventory().getItems(fillet);
                        if (!fillets.isEmpty()) {
                            WItem filletItem = fillets.get(0);
                            NUtils.takeItemToHand(filletItem);
                            
                            NInventory frameInv = gui.getInventory(container.cap);
                            if (frameInv != null) {
                                Coord freeCoord = frameInv.findFreeCoord(new Coord(1, 1));
                                if (freeCoord != null) {
                                    frameInv.wdgmsg("drop", freeCoord);
                                    NUtils.addTask(new NTask() {
                                        @Override
                                        public boolean check() {
                                            return gui.vhand == null;
                                        }
                                    });
                                }
                            }
                        }
                    }
                    
                    new CloseTargetContainer(container).run(gui);
                    
                    // Обновляем информацию о свободном месте
                    space.update();
                }
            }
        }
    }
}

