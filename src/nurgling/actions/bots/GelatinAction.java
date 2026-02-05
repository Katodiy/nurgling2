package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItemFromPile;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NMakewindow;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * GelatinAction - бот для крафта желатина из готовых шкур.
 *
 * Использует зону readyHides с готовыми шкурами (не сырыми, не свежими).
 * Ищет зону глобально и использует chunk navigation для навигации.
 * Берет шкуры разного размера (2x2) в инвентарь по одной,
 * подготавливает котёл (fuel, fire, water) через PrepareWorkStation,
 * крафтит желатин используя стандартный Craft в режиме prefilled,
 * выгружает результат и повторяет цикл.
 */
public class GelatinAction implements Action {

    // Алиас для готовых шкур (исключаем Fresh и Raw)
    private final NAlias hidesAlias = new NAlias(
            new ArrayList<>(Arrays.asList("hide", "Hide", "Fur", "fur", "skin", "Skin", "Scale")),
            new ArrayList<>(Arrays.asList("Fresh", "Raw", "water"))
    );

    // Название рецепта желатина
    private static final String GELATIN_RECIPE_NAME = "Gelatin";

    // Минимальный размер шкуры для проверки места (2x2)
    private static final Coord HIDE_SIZE = new Coord(2, 2);

    // Минимальное количество слотов для крафта
    private static final int MIN_SLOTS_FOR_CRAFT = 5;

    // Размер Gelatin (1x1) - нужно оставить место для результата
    private static final Coord GELATIN_SIZE = new Coord(1, 1);

    // Сколько слотов оставить для результата и топлива (4 для Gelatin + 2 для дров)
    private static final int RESERVE_SLOTS_FOR_OUTPUT = 6;

    public GelatinAction() {
    }

    public GelatinAction(Map<String, Object> settings) {
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        gui.msg("Gelatin bot started");

        // Главный цикл крафта
        while (true) {
            // Ищем зону со шкурами глобально
            NArea hidesArea = NContext.findSpec(Specialisation.SpecName.readyHides.toString());
            if (hidesArea == null) {
                hidesArea = NContext.findSpecGlobal(Specialisation.SpecName.readyHides.toString());
            }
            if (hidesArea == null) {
                return Results.ERROR("Required zone 'readyHides' not found");
            }

            // Проверяем энергию
            double currentEnergy = NUtils.getEnergy();
            if (currentEnergy < 0.25) {
                if (!new RestoreResources().run(gui).IsSuccess()) {
                    return Results.ERROR("Energy too low and failed to restore resources");
                }
            }

            // Создаем NContext для работы с зонами
            NContext ncontext = new NContext(gui);

            // Настраиваем workstation на cauldron
            ncontext.workstation = new NContext.Workstation("gfx/terobjs/cauldron", null);

            // 1. Сначала набираем шкуры в инвентарь (оставляем место для дров и результата)
            gui.msg("Collecting hides...");
            Results fillResult = fillInventoryWithHides(gui, hidesArea);
            if (!fillResult.IsSuccess()) {
                gui.msg("No more hides available");
                break;
            }

            // Проверяем что шкуры есть в инвентаре и считаем слоты
            ArrayList<WItem> hidesInInv = gui.getInventory().getItems(hidesAlias);
            if (hidesInInv.isEmpty()) {
                gui.msg("No hides in inventory, stopping");
                break;
            }

            // Считаем слоты (размер шкуры: 2x1=2 слота, 2x2=4 слота и т.д.)
            int totalSlots = countHideSlots(hidesInInv, gui);
            if (totalSlots < MIN_SLOTS_FOR_CRAFT) {
                gui.msg("Not enough hides: " + totalSlots + " slots, need " + MIN_SLOTS_FOR_CRAFT);
                break;
            }

            gui.msg("Collected " + hidesInInv.size() + " hides (" + totalSlots + " slots), going to cauldron");

            // 2. Идём к котлу и подготавливаем его (топливо, вода, огонь)
            // PrepareWorkStation сам найдёт котёл, подойдёт к нему и подготовит
            Results prepareResult = new PrepareWorkStation(ncontext, ncontext.workstation.station).run(gui);
            if (!prepareResult.IsSuccess()) {
                return Results.ERROR("Failed to prepare cauldron workstation");
            }
            gui.msg("Cauldron prepared, starting craft");

            // 3. Открываем меню крафта желатина
            if (!openGelatinCraft(gui)) {
                return Results.ERROR("Failed to open gelatin craft menu");
            }

            // 4. Добавляем output для Gelatin
            ncontext.addOutItem("Gelatin", null, 1);

            // 5. Крафтим в режиме prefilled
            NMakewindow mwnd = gui.craftwnd.makeWidget;
            if (mwnd == null) {
                return Results.ERROR("Craft window not available");
            }

            // Включаем авто-режим
            mwnd.autoMode = true;
            if (mwnd.noTransfer != null) {
                mwnd.noTransfer.visible = true;
            }

            // Ждем загрузки рецепта
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    if (mwnd.inputs == null || mwnd.inputs.isEmpty()) {
                        return false;
                    }
                    for (NMakewindow.Spec spec : mwnd.inputs) {
                        if (spec.name == null) {
                            return false;
                        }
                    }
                    return true;
                }
            });

            // Запускаем крафт в режиме prefilled (ингредиенты уже в инвентаре)
            // Передаём 1 чтобы сделать один цикл крафта (сколько получится из имеющихся шкур)
            // НЕ передаём totalSlots, иначе Craft попытается сделать 36 крафтов в цикле
            Craft craft = new Craft(mwnd, 1, true);
            craft.run(gui);

            // 6. Выгружаем результат (желатин)
            new FreeInventory2(ncontext).run(gui);

            // Также выкладываем оставшиеся шкуры обратно
            ArrayList<WItem> remainingHides = gui.getInventory().getItems(hidesAlias);
            if (!remainingHides.isEmpty()) {
                NUtils.navigateToArea(hidesArea);
                new TransferToPiles(hidesArea.getRCArea(), hidesAlias).run(gui);
            }
        }

        gui.msg("Gelatin bot finished");
        return Results.SUCCESS();
    }

    /**
     * Заполняет инвентарь шкурами из зоны readyHides.
     * Берёт шкуры по одной, проверяя место под 2x2 (как в TakeItemsByTetris).
     */
    private Results fillInventoryWithHides(NGameUI gui, NArea hidesArea) throws InterruptedException {
        // Идем к зоне со шкурами (глобальная навигация)
        NUtils.navigateToArea(hidesArea);

        Pair<Coord2d, Coord2d> area = hidesArea.getRCArea();
        int initialCount = gui.getInventory().getItems(hidesAlias).size();

        // Ищем пайлы со шкурами
        ArrayList<Gob> piles = Finder.findGobs(area, new NAlias("stockpile"));
        if (piles.isEmpty()) {
            return Results.ERROR("No stockpiles found in hides area");
        }

        piles.sort(NUtils.d_comp);

        // Берем шкуры по одной пока есть место под 2x2 И остаётся место для результата
        for (Gob pile : piles) {
            // Проверяем есть ли место под шкуру 2x2 + резерв для Gelatin
            int freeSlots = gui.getInventory().getFreeSpace();
            if (gui.getInventory().getNumberFreeCoord(HIDE_SIZE) <= 0 || freeSlots <= RESERVE_SLOTS_FOR_OUTPUT) {
                break;
            }

            new PathFinder(pile).run(gui);
            new OpenTargetContainer("Stockpile", pile).run(gui);

            if (gui.getStockpile() != null) {
                // Берём по одной шкуре пока есть место (с учётом резерва для результата)
                while (gui.getInventory().getNumberFreeCoord(HIDE_SIZE) > 0 &&
                       gui.getInventory().getFreeSpace() > RESERVE_SLOTS_FOR_OUTPUT &&
                       gui.getStockpile() != null) {
                    ((NUI)gui.ui).enableMonitor(gui.maininv);
                    gui.getStockpile().transfer(1);
                    WaitItemFromPile wifp = new WaitItemFromPile();
                    NUtils.getUI().core.addTask(wifp);
                    ((NUI)gui.ui).disableMonitor();

                    // Если пайл опустел, выходим из внутреннего цикла
                    if (wifp.getResult().isEmpty()) {
                        break;
                    }
                }
            }

            new CloseTargetWindow(gui.getWindow("Stockpile")).run(gui);
        }

        // Проверяем что набрали хоть что-то
        ArrayList<WItem> collectedHides = gui.getInventory().getItems(hidesAlias);
        if (collectedHides.size() <= initialCount) {
            return Results.ERROR("Could not collect any hides");
        }

        // Проверяем что набрали минимум слотов
        int slots = countHideSlots(collectedHides, gui);
        if (slots < MIN_SLOTS_FOR_CRAFT) {
            return Results.ERROR("Not enough hides: " + slots + " slots");
        }

        return Results.SUCCESS();
    }

    /**
     * Открывает меню крафта желатина (аналогично LightFire)
     */
    private boolean openGelatinCraft(NGameUI gui) throws InterruptedException {
        // Проверяем, открыто ли уже окно крафта с желатином
        if (gui.craftwnd != null && gui.craftwnd.makeWidget != null) {
            if (gui.craftwnd.makeWidget.rcpnm != null &&
                gui.craftwnd.makeWidget.rcpnm.equals(GELATIN_RECIPE_NAME)) {
                return true;
            }
        }

        // Ищем pagina для желатина по имени рецепта
        MenuGrid.Pagina gelatinPag = null;
        for (MenuGrid.Pagina pb : gui.menu.paginae) {
            try {
                if (pb.button() != null && pb.button().name().equals(GELATIN_RECIPE_NAME)) {
                    gelatinPag = pb;
                    break;
                }
            } catch (Loading l) {
                // Skip if not loaded
            }
        }

        if (gelatinPag == null) {
            gui.error("Could not find gelatin recipe in menu");
            return false;
        }

        // Открываем рецепт
        gelatinPag.button().use(new MenuGrid.Interaction());

        // Ждем появления окна крафта с правильным рецептом
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.craftwnd != null &&
                       gui.craftwnd.makeWidget != null &&
                       gui.craftwnd.makeWidget.rcpnm != null &&
                       gui.craftwnd.makeWidget.rcpnm.equals(GELATIN_RECIPE_NAME);
            }
        });

        return gui.craftwnd != null && gui.craftwnd.makeWidget != null;
    }

    /**
     * Подсчитывает количество слотов занимаемых шкурами.
     * Размер 2x1 = 2 слота, 2x2 = 4 слота и т.д.
     */
    private int countHideSlots(ArrayList<WItem> hides, NGameUI gui) {
        int totalSlots = 0;
        for (WItem hide : hides) {
            if (hide.item.spr != null) {
                Coord size = hide.item.spr.sz().div(UI.scale(32));
                totalSlots += size.x * size.y;
            }
        }
        return totalSlots;
    }
}
