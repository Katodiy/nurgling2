package nurgling.overlays;


import haven.*;
import haven.render.*;
import monitoring.NGlobalSearchItems;
import nurgling.NConfig;
import nurgling.NGob;
import nurgling.NUtils;
import nurgling.tools.VSpec;

import java.awt.*;


public class NGlobalSearch extends GAttrib implements Gob.SetupMod
{
    public NGlobalSearch(Gob g) {
        super(g);
        start = NUtils.getTickId();
    }

    private static final Color COLOR = new Color(64, 255, 64, 255);
    private static final long cycle = 50;
    private long start = 0;

    public Pipe.Op gobstate() {
        if(NGlobalSearchItems.containerHashes.contains(gob.ngob.hash)) {
            return new MixColor(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 255);
        }
        return null;
    }
}
