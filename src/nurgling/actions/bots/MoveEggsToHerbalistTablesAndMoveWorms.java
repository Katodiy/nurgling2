package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class MoveEggsToHerbalistTablesAndMoveWorms implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        NArea.Specialisation herbalistTableArea = new NArea.Specialisation(Specialisation.SpecName.htable.toString(), "Silkworm Eggs");

        NAlias eggAlias = new NAlias("Silkworm Egg");
        NAlias wormAlias = new NAlias("Silkworm");
        NAlias mulberryLeavesAlias = new NAlias("Mulberry Leaf");
        NArea eggStorage = NContext.findIn(eggAlias);
        NArea feedingArea = NContext.findOut(wormAlias, 1);
        NArea herbalistTablesArea = NContext.findSpec(herbalistTableArea);
        NArea mulberryLeaves = NContext.findIn(mulberryLeavesAlias);

        ArrayList<Container> herbalistTables = getContainers(herbalistTablesArea, "gfx/terobjs/htable");
        ArrayList<Container> eggCupboards = getContainers(eggStorage, new ArrayList<>(NContext.contcaps.keySet()));
        ArrayList<Container> wormCupboards = getContainers(feedingArea, new ArrayList<>(NContext.contcaps.keySet()));
        ArrayList<Container> mulberrySources = getContainers(mulberryLeaves, new ArrayList<>(NContext.contcaps.keySet()));

        fillFeedingCupboardsWithMulberryLeaves(wormCupboards, mulberrySources, mulberryLeavesAlias, gui, context);
        moveWormsFromHerbalistTables(herbalistTables, wormAlias, gui, context);
        ArrayList<Container> emptyTables = getEmptyHerbalistTables(herbalistTables, eggAlias, wormAlias, gui);
        moveEggsToEmptyTables(emptyTables, context, gui);
        new FreeInventory2(context).run(gui);

        return Results.SUCCESS();
    }

    private ArrayList<Container> getContainers(NArea area, Object aliasOrKeys) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();
        NAlias alias;
        if (aliasOrKeys instanceof String) {
            alias = new NAlias((String) aliasOrKeys);
            for (Gob gob : Finder.findGobs(area.getRCArea(), alias)) {
                Container c = new Container(gob, NContext.contcaps.get(gob.ngob.name));
                c.initattr(Container.Space.class);
                containers.add(c);
            }
        } else if (aliasOrKeys instanceof ArrayList) {
            @SuppressWarnings("unchecked")
            ArrayList<String> keys = (ArrayList<String>) aliasOrKeys;
            for (Gob gob : Finder.findGobs(area.getRCArea(), new NAlias(keys))) {
                Container c = new Container(gob, NContext.contcaps.get(gob.ngob.name));
                c.initattr(Container.Space.class);
                containers.add(c);
            }
        }
        return containers;
    }

    private void fillFeedingCupboardsWithMulberryLeaves(ArrayList<Container> wormCupboards, ArrayList<Container> mulberrySources, NAlias mulberryLeavesAlias, NGameUI gui, NContext context) throws InterruptedException {
        for (Container cupboard : wormCupboards) {
            cupboard.initattr(Container.Space.class);
            int currentLeaves = 0;
            new PathFinder(Finder.findGob(cupboard.gobid)).run(gui);
            new OpenTargetContainer(cupboard).run(gui);
            currentLeaves = gui.getInventory(cupboard.cap).getItems(mulberryLeavesAlias).size();
            new CloseTargetContainer(cupboard).run(gui);

            int toFill = 32 - currentLeaves;
            if (toFill <= 0)
                continue;

            for (Container source : mulberrySources) {
                if (toFill <= 0)
                    break;
                new PathFinder(Finder.findGob(source.gobid)).run(gui);
                new OpenTargetContainer(source).run(gui);
                int available = gui.getInventory(source.cap).getItems(mulberryLeavesAlias).size();
                if (available == 0) {
                    new CloseTargetContainer(source).run(gui);
                    continue;
                }
                int fetch = Math.min(available, Math.min(toFill, gui.getInventory().getFreeSpace()));
                if (fetch <= 0) {
                    new CloseTargetContainer(source).run(gui);
                    continue;
                }
                new TakeAvailableItemsFromContainer(source, mulberryLeavesAlias, fetch).run(gui);
                new CloseTargetContainer(source).run(gui);

                new PathFinder(Finder.findGob(cupboard.gobid)).run(gui);
                new OpenTargetContainer(cupboard).run(gui);
                new TransferToContainer(cupboard, mulberryLeavesAlias).run(gui);
                new CloseTargetContainer(cupboard).run(gui);

                toFill -= fetch;
                if (gui.getInventory().getFreeSpace() == 0)
                    new FreeInventory2(context).run(gui);
            }
        }
    }

    private void moveWormsFromHerbalistTables(ArrayList<Container> herbalistTables, NAlias wormAlias, NGameUI gui, NContext context) throws InterruptedException {
        for (Container table : herbalistTables) {
            while (true) {
                PathFinder pf = new PathFinder(Finder.findGob(table.gobid));
                pf.isHardMode = true;
                pf.run(gui);
                new OpenTargetContainer(table).run(gui);

                ArrayList<WItem> worms = gui.getInventory(table.cap).getItems(wormAlias);
                if (worms.isEmpty()) {
                    new CloseTargetContainer(table).run(gui);
                    break;
                }

                int fetchCount = Math.min(worms.size(), gui.getInventory().getFreeSpace());
                if (fetchCount == 0) {
                    new CloseTargetContainer(table).run(gui);
                    new FreeInventory2(context).run(gui);
                    continue;
                }
                new TakeAvailableItemsFromContainer(table, wormAlias, fetchCount).run(gui);

                new CloseTargetContainer(table).run(gui);

                new FreeInventory2(context).run(gui);
            }
        }
    }

    private ArrayList<Container> getEmptyHerbalistTables(ArrayList<Container> herbalistTables, NAlias eggAlias, NAlias wormAlias, NGameUI gui) throws InterruptedException {
        ArrayList<Container> emptyTables = new ArrayList<>();
        for (Container table : herbalistTables) {
            PathFinder pf = new PathFinder(Finder.findGob(table.gobid));
            pf.isHardMode = true;
            pf.run(gui);
            new OpenTargetContainer(table).run(gui);
            ArrayList<WItem> eggs = gui.getInventory(table.cap).getItems(eggAlias);
            ArrayList<WItem> worms = gui.getInventory(table.cap).getItems(wormAlias);
            boolean isEmpty = eggs.isEmpty() && worms.isEmpty();
            if (isEmpty)
                emptyTables.add(table);
            new CloseTargetContainer(table).run(gui);
        }
        return emptyTables;
    }

    private void moveEggsToEmptyTables(ArrayList<Container> emptyTables, NContext context, NGameUI gui) throws InterruptedException {
        new FillContainers2(emptyTables, "Silkworm Egg", context).run(gui);
    }
}
