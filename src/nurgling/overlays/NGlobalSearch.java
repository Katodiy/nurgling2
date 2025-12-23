package nurgling.overlays;

import haven.*;
import haven.render.*;
import monitoring.NGlobalSearchItems;

import java.awt.*;


public class NGlobalSearch extends GAttrib implements Gob.SetupMod
{
    public NGlobalSearch(Gob g) {
        super(g);
    }

    private static final Color COLOR = new Color(64, 255, 64, 255);
    // Use a cached MixColor to avoid creating new objects every frame
    private static final MixColor HIGHLIGHT_COLOR = new MixColor(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 255);

    public Pipe.Op gobstate() {
        if (gob.ngob.hash != null) {
            synchronized (NGlobalSearchItems.containerHashes) {
                if (NGlobalSearchItems.containerHashes.contains(gob.ngob.hash)) {
                    return HIGHLIGHT_COLOR;
                }
            }
        }
        return null;
    }
}
