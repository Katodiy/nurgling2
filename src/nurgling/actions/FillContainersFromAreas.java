package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashSet;

public class FillContainersFromAreas implements Action
{
    ArrayList<Container> conts;
    NAlias transferedItems;
    Coord targetCoord = new Coord(1,1);
    Context context;
    ArrayList<Container> currentContainers = new ArrayList<>();
    public FillContainersFromAreas(ArrayList<Container> conts, NAlias transferedItems, Context context) {
        this.conts = conts;
        this.context = context;
        this.transferedItems = transferedItems;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<NArea> areas = NArea.findAllIn(transferedItems);
        for (Container cont : conts) {
            Container.Space space = cont.getattr(Container.Space.class);

            while ((Integer)space.getRes().get(Container.Space.FREESPACE)!=0)
            {
                    if (gui.getInventory().getItems(transferedItems).isEmpty()) {
                        if(context.icontainers.isEmpty())
                            return Results.ERROR("NO INPUT CONTAINERS");
                        int target_size = 0;
                        for (Container tcont : conts) {
                            Container.Space tspace = tcont.getattr(Container.Space.class);
                            if(tcont.getattr(Container.TargetItems.class).getRes().containsKey(Container.TargetItems.MAXNUM))
                            {
                                int need = (Integer)tcont.getattr(Container.TargetItems.class).getRes().get(Container.TargetItems.MAXNUM) - (Integer)tcont.getattr(Container.TargetItems.class).getTargets(transferedItems);
                                target_size += Math.min(need,(Integer) tspace.getRes().get(Container.Space.FREESPACE));
                            }
                            else
                            {
                            target_size += (Integer) tspace.getRes().get(Container.Space.FREESPACE);
                            }
                        }

                        for (Container container : context.icontainers) {
                            if (!container.getattr(Container.Space.class).isReady() || container.getattr(Container.TargetItems.class).getTargets(transferedItems)>0) {
                                new PathFinder(container.gob).run(gui);
                                new OpenTargetContainer(container).run(gui);
                                TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container, transferedItems, target_size);
                                tifc.run(gui);
                                target_size-=tifc.getCount();
                                if(target_size==0 || !tifc.getResult())
                                    break;
                            }
                        }
                    }
                if (gui.getInventory().getItems(transferedItems).isEmpty())
                    return Results.ERROR("NO ITEMS");
                TransferToContainer ttc = new TransferToContainer(new Context(), cont, transferedItems);
                ttc.run(gui);
                if(cont.getattr(Container.TargetItems.class).getRes().containsKey(Container.TargetItems.MAXNUM))
                {

                }
            }
        }
        return Results.SUCCESS();
    }
}
