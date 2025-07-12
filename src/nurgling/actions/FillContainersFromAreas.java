package nurgling.actions;

import haven.Coord;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FillContainersFromAreas implements Action
{
    ArrayList<Container> conts;
    NAlias transferedItems;
    Context context;
    ArrayList<Container> currentContainers = new ArrayList<>();

    public FillContainersFromAreas(ArrayList<Container> conts, NAlias transferedItems, Context context) {
        this.conts = conts;
        this.context = context;
        this.transferedItems = transferedItems;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        for (Container cont : conts) {
            while(!isReady(cont)) {
                if (gui.getInventory().getItems(transferedItems).isEmpty()) {
                    int target_size = calculateTargetSize();
                    if(context.icontainers.isEmpty()) {
                        if(!context.getInputs(transferedItems.getDefault()).isEmpty())
                        {
                            for(Context.Input input : context.getInputs(transferedItems.getDefault())) {
                                if(input instanceof Context.Barrel)
                                {
                                    new TakeFromBarrel(((Context.Barrel) input).barrel,transferedItems,target_size).run(gui);
                                }
                            }
                        }
                    }
                    else {
                        for (Container container : context.icontainers) {
                            if (!container.getattr(Container.Space.class).isReady() || container.getattr(Container.TargetItems.class).getTargets(transferedItems) > 0) {
                                new PathFinder(Finder.findGob(container.gobid)).run(gui);
                                new OpenTargetContainer(container).run(gui);
                                TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container, transferedItems, target_size);
                                tifc.run(gui);
                                target_size -= tifc.getCount();
                                new CloseTargetContainer(container).run(gui);
                                if (target_size == 0 || !tifc.getResult())
                                    break;
                            }
                        }
                    }
                    if (gui.getInventory().getItems(transferedItems).isEmpty())
                        return Results.ERROR("NO ITEMS");
                }
                TransferToContainer ttc = new TransferToContainer(cont, transferedItems);
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
