package nurgling.tasks;

import haven.Gob;
import haven.res.gfx.hud.rosters.Rangable;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class AnimalRangLoad extends NTask {
    private final NArea area;
    private final NAlias animal;
    private final Class<? extends Entry> animalClass;

    public AnimalRangLoad(NArea area, NAlias animal, Class<? extends Entry> animalClass) {
        this.area = area;
        this.animal = animal;
        this.animalClass = animalClass;
    }

    @Override
    public boolean check() {
        Gob gob;
        try {
            gob = Finder.findGob(area, animal);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (gob == null) {
            return false;
        }

        try {
            RosterWindow rosterWindow = NUtils.getRosterWindow(animalClass);
            rosterWindow.roster(animalClass).setFilterAreaId(area);
            NUtils.addTask(new WaitWindow("Cattle Roster"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Rangable animalEntity = (Rangable) NUtils.getAnimalEntity(gob, animalClass);

        return animalEntity.rang() > 0;
    }
}
