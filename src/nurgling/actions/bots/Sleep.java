package nurgling.actions.bots;

import haven.Gob;
import nurgling.NCharlist;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.WaitCharlist;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class Sleep implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation bed = new NArea.Specialisation(Specialisation.SpecName.bed.toString());
        NArea bedArea = NContext.findSpec(bed);

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(bed);

        ArrayList<Gob> beds = Finder.findGobs(bedArea, new NAlias("bed", "Bed"));

        Gob targetBed = null;

        for(Gob bedGob : beds) {
            // 512 - sturdy bed with a person sleeping, 1 - thatched bed with a person sleeping.
            if(bedGob.ngob.getModelAttribute() != 512 && bedGob.ngob.getModelAttribute() != 1) {
                targetBed = bedGob;
            }
        }

        if(targetBed == null) {
            return Results.FAIL();
        }

        new PathFinder(targetBed).run(gui);

        new SelectFlowerAction("Sleep", targetBed).run(gui);

        NUtils.addTask(new WaitCharlist());

        return Results.SUCCESS();
    }
}
