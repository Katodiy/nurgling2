package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.UI;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FillContainersFromPiles implements Action {
    ArrayList<Container> conts;
    NAlias transferedItems;
    NArea area;
    Coord targetCoord = new Coord(1, 1);
    boolean tetris = false;
    boolean tetris_done = true;


    public FillContainersFromPiles(ArrayList<Container> conts, NArea area, NAlias transferedItems) {
        this.conts = conts;
        this.area = area;
        this.transferedItems = transferedItems;
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Container> containers;

        while (!(containers = allDone(conts)).isEmpty())
        {
            for (Container cont : containers) {
                Container.Space space = cont.getattr(Container.Space.class);
                while ((Integer) space.getRes().get(Container.Space.FREESPACE) != 0) {
                    if (gui.getInventory().getItems(transferedItems).isEmpty()) {
                        int target_size = 0;
                        if (targetCoord.equals(1, 1)) {
                            for (Container tcont : conts) {
                                if (tcont.getattr(Container.Tetris.class) != null) {
                                    tetris = true;
                                    Container.Tetris tspace = tcont.getattr(Container.Tetris.class);
                                    tetris_done = tetris_done && (boolean) tspace.getRes().get(Container.Tetris.DONE);
                                } else {
                                    Container.Space tspace = tcont.getattr(Container.Space.class);
                                    target_size += (Integer) tspace.getRes().get(Container.Space.FREESPACE);
                                }
                            }
                        }


                        while (((tetris && !tetris_done) || target_size != 0) && NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord) != 0) {
                            ArrayList<Gob> piles = Finder.findGobs(area, new NAlias("stockpile"));
                            if (piles.isEmpty()) {
                                if (gui.getInventory().getItems(transferedItems).isEmpty())
                                    return Results.ERROR("no items");
                                else
                                    break;
                            }
                            piles.sort(NUtils.d_comp);

                            Gob pile = piles.get(0);
                            new PathFinder(pile).run(gui);
                            new OpenTargetContainer("Stockpile", pile).run(gui);
                            if (tetris) {
                                TakeItemsByTetris tifp;
                                (tifp = new TakeItemsByTetris(pile, gui.getStockpile(), conts)).run(gui);
                                new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                                tetris_done = tifp.isDone();
                            } else {
                                TakeItemsFromPile tifp;
                                (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(target_size, gui.getInventory().getFreeSpace()))).run(gui);
                                new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                                target_size = target_size - tifp.getResult();
                            }

                        }
                    }
                    if (tetris) {
                        Container.Tetris tetr;
                        if ((tetr = cont.getattr(Container.Tetris.class)) != null) {
                            ArrayList<WItem> witems = gui.getInventory().getItems(transferedItems);
                            boolean hole = false;
                            for (WItem witem : witems)
                                if (tetr.calcNumberFreeCoord(Container.Tetris.SRC, witem.item.spr.sz().div(UI.scale(32))) > 0) {
                                    hole = true;
                                    break;
                                }
                            if (!hole)
                                break;
                        }
                    }
                    new TransferToContainer(new Context(), cont, transferedItems).run(gui);
                }
                new CloseTargetContainer(cont).run(gui);
            }
        }
        return Results.SUCCESS();
    }

    ArrayList<Container> allDone(ArrayList<Container> containers) throws InterruptedException {
        ArrayList<Container> result = new ArrayList<>();
        for (Container cont : containers) {
            Container.Tetris tetris;
            if((tetris = cont.getattr(Container.Tetris.class)) != null) {
               if(!(Boolean)tetris.getRes().get(Container.Tetris.DONE)) {
                   result.add(cont);
               }
            }
            else
            {
                Container.Space space = cont.getattr(Container.Space.class);
                if(space != null) {
                   if((Integer)space.getRes().get(Container.Space.FREESPACE) != 0) {
                       result.add(cont);
                   }
                }
            }
        }
        return result;
    }
}
