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
import nurgling.tools.VSpec;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class DFrameFishAction implements Action {

    NAlias fish = new NAlias("Fish");
    NAlias dfillet = new NAlias("Dried Filet");
    NAlias rfillet = new NAlias(new ArrayList<>(Arrays.asList("Filet")),new ArrayList<>(Arrays.asList("Dried")));

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
            new FreeContainers(containers, dfillet).run(gui);
            
            // Основной цикл обработки рыбы
            NContext context = new NContext(gui);
            Pair<Coord2d, Coord2d> fishArea = NContext.findSpec(Specialisation.SpecName.rawfish.toString()).getRCArea();
            
            // Получаем размер рыбы из пайла (как в FillContainersFromPiles)
            Coord fishSize = new Coord(1, 1); // размер по умолчанию
            
            while (hasEmptySlots(containers) && gui.getInventory().getNumberFreeCoord(new Coord(1,3))>0 && gui.getInventory().getNumberFreeCoord(new Coord(2,1))>0) {
                // Проверяем, есть ли рыба в пайлах
                ArrayList<Gob> piles = Finder.findGobs(fishArea, new NAlias("stockpile"));
                if (piles.isEmpty()) {
                    break; // Нет рыбы - выходим
                }
                
                // Освобождаем инвентарь если нужно
                if (gui.getInventory().getFreeSpace() < 3) {
                    // Если есть филе, переносим на сушилки
                    if (!gui.getInventory().getItems(dfillet).isEmpty()) {
                        fillDryingFrames(gui, containers);
                    }
                    // Если места все еще нет, выходим
                    if (gui.getInventory().getFreeSpace() < 3) {
                        break;
                    }
                }
                
                piles.sort(NUtils.d_comp);
                Gob pile = piles.get(0);
                new PathFinder(pile).run(gui);
                new OpenTargetContainer("Stockpile", pile).run(gui);
                
                while(Finder.findGob(pile.id)!=null && gui.getInventory().getNumberFreeCoord(new Coord(1,3))>0 && gui.getInventory().getNumberFreeCoord(new Coord(2,1))>0)
                {
                    new TakeItemsFromPile(pile, gui.getStockpile(), 1).run(gui);
                }
                new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                
                ArrayList<WItem> fishItems = gui.getInventory().getItems(new NAlias(VSpec.getCategoryContent("Fish")));
                for (WItem fishItem : fishItems) {
                    int oldFilletCount = gui.getInventory().getItems(rfillet).size();
                    new SelectFlowerAction("Butcher", fishItem).run(gui);
                    
                    NUtils.addTask(new WaitItems((NInventory) gui.maininv, rfillet, oldFilletCount + 1));
                }
                
                // После обработки всей взятой рыбы вешаем оставшееся филе на сушилки
                if (!gui.getInventory().getItems(rfillet).isEmpty()) {
                    fillDryingFrames(gui, containers);
                }
            }
            
            // В конце переносим все оставшееся филе на сушилки
            if (!gui.getInventory().getItems(rfillet).isEmpty()) {
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
            if (gui.getInventory().getItems(rfillet).isEmpty()) {
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
                    int toTransfer = Math.min(freeSpace, gui.getInventory().getItems(rfillet).size());
                    for (int i = 0; i < toTransfer; i++) {
                        ArrayList<WItem> fillets = gui.getInventory().getItems(rfillet);
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
                }
            }
        }
    }
}

