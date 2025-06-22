package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NoGob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.List;

public class CatchBugsAround implements Action {

    NAlias bugs = new NAlias("cavemoth", "rat", "grasshopper", "silkmoth", "snail", "waterstrider", "grub", "ladybug");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while(true) {
            if(NUtils.getGameUI() != null) {
                List<Gob> gobs = Finder.findGobs(bugs);

                if (NUtils.player() != null) {
                    gobs = NUtils.sortByNearest(gobs, NUtils.player().rc);
                }
                for (Gob gob : gobs) {
                    NUtils.rclickGob(gob);
                    NUtils.getUI().core.addTask(new NoGob(gob.id));
                }
            }
        }
    }
}
