package nurgling.actions;

import haven.*;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.conf.QuickActionPreset;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Pattern;

import static haven.OCache.posres;

public class QuickActionBot implements Action {
    boolean ignorePatter = false;
    boolean useMouse = false;
    QuickActionPreset preset = null;

    public QuickActionBot(boolean ignorePatter) {
        this.ignorePatter = ignorePatter;
        this.useMouse = false;
        this.preset = null;
    }

    public QuickActionBot(boolean ignorePatter, boolean useMouse) {
        this.ignorePatter = ignorePatter;
        this.useMouse = useMouse;
        this.preset = null;
    }

    public QuickActionBot(boolean ignorePatter, boolean useMouse, QuickActionPreset preset) {
        this.ignorePatter = ignorePatter;
        this.useMouse = useMouse;
        this.preset = preset;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        double dist = (Integer) NConfig.get(NConfig.Key.q_range) * MCache.tilesz.x;
        if (!ignorePatter) {

            ArrayList<Gob> gobs;
            Coord2d centerPoint;
            
            // Get patterns either from preset or from the current UI settings
            ArrayList<Pattern> patterns = getPatterns();
            
            if (useMouse) {
                // Get mouse position in world coordinates
                centerPoint = getMouseWorldPosition(gui);
                if (centerPoint == null) {
                    return Results.FAIL();
                }
                gobs = Finder.findGobByPatternsAroundPoint(patterns, dist, centerPoint);
                gobs.sort(Comparator.comparingDouble(a -> a.rc.dist(centerPoint)));
            } else {
                // Use player position (original behavior)
                centerPoint = NUtils.player().rc;
                gobs = Finder.findGobByPatterns(patterns, dist);
                gobs.sort(NUtils.d_comp);
            }
            
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

    /**
     * Get patterns from the preset if available, otherwise from current settings
     */
    @SuppressWarnings("unchecked")
    private ArrayList<Pattern> getPatterns() {
        ArrayList<Pattern> patterns = new ArrayList<>();
        
        if (preset != null && preset.patterns != null) {
            // Use patterns from the specified preset
            for (HashMap<String, Object> item : preset.patterns) {
                Boolean enabled = (Boolean) item.getOrDefault("enabled", true);
                if (enabled) {
                    String patternStr = (String) item.get("name");
                    if (patternStr != null) {
                        try {
                            patterns.add(Pattern.compile(patternStr));
                        } catch (Exception e) {
                            // Invalid pattern, skip it
                        }
                    }
                }
            }
        } else {
            // Fallback to NUtils method for backward compatibility
            patterns = NUtils.getQAPatterns();
        }
        
        return patterns;
    }

    private Coord2d getMouseWorldPosition(NGameUI gui) {
        try {
            // Get mouse coordinates relative to map widget
            Coord mvc = gui.map.rootxlate(gui.ui.mc);
            
            if (!mvc.isect(Coord.z, gui.map.sz)) {
                return null; // Mouse not over map
            }

            // Use the same coordinate system as the MapView
            // This approach uses a hit test to get world coordinates
            final Coord2d[] result = new Coord2d[1];
            final boolean[] done = new boolean[1];
            
            gui.map.new Maptest(mvc) {
                protected void hit(Coord pc, Coord2d mc) {
                    result[0] = mc;
                    done[0] = true;
                }
                protected void nohit(Coord pc) {
                    result[0] = null;
                    done[0] = true;
                }
            }.run();
            
            // Wait for the maptest to complete
            int timeout = 100; // 100ms timeout
            while (!done[0] && timeout > 0) {
                Thread.sleep(1);
                timeout--;
            }
            
            return result[0];
        } catch (Exception e) {
            return null;
        }
    }
}
