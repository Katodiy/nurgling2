package nurgling.actions;

import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.AnimalInRoster;
import nurgling.tasks.AnimalRangLoad;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Iterator;

public class MemorizeAnimalsAction implements Action {
    private final NAlias animal;
    private final String type;
    private final Class<? extends Entry> cattleRoster;

    public MemorizeAnimalsAction(NAlias animal, String type, Class<? extends Entry> cattleRoster) {
        this.animal = animal;
        this.type = type;
        this.cattleRoster = cattleRoster;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea current = NContext.findSpec(this.type);
        if (current == null)
            return Results.ERROR("No animal area set. (Sheeps, Cows, Pigs, Goats in area specialization)");

        RosterWindow w = NUtils.getRosterWindow(cattleRoster);
        NUtils.addTask(new AnimalRangLoad(current, animal, cattleRoster, w));

        ArrayList<Gob> gobs = Finder.findGobs(current, animal);
        if (gobs.isEmpty()) {
            return Results.ERROR("Area (" + animal.getDefault() + ") has no animals. Check that it covers animals pen.");
        }

        boolean needsMoreMemorizing;
        do {
            needsMoreMemorizing = memorize(gobs, gui, w, cattleRoster);
        } while (needsMoreMemorizing);

        return Results.SUCCESS();
    }

    public static boolean memorize(ArrayList<Gob> gobs, NGameUI gui, RosterWindow w,
                                   Class<? extends Entry> cattleRoster) throws InterruptedException {
        gobs.sort(NUtils.d_comp);
        for (Iterator<Gob> it = gobs.iterator(); it.hasNext(); ) {
            Gob gob = it.next();
            if (gob.getattr(CattleId.class) == null && gob.pose() != null && !NParser.checkName(gob.pose(), "knock")) {
                new DynamicPf(gob).run(gui);
                new SelectFlowerAction("Memorize", gob).run(gui);
                AnimalInRoster ain;
                NUtils.getUI().core.addTask(ain = new AnimalInRoster(gob, cattleRoster, w));
                if (ain.getResult()) {
                    it.remove();
                }
                return true;
            }
        }
        return false;
    }
}