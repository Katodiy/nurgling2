package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.FollowAndPose;
import nurgling.tasks.IsVesselMoving;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static haven.MCache.tilesz;

public class NomadOistererSnekkja implements Action {
    static NAlias oyster = new NAlias("gfx/terobjs/herbs/oyster");
    static NAlias animals = new NAlias(new ArrayList<String>(
            Arrays.asList("/boar", "/badger", "/wolverine", "/adder", "/bat", "/moose", "/bear", "/wolf", "/lynx", "/walrus")));

    public NomadOistererSnekkja(String path){
        this.path = path;
    }
    public static boolean alarmOyster() throws InterruptedException {
        if(!Finder.findGobs(oyster, 275).isEmpty())
            return true;
        return false;
    }

    public static boolean alarmAnimal() throws InterruptedException {
        ArrayList<Gob> ar = Finder.findGobs(animals, 220);
        if(!Finder.findGobs(animals, 275).isEmpty())
            return true;
        return false;
    }

    public static boolean alarmFoe() throws InterruptedException {
        Gob borka = Finder.findGob(new NAlias("/borka"));
        if (borka != NUtils.player())
                return true;
        return false;
    }

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        int temp = NomadOisterer.oic.get();
        if(path.length() == 0)
            return Results.ERROR("Set proper path file.");

        URL url = NUtils.class.getProtectionDomain ().getCodeSource ().getLocation ();
        if ( url != null ) {

            try {
                DataInputStream in =
                        new DataInputStream ( new FileInputStream(path));
                while( true ){
                    try {
                        if ( !(in.available ()>0) )
                            break;
                        coords.add ( new Coord2d (in.readInt (),in.readInt ()));
                    }
                    catch ( IOException e ) {
                        break;
                    }
                }
            }
            catch (  FileNotFoundException e ) {
                e.printStackTrace ();
            }
        }
        gui.msg("File is loaded");

        Gob snek = Finder.findGob(new NAlias("snekkja"));
        if(alarmFoe()){
            NUtils.getUI().msg("Found FOE! TPOUT!");
            NUtils.hfout();
            return Results.ERROR("Found foe and tp outed.");
        }
        if(gui.hand.isEmpty()){
            NUtils.rclickGob(snek);
            new SelectFlowerAction("Man the helm", snek).run(gui);
            NUtils.getUI().core.addTask(new FollowAndPose(NUtils.player(),"gfx/borka/snekkjaman0"));
        }

        Gob gob = Finder.findGob (new NAlias ("pow") );
        Coord2d shift = gob.rc;

        AtomicInteger oister_quantity = new AtomicInteger(0);
        for(Coord2d coord : coords){
            if(snek.getattr(GobHealth.class).hp <= 0.5){
                NUtils.getGameUI().msg("Fix the boat!");
            }

            Coord2d pos =  coord.add ( shift );
            Coord poscoord = pos.div ( tilesz ).floor ();
            pos = new Coord2d ( ( poscoord ).x * tilesz.x + tilesz.x/2, ( poscoord ).y * tilesz.y + tilesz.y/2);
            previousCoord = pos;//заготовка под задкование
            PathFinder pf = new PathFinder(pos);
            pf.waterMode = true;
            pf.run(gui);

            if(alarmAnimal()){
                //TODO flee back
                NUtils.getGameUI().msg("Continue!");
                continue;
            }
            if(alarmFoe()){
                NUtils.getUI().msg("Found FOE! TPOUT!");
                NUtils.hfout();
                return Results.ERROR("Found foe and tp outed.");
            }
            if(alarmOyster()){
                if(alarmAnimal()){
                    //TODO flee back
                    NUtils.getGameUI().msg("Continue!");
                    continue;
                }
                if(alarmFoe()){
                    NUtils.getUI().msg("Found FOE! TPOUT!");
                    NUtils.hfout();
                    return Results.ERROR("Found foe and tp outed.");
                }
                new OisterFounder().run(gui);
            }
            //Thread.sleep(200);
        }
        NUtils.getGameUI().msg("End of coordinate list. HF outing...");
        NUtils.getGameUI().msg("Totally for circle : " + (NomadOisterer.oic.get() - temp) + " oysters.");
        NUtils.getGameUI().msg("Totally for day: " + NomadOisterer.oic.get() + " oysters.");

        NUtils.hfout();
        return null;
    }


    String path = "";
    Coord2d previousCoord = null;
    ArrayList<Coord2d> coords = new ArrayList<Coord2d>();
    NArea area;
}