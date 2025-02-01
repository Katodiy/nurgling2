package nurgling.actions.bots;

import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class BarleyFarmerBarrels implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Barley");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Barley");
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCropNoCistern(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/barley"),new NAlias("barley"),3).run(gui);
            new CollectItemsToTrough( NArea.findSpec(field), NArea.findSpec(seed), new NAlias("straw", "Straw") ).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/barley"),new NAlias("Barley")).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }
}
