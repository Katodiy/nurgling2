package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.UI;
import haven.WItem;
import nurgling.*;
import nurgling.tasks.WaitItemFromPile;
import nurgling.tools.Container;
import nurgling.tools.NAlias;
import nurgling.tools.StockpileUtils;

import java.util.ArrayList;

public class TakeItemsByTetris implements Action
{
    NISBox pile;
    Gob gpile;
    ArrayList<Container> conts;

    public TakeItemsByTetris(Gob gob, NISBox pile, ArrayList<Container> conts)
    {
        gpile = gob;
        this.pile = pile;
        this.conts = conts;
    }
    boolean isDone = false;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        Coord target_coord = new Coord(1,1);
        for (Container container: conts) {
            Container.Tetris tetris = container.getattr(Container.Tetris.class);
            for (Coord coord : (ArrayList<Coord>) tetris.getRes().get(Container.Tetris.TARGET_COORD)) {
                target_coord.x = Math.max(target_coord.x, coord.x);
                target_coord.y = Math.max(target_coord.y, coord.y);
            }
        }

        while (gui.getInventory().getNumberFreeCoord(target_coord) > 0 && gui.getStockpile()!=null)
        {
            ((NUI)gui.ui).enableMonitor(gui.maininv);
            gui.getStockpile().transfer(1);
            WaitItemFromPile wifp = new WaitItemFromPile();
            NUtils.getUI().core.addTask(wifp);

            for(NGItem item: wifp.getResult()) {
                for (Container container: conts) {
                    Container.Tetris tetris = container.getattr(Container.Tetris.class);
                    if(tetris.tryPlace(item.spr.sz().div(UI.scale(32)).swapXY()))
                        break;
                }
            }
            ((NUI)gui.ui).disableMonitor();
            boolean isSuccess = true;
            for (Container container: conts) {
                Container.Tetris tetris = container.getattr(Container.Tetris.class);
                if(!(Boolean) tetris.getRes().get(Container.Tetris.VIRTUAL))
                {
                    isSuccess = false;
                }
            }
            if(isSuccess) {
                isDone = true;
                return Results.SUCCESS();
            }
        }
        if ((gui.getInventory().getNumberFreeCoord(target_coord) == 0))
            isDone = true;
        return Results.SUCCESS();
    }


    public boolean isDone() {
        return isDone;
    }
}
