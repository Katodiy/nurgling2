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

//        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Barley");
//        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Barley");
//        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
//        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
//        ArrayList<NArea.Specialisation> req = new ArrayList<>();
//        req.add(field);
//        req.add(seed);
//        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
//        req.add(trough);
//        opt.add(swill);

        NUtils.getGameUI().msg("Please select area for harvest");
        SelectArea insa;
        (insa = new SelectArea(Resource.loadsimg("baubles/custom"))).run(gui);

        NUtils.getGameUI().msg("Please select area for seed barrels and trough");
        SelectArea seedb;
        (seedb = new SelectArea(Resource.loadsimg("baubles/custom"))).run(gui);


        new HarvestCropBarrel(insa.getRCArea(),seedb.getRCArea(),new NAlias("plants/barley"),new NAlias("barley"),3).run(gui);
        return Results.SUCCESS();
//        if(new Validator(req, opt).run(gui).IsSuccess())
//        {
//            new HarvestCropBarrel(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/barley"),new NAlias("barley"),3).run(gui);
//
//            //new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/barley"),new NAlias("Barley")).run(gui);
//            return Results.SUCCESS();
//        }

//    return Results.FAIL();
    }
}
