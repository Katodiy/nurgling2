package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.UI;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NISBox;
import nurgling.NUtils;
import nurgling.tasks.WaitItemFromPile;
import nurgling.tools.Container;

import java.util.ArrayList;

public class TakeItemsByTetris implements Action
{
    NISBox pile;
    ArrayList<Container> conts;

    int target_size = Integer.MAX_VALUE;


    public TakeItemsByTetris(Gob gob, NISBox pile, ArrayList<Container> conts)
    {
        this.pile = pile;
        this.target_size = target_size;
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
            gui.getStockpile().transfer(1);
            WaitItemFromPile wifp = new WaitItemFromPile(gui.getInventory().getItems());
            NUtils.getUI().core.addTask(wifp);
            for(WItem item: wifp.getResult()) {
                for (Container container: conts) {
                    Container.Tetris tetris = container.getattr(Container.Tetris.class);
                    if(tetris.tryPlace(item.item.spr.sz().div(UI.scale(32))))
                        break;
                }
            }
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
