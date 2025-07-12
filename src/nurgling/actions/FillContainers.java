package nurgling.actions;

import haven.Coord;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FillContainers implements Action
{
    ArrayList<Container> conts;
    String transferedItems;
    Context context;
    ArrayList<Container> currentContainers = new ArrayList<>();
    Coord targetCoord = new Coord(1,1);

    public FillContainers(ArrayList<Container> conts, String transferedItems, Context context) {
        this.conts = conts;
        this.context = context;
        this.transferedItems = transferedItems;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea area = NContext.findIn(transferedItems);
        if (area == null)
            return Results.ERROR("NO area for: " + transferedItems);
        context.addInput(transferedItems, Context.GetInput(transferedItems, area));
        for (Container cont : conts) {
            while(!isReady(cont)) {
                if (gui.getInventory().getItems(transferedItems).isEmpty()) {
                    int target_size = calculateTargetSize();
                    new TakeItems(context,transferedItems,Math.min(target_size,NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord))).run(gui);
                    if (gui.getInventory().getItems(transferedItems).isEmpty())
                        return Results.ERROR("NO ITEMS");
                }
                TransferToContainer ttc = new TransferToContainer(cont, new NAlias(transferedItems));
                ttc.run(gui);
                new CloseTargetContainer(cont).run(gui);
            }

        }
        return Results.SUCCESS();
    }

    private int calculateTargetSize() throws InterruptedException {
        int target_size = 0;
        for (Container tcont : conts) {
            Container.Tetris tetris = tcont.getattr(Container.Tetris.class);
            if(tetris!=null) {
                if (!(Boolean) tetris.getRes().get(Container.Tetris.DONE)) {
                    ArrayList<Coord> coords = (ArrayList<Coord>) tetris.getRes().get(Container.Tetris.TARGET_COORD);
                    if (coords.size() != 1) {
                        NUtils.getGameUI().error("BAD LOGIC. TOO BIG COORDS ARRAY FOR TETRIS");
                        throw new InterruptedException();
                    }
                    Coord target_coord = coords.get(0);
                    target_size += tetris.calcNumberFreeCoord(Container.Tetris.SRC, target_coord);
                }
            }
            else
            {
                Container.Space space = tcont.getattr(Container.Space.class);
                target_size += (Integer) space.getRes().get(Container.Space.FREESPACE);
            }
        }
        return target_size;
    }

    boolean isReady(Container container) {
        Container.Tetris tetris = container.getattr(Container.Tetris.class);
        if(tetris!=null)
        {
            return (Boolean)tetris.getRes().get(Container.Tetris.DONE);
        }
        else
        {
            Container.Space space = container.getattr(Container.Space.class);
            return (Integer)space.getRes().get(Container.Space.FREESPACE) != null && (Integer)space.getRes().get(Container.Space.FREESPACE)==0;
        }
    };
}
