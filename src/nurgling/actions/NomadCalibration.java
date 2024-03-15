package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.bots.SelectArea;
import nurgling.areas.NArea;
import nurgling.tasks.GetDistance;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class NomadCalibration extends ActionWithFinal {
    public static NAlias anchors = new NAlias("milestone-stone-m", "milestone-wood", "pow");

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        try {

            SelectArea sela;
            NUtils.getGameUI().msg("Please select area with anchor");
            (sela = new SelectArea()).run(gui);

            Gob start_gob = Finder.findGob(sela.getRCArea(), anchors);
            if (start_gob != null) {
                Coord2d start = Finder.findGob(sela.getRCArea(), anchors).rc;
                Coord2d next = new Coord2d(start.x, start.y);

                while (true) {
                    GetDistance gdst = new GetDistance(next);
                    NUtils.getUI().core.addTask(gdst);
                    Coord2d forWrite = gdst.getNext().sub(start);
                    coords.add(forWrite);
                    next = gdst.getNext();
                    NUtils.getGameUI().msg("Added 1 coordinate. " + coords.size());
                }
            } else {
                return Results.ERROR("NomadCalibration did not find start_gob. No milestone or POW in range.");
            }
        }
        catch (InterruptedException e)
        {
            throw e;
        }

    }

    @Override
    public void endAction(){

        if(coords.size() > 0) {
            NUtils.getGameUI().msg("NomadCalibration stopped. Total coords : " + coords.size());

            String path = "./nomad.dat";

            DataOutputStream out = null;
            try {
                URL url = NUtils.class.getProtectionDomain().getCodeSource().getLocation();
                out = new DataOutputStream(new FileOutputStream(path));
                for (Coord2d coord2d : coords) {
                    out.writeInt((int) coord2d.x);
                    out.writeInt((int) coord2d.y);
                }
                out.close();
                NUtils.getGameUI().msg("URL:" + url + " Path:" + path);
                NUtils.getGameUI().msg("NIO: " + java.nio.file.Paths.get(path).toAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    ArrayList<Coord2d> coords = new ArrayList<Coord2d>();
    NArea area;
}