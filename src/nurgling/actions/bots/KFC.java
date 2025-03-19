package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.*;

public class KFC implements Action {

    // Подкласс для информации о курятнике
    private class CoopInfo {
        Container container; // Контейнер курятника
        double roosterQuality; // Качество петуха
        ArrayList<Float> henQualities = new ArrayList<>(); // Список качеств кур

        public CoopInfo(Container container, double roosterQuality) {
            this.container = container;
            this.roosterQuality = roosterQuality;
        }
    }

    // Подкласс для информации об инкубаторе
    private class IncubatorInfo {
        Container container; // Контейнер инкубатора
        double chickenQuality; // Качество курицы

        public IncubatorInfo(Container container, double chickenQuality) {
            this.container = container;
            this.chickenQuality = chickenQuality;
        }
    }

    // Подкласс для данных о яйцах
    private class EggData {
        double quality; // Качество яйца
        Container container; // Контейнер, в котором находится яйцо

        public EggData(double quality, Container container) {
            this.quality = quality;
            this.container = container;
        }
    }

    // Компаратор для сортировки инкубаторов по качеству
    Comparator<IncubatorInfo> incubatorComparator = new Comparator<IncubatorInfo>() {
        @Override
        public int compare(IncubatorInfo o1, IncubatorInfo o2) {
            return Double.compare(o1.chickenQuality, o2.chickenQuality);
        }
    };

    // Компаратор для сортировки курятников
    Comparator<CoopInfo> coopComparator = new Comparator<CoopInfo>() {
        @Override
        public int compare(CoopInfo o1, CoopInfo o2) {
            int res = Double.compare(o1.roosterQuality, o2.roosterQuality);
            if (res == 0) {
                if (!o1.henQualities.isEmpty() && !o2.henQualities.isEmpty()) {
                    double avgQuality1 = o1.henQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                    double avgQuality2 = o2.henQualities.stream().mapToDouble(Float::doubleValue).average().orElse(0);
                    res = Double.compare(avgQuality1, avgQuality2);
                }
            }
            return res;
        }
    };

    // Компаратор для сортировки яиц по качеству
    Comparator<EggData> eggComparator = new Comparator<EggData>() {
        @Override
        public int compare(EggData o1, EggData o2) {
            return Double.compare(o1.quality, o2.quality);
        }
    };

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();

        for (Gob ttube : Finder.findGobs(NArea.findSpec(Specialisation.SpecName.chicken.toString()),
                new NAlias("gfx/terobjs/chickencoop"))) {
            Container cand = new Container();
            cand.gob = ttube;
            cand.cap = "Chicken Coop";

            cand.initattr(Container.Space.class);

            containers.add(cand);
        }

        ArrayList<Container> ccontainers = new ArrayList<>();

        for (Gob ttube : Finder.findGobs(NArea.findSpec(Specialisation.SpecName.incubator.toString()),
                new NAlias("gfx/terobjs/chickencoop"))) {
            Container cand = new Container();
            cand.gob = ttube;
            cand.cap = "Chicken Coop";

            cand.initattr(Container.Space.class);

            ccontainers.add(cand);
        }

        // Заполняем курятники и инкубаторы жидкостями
        new FillFluid(containers, NArea.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(), new NAlias("swill"), 2).run(gui);
        new FillFluid(ccontainers, NArea.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(), new NAlias("swill"), 2).run(gui);
        new FillFluid(containers, NArea.findSpec(Specialisation.SpecName.water.toString()).getRCArea(), new NAlias("water"), 1).run(gui);
        new FillFluid(ccontainers, NArea.findSpec(Specialisation.SpecName.water.toString()).getRCArea(), new NAlias("water"), 1).run(gui);

        // Считываем содержимое курятников и сортируем их
        ArrayList<CoopInfo> coopInfos = new ArrayList<>();
        ArrayList<IncubatorInfo> qcocks = new ArrayList<>();
        ArrayList<IncubatorInfo> qhens = new ArrayList<>();
        for (Container container : containers) {
            new PathFinder( container.gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop",container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }



            double roosterQuality;
            if(gui.getInventory("Chicken Coop").getItem(new NAlias("Cock"))!=null) {
                // Получаем информацию о петухе
                NGItem roost = (NGItem) gui.getInventory("Chicken Coop").getItem(new NAlias("Cock")).item;
                roosterQuality = roost.quality;
            }
            else
            {
                roosterQuality = -1;
            }


            // Создаем объект CoopInfo для текущего курятника
            CoopInfo coopInfo = new CoopInfo(container, roosterQuality);

            // Получаем информацию о курах
            ArrayList<WItem> hens = gui.getInventory("Chicken Coop").getItems(new NAlias("Hen"));
            for (WItem hen : hens) {
                coopInfo.henQualities.add(((NGItem)hen.item).quality);
            }
            coopInfo.henQualities.sort(Float::compareTo);

            // Добавляем курятник в список
            coopInfos.add(coopInfo);

            new CloseTargetContainer(container).run(gui);
        }

        // Сортируем курятники по качеству петухов и среднему качеству кур
        coopInfos.sort(coopComparator.reversed());

        for (Container container : ccontainers) {
            new PathFinder(container.gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            ArrayList<WItem> roosters = gui.getInventory("Chicken Coop").getItems(new NAlias("Cock"));
            for (WItem rooster : roosters) {
                qcocks.add(new IncubatorInfo(container, ((NGItem) rooster.item).quality));
            }

            ArrayList<WItem> hens = gui.getInventory("Chicken Coop").getItems(new NAlias("Hen"));
            for (WItem hen : hens) {
                qhens.add(new IncubatorInfo(container, ((NGItem) hen.item).quality));
            }

            new CloseTargetContainer(container).run(gui);
        }

        Results roosterResult = processRoosters(gui, coopInfos, qcocks);
        if (!roosterResult.IsSuccess()) {
            return roosterResult;
        }


        Results henResult = processHens(gui, coopInfos, qhens);
        if (!henResult.IsSuccess()) {
            return henResult;
        }

        Context chickCnt = new Context();
        ArrayList<Context.Output> outputs = new ArrayList<>();
        for (Container cc :ccontainers) {
            Context.OutputContainer container = new Context.OutputContainer(cc.gob, NArea.findSpec(Specialisation.SpecName.incubator.toString()).getRCArea(), 1);
            container.cap = "Chicken Coop";
            container.initattr(Container.Space.class);
            outputs.add(container);
        }

        chickCnt.addOutput("Chick",outputs);
        HashSet<String> chicks = new HashSet<>();
        chicks.add("Chick");
        new TransferTargetItemsFromContainers(chickCnt,containers,chicks, new NAlias(new ArrayList<>(), new ArrayList<>(List.of("Egg", "Feather", "Meat", "Bone")))).run(gui);



        // Выясняем пороговое качество для яиц
        new PathFinder(coopInfos.get(0).container.gob).run(gui);
        if (!(new OpenTargetContainer("Chicken Coop", coopInfos.get(0).container.gob).run(gui).IsSuccess())) {
            return Results.FAIL();
        }

        // Получаем информацию о курах
        ArrayList<WItem> topHens = gui.getInventory("Chicken Coop").getItems(new NAlias("Hen"));
        ArrayList<Float> qtop = new ArrayList<>();
        for (WItem top : topHens) {
            qtop.add(((NGItem) top.item).quality);
        }
        qtop.sort(Float::compareTo);

        // Выводим пороговое качество
        gui.msg(String.valueOf(qtop.get(0)));

        double chicken_th = qtop.get(0);


        Context eggCnt = new Context();
        HashSet<String> eggs = new HashSet<>();
        eggs.add("Chicken Egg");
        for(Container container: containers)
        {
            PathFinder pf = new PathFinder(container.gob);
            pf.isHardMode = true;
            pf.run(gui);
            new OpenTargetContainer(container).run(gui);


            while (!new TakeItemsFromContainer(container, eggs, null, chicken_th).run(gui).isSuccess)
            {
                new TransferItems(eggCnt, eggs).run(gui);
                pf = new PathFinder(container.gob);
                pf.isHardMode = true;
                pf.run(gui);
                new OpenTargetContainer(container).run(gui);
            }
            new CloseTargetContainer(container).run(gui);
        }
        new TransferItems(eggCnt, eggs).run(gui);
        return Results.SUCCESS();
    }


    private Results processRoosters(NGameUI gui, ArrayList<CoopInfo> coopInfos, ArrayList<IncubatorInfo> qcocks) throws InterruptedException {

        // Сортируем петушков по качеству (от лучшего к худшему)
        qcocks.sort(incubatorComparator.reversed());

        for (IncubatorInfo roosterInfo : qcocks) {
            // Открываем курятник с петушком
            new PathFinder(roosterInfo.container.gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", roosterInfo.container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Получаем петушка из инвентаря
            WItem rooster = (WItem) gui.getInventory("Chicken Coop").getItem(new NAlias("Cock"));
            if (rooster == null) {
                return Results.ERROR("NO_ROOSTER");
            }
            double roosterQuality = ((NGItem)rooster.item).quality;

            Coord pos = rooster.c.div(Inventory.sqsz);
            rooster.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory("Chicken Coop").isSlotFree(finalPos1);
                }
            });

            // Ищем курятник с худшим петухом и заменяем его
            for (CoopInfo coopInfo : coopInfos) {
                if (coopInfo.roosterQuality < roosterQuality && coopInfo.roosterQuality!=-1) {


                    rooster = (WItem) gui.getInventory().getItem(new NAlias("Cock"));

                    // Открываем курятник для замены
                    new PathFinder(coopInfo.container.gob).run(gui);
                    if (!(new OpenTargetContainer("Chicken Coop", coopInfo.container.gob).run(gui).IsSuccess())) {
                        return Results.FAIL();
                    }

                    // Получаем текущего петушка в курятнике
                    WItem oldRooster = gui.getInventory("Chicken Coop").getItem(new NAlias("Cock"));
                    if (oldRooster == null) {
                        return Results.ERROR("NO_ROOSTER_IN_COOP");
                    }

                    // Заменяем петушка
                    pos = oldRooster.c.div(Inventory.sqsz);
                    oldRooster.item.wdgmsg("transfer", Coord.z);
                    Coord finalPos = pos;
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return gui.getInventory("Chicken Coop").isSlotFree(finalPos);
                        }
                    });

                    NUtils.takeItemToHand(rooster);
                    gui.getInventory("Chicken Coop").dropOn(pos,"Cock");




                    // Обновляем качество петушка в курятнике
                    coopInfo.roosterQuality = roosterQuality;
                    roosterQuality = ((NGItem)oldRooster.item).quality;
                    // Обновляем качество для следующей замены
                    new CloseTargetContainer(coopInfo.container).run(gui);
                }
            }

            rooster = (WItem) gui.getInventory().getItem(new NAlias("Cock"));
            new SelectFlowerAction( "Wring neck", rooster).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Dead Cock"), 1));

            rooster = (WItem) gui.getInventory().getItem(new NAlias("Dead Cock"));
            new SelectFlowerAction( "Pluck", rooster).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Plucked Chicken"), 1));

            rooster = (WItem) gui.getInventory().getItem(new NAlias("Plucked Chicken"));
            new SelectFlowerAction( "Clean", rooster).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv,new NAlias("Cleaned Chicken"), 1));

            rooster = (WItem) gui.getInventory().getItem(new NAlias("Cleaned Chicken"));
            new SelectFlowerAction( "Butcher", rooster).run(gui);
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    try {
                        return gui.getInventory().getItems(new NAlias("Cleaned Chicken")).isEmpty();
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            });

            Context context = new Context();
            new FreeInventory(context).run(gui);
        }
        Context context = new Context();
        new FreeInventory(context).run(gui);
        return Results.SUCCESS();
    }


    private Results processHens(NGameUI gui, ArrayList<CoopInfo> coopInfos, ArrayList<IncubatorInfo> qhens) throws InterruptedException {
        // Сортируем кур по качеству (от лучшего к худшему)
        qhens.sort(incubatorComparator.reversed());

        for (IncubatorInfo henInfo : qhens) {
            // Открываем курятник с курицей
            new PathFinder(henInfo.container.gob).run(gui);
            if (!(new OpenTargetContainer("Chicken Coop", henInfo.container.gob).run(gui).IsSuccess())) {
                return Results.FAIL();
            }

            // Получаем курицу из инвентаря
            WItem hen = (WItem) gui.getInventory("Chicken Coop").getItem(new NAlias("Hen"));
            if (hen == null) {
                return Results.ERROR("NO_HEN");
            }
            float henQuality = ((NGItem) hen.item).quality;

            Coord pos = hen.c.div(Inventory.sqsz);
            hen.item.wdgmsg("transfer", Coord.z);
            Coord finalPos1 = pos;
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return gui.getInventory("Chicken Coop").isSlotFree(finalPos1);
                }
            });

            // Ищем курятник с худшей курицей и заменяем её
            for (CoopInfo coopInfo : coopInfos) {
                for (int i = 0; i < coopInfo.henQualities.size(); i++) {
                    if (coopInfo.henQualities.get(i) < henQuality) {
                        // Открываем курятник для замены
                        new PathFinder(coopInfo.container.gob).run(gui);
                        if (!(new OpenTargetContainer("Chicken Coop", coopInfo.container.gob).run(gui).IsSuccess())) {
                            return Results.FAIL();
                        }

                        hen = (WItem) gui.getInventory().getItem(new NAlias("Hen"));

                        // Получаем текущую курицу в курятнике
                        WItem oldHen = gui.getInventory("Chicken Coop").getItem(new NAlias("Hen"), coopInfo.henQualities.get(i));
                        if (oldHen == null) {
                            return Results.ERROR("NO_HEN_IN_COOP");
                        }

                        // Заменяем курицу
                        pos = oldHen.c.div(Inventory.sqsz);
                        oldHen.item.wdgmsg("transfer", Coord.z);
                        Coord finalPos = pos;
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return gui.getInventory("Chicken Coop").isSlotFree(finalPos);
                            }
                        });

                        NUtils.takeItemToHand(hen);
                        gui.getInventory("Chicken Coop").dropOn(pos, "Hen");

                        // Обновляем качество курицы в курятнике
                        coopInfo.henQualities.set(i, henQuality);
                        henQuality = ((NGItem) oldHen.item).quality; // Обновляем качество для следующей замены
                        new CloseTargetContainer(coopInfo.container).run(gui);
                        break;
                    }
                }
            }

            // Убиваем курицу и обрабатываем её
            hen = (WItem) gui.getInventory().getItem(new NAlias("Hen"));
            new SelectFlowerAction("Wring neck", hen).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Dead Hen"), 1));

            hen = (WItem) gui.getInventory().getItem(new NAlias("Dead Hen"));
            new SelectFlowerAction("Pluck", hen).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Plucked Chicken"), 1));

            hen = (WItem) gui.getInventory().getItem(new NAlias("Plucked Chicken"));
            new SelectFlowerAction("Clean", hen).run(gui);
            NUtils.addTask(new WaitItems((NInventory) gui.maininv, new NAlias("Cleaned Chicken"), 1));

            hen = (WItem) gui.getInventory().getItem(new NAlias("Cleaned Chicken"));
            new SelectFlowerAction("Butcher", hen).run(gui);
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    try {
                        return gui.getInventory().getItems(new NAlias("Cleaned Chicken")).isEmpty();
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            });

            Context context = new Context();
            new FreeInventory(context).run(gui);
        }
        Context context = new Context();
        new FreeInventory(context).run(gui);
        return Results.SUCCESS();
    }


}
