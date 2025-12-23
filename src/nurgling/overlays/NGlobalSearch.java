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
    private long lastVersion = -1;
    private boolean lastHighlighted = false;

    public Pipe.Op gobstate() {
        // Check if containerHashes was updated
        long currentVersion = NGlobalSearchItems.updateVersion;
        
        boolean isHighlighted = false;
        if (gob.ngob.hash != null) {
            synchronized (NGlobalSearchItems.containerHashes) {
                isHighlighted = NGlobalSearchItems.containerHashes.contains(gob.ngob.hash);
            }
        }
        
        // If version changed and highlight state changed, force gob state update
        if (currentVersion != lastVersion && isHighlighted != lastHighlighted) {
            lastVersion = currentVersion;
            lastHighlighted = isHighlighted;
            // Schedule state update for next frame (can't call updstate during gobstate)
            gob.defer(() -> gob.updstate());
        } else {
            lastVersion = currentVersion;
            lastHighlighted = isHighlighted;
        }
        
        if (isHighlighted) {
            return new MixColor(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 255);
        }
        return null;
    }
}
