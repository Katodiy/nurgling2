package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.bots.SelectArea;
import nurgling.areas.NArea;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class NomadCalibration implements Action {
    public static NAlias anchors = new NAlias(new ArrayList<String>(Arrays.asList("milestone-stone-m", "milestone-wood", "pow")));

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        nurgling.widgets.bots.NomadCalibrator w = null;
        try {
            worked.set(true);
            NUtils.getGameUI().add((w = new nurgling.widgets.bots.NomadCalibrator(worked)), UI.scale(200,200));

            SelectArea sela;
            NUtils.getGameUI().msg("Please select area for deforestation");
            (sela = new SelectArea()).run(gui);

            Gob start_gob = Finder.findGob(sela.getRCArea(), anchors);
            if (start_gob != null) {
                Coord2d start = Finder.findGob(sela.getRCArea(), anchors).rc;
                Coord2d next = new Coord2d(start.x, start.y);
                int counter = 0;
                int sum = 0;

                while (worked.get()) {
                    Thread.sleep(100);
                    Coord2d current = NUtils.getGameUI().map.player().rc;
                    if (next.dist(current) >= 100) {
                        next = current;
                        Coord2d forWrite = next.sub(start);
                        coords.add(forWrite);
                        if (counter > 9) {
                            counter = 0;
                            gui.msg("Total coord: " + sum);
                        }
                        counter++;
                        sum++;
                    }
                }

                gui.msg("NomadCalibration stopped. Total coords : " + sum);
                return Results.SUCCESS();
            } else {
                return Results.ERROR("NomadCalibration did not find start_gob. No milestone or POW in range.");
            }
        }
        catch (InterruptedException e)
        {
            throw e;
        }

    }

    ArrayList<Coord2d> coords;
    NArea area;
    AtomicBoolean worked = new AtomicBoolean(false);
}