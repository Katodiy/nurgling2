package nurgling.actions;

import haven.*;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

import static haven.OCache.posres;

public class QuickActionBot implements Action {
    boolean ignorePatter = false;

    public QuickActionBot(boolean ignorePatter) {
        this.ignorePatter = ignorePatter;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        double dist = (Integer) NConfig.get(NConfig.Key.q_range) * MCache.tilesz.x;
        if (!ignorePatter) {

            ArrayList<Gob> gobs = Finder.findGobByPatterns(NUtils.getQAPatterns(), dist);
            gobs.sort(NUtils.d_comp);
            Gob gob = null;
            Following fol;
            for(Gob cand : gobs) {
                if (!(NParser.checkName(cand.ngob.name, "horse") && (fol = NUtils.player().getattr(Following.class)) != null && fol.tgt == cand.id)) {
                    if(cand.getattr(Following.class) == null || !NParser.checkName(NUtils.player().pose(),"banzai") || cand.rc.dist(NUtils.player().rc)>0.01) {
                        gob = cand;
                        break;
                    }
                }
            }
            if (gob != null) {
                gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 1, (int) gob.id, gob.rc.floor(posres),
                        0, -1);
                return Results.SUCCESS();
            }
        }

        if ((Boolean) NConfig.get(NConfig.Key.q_door) || ignorePatter) {
            NUtils.openDoor(gui);
        }

        return Results.SUCCESS();
}
}
