package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Context;
import nurgling.widgets.FoodContainer;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;

import static haven.Coord.of;

public class Eater implements Action {

    boolean oz = false;

    public Eater(boolean oz) {
        this.oz = oz;
    }

    public Eater() {
        this.oz = false;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<String> items = FoodContainer.getFoodNames();

        Pair<Coord2d,Coord2d> area = null;
        NArea nArea = NContext.findSpec(Specialisation.SpecName.eat.toString());
        if(nArea==null)
        {
            nArea = NContext.findSpecGlobal(Specialisation.SpecName.eat.toString());
        }
        else
        {
            area = nArea.getRCArea();
        }
        if(area!=null) {
            Context cnt = new Context();
            new FindAndEatItems(cnt, items, 8000, area).run(gui);
            return NUtils.getEnergy()*10000>8000?Results.SUCCESS():Results.FAIL();
        }
        else
            return Results.FAIL();
    }
}
