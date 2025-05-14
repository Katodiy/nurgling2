package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class WaitForDoorGob extends NTask {

    @Override
    public boolean check() {
        Gob door;
        try {
            door = Finder.findGob(NUtils.player().rc, new NAlias(
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
            ), null, 100);
        } catch (Exception e) {
            return false;
        }

        return door != null;
    }
}
