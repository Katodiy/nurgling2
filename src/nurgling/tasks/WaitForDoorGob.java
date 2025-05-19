package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class WaitForDoorGob extends NTask {

    @Override
    public boolean check() {
        ArrayList<Gob> door;

        String[] names = {
                "gfx/terobjs/arch/stonestead",
                "gfx/terobjs/arch/stonemansion",
                "gfx/terobjs/arch/greathall",
                "gfx/terobjs/arch/primitivetent",
                "gfx/terobjs/arch/windmill",
                "gfx/terobjs/arch/stonetower",
                "gfx/terobjs/arch/logcabin",
                "gfx/terobjs/arch/timberhouse",
                "gfx/terobjs/arch/minehole",
                "gfx/terobjs/arch/ladder",
                "gfx/terobjs/arch/upstairs",
                "gfx/terobjs/arch/downstairs",
                "gfx/terobjs/arch/cellardoor",
                "gfx/terobjs/arch/cellarstairs"
        };

        for(String name : names) {
            door = Finder.findGobs(new NAlias(name));
            if(door.size() > 0 && door.get(0).ngob.hash != null) {
                return true;
            }
        }

        return false;
    }
}
