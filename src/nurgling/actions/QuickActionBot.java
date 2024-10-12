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
            Gob arch = Finder.findGob(NUtils.player().rc, new NAlias("gfx/terobjs/arch/stonestead", "gfx/terobjs/arch/stonemansion", "gfx/terobjs/arch/greathall", "gfx/terobjs/arch/primitivetent", "gfx/terobjs/arch/windmill", "gfx/terobjs/arch/stonetower", "gfx/terobjs/arch/logcabin", "gfx/terobjs/arch/timberhouse"), null, 100);
            if (arch != null) {
                if (NParser.checkName(arch.ngob.name, "gfx/terobjs/arch/greathall")) {
                    Coord2d A = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.begin.y).rot(arch.a).add(arch.rc);
                    Coord2d B = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.end.y).rot(arch.a).add(arch.rc);
                    Coord2d C = B.sub(A).div(2).add(A);
                    double a = A.add(B.sub(A).div(4)).dist(NUtils.player().rc);
                    double b = B.add(A.sub(B).div(4)).dist(NUtils.player().rc);
                    double c = C.dist(NUtils.player().rc);
                    if (a < b && a < c)
                        gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                                0, 18);
                    else if (b < c && b < a)
                        gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                                0, 16);
                    else
                        gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                                0, 17);
                } else {
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 16);
                }
            }
        }

        return Results.SUCCESS();
}
}
