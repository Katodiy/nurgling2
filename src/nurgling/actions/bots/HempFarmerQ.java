package nurgling.actions.bots;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class HempFarmerQ implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NArea.Specialisation cropQ = new NArea.Specialisation(Specialisation.SpecName.cropQ.toString(), "Hemp");
        NArea.Specialisation seedQ = new NArea.Specialisation(Specialisation.SpecName.seedQ.toString(), "Hemp");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());

        NArea hempFibersArea = NContext.findOut("Hemp Fibres", 1);

        if(hempFibersArea == null) {
            return Results.ERROR("PUT Area for Hemp Fibres required, but not found!");
        }

        Boolean cleanupQContainers = (Boolean) NConfig.get(NConfig.Key.cleanupQContainers);

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(cropQ);
        req.add(seedQ);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(trough);

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            new HarvestCrop(
                    NContext.findSpec(cropQ),
                    NContext.findSpec(seedQ),
                    null,
                    new NAlias("plants/hemp"),
                    true
            ).run(gui);
            if (hempFibersArea != null)
                new CollectItemsToPile(NContext.findSpec(cropQ).getRCArea(), hempFibersArea.getRCArea(), new NAlias("hempfibre", "Hemp Fibres")).run(gui);
            new SeedCrop(NContext.findSpec(cropQ), NContext.findSpec(seedQ), new NAlias("plants/hemp"), new NAlias("Hemp"), false, true).run(gui);

            if (cleanupQContainers && NContext.findSpec(trough) != null) {
                new CleanupSeedQContainer(NContext.findSpec(seedQ), new NAlias("Hemp"), NContext.findSpec(trough)).run(gui);
            }

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
