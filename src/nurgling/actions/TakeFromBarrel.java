package nurgling.actions;

import haven.*;
import haven.res.ui.relcnt.RelCont;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.Context;
import nurgling.tools.NAlias;

public class TakeFromBarrel implements Action{

    Gob barrel;
    NAlias items;

    int count = -1;

    public TakeFromBarrel(Gob barrel, NAlias items, int count) {
        this(barrel, items);
        this.count = count;
    }

    public TakeFromBarrel(Gob barrel, NAlias items) {
        this.barrel = barrel;
        this.items = items;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if(barrel==null){
            return Results.ERROR("NULL BARREL");
        }
        new PathFinder( barrel ).run (gui);
        if(!isFound())
        {
            return Results.ERROR("NO SEEDS");
        }
        if(count==-1) {
            NUtils.takeAllGob(barrel);
            NUtils.getUI().core.addTask(new WaitMoreItems(NUtils.getGameUI().getInventory(), items, 3));
        }
        else
        {
            this.count = Math.min(NUtils.getGameUI().getInventory().getFreeSpace(),count);
            new OpenTargetContainer("Barrel", barrel).run(gui);
            Window bwnd = NUtils.getGameUI().getWindow("Barrel");
            for(Widget child : bwnd.children())
            {
                if(child instanceof RelCont)
                {
                    for(Widget child2 : child.children()) {
                        if(child2 instanceof Button) {
                            Button b = (Button) child2;
                            if (b.text.text.equals("Take")) {
                                for (int i = 0; i < count; i++) {
                                    b.click();
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
            NUtils.getUI().core.addTask(new WaitMoreItems(NUtils.getGameUI().getInventory(), items, count));
        }

        if(!gui.hand.isEmpty()) {
            NUtils.activateItem(barrel, false);
            NUtils.getUI().core.addTask(new WaitFreeHand());
        }
        return Results.SUCCESS();
    }

    private boolean isFound() {
        for (Gob.Overlay ol : barrel.ols) {
            if(ol.spr instanceof StaticSprite) {
                return true;
            }
        }
        return false;
    }

}
