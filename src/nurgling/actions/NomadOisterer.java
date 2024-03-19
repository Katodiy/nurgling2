package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.FollowAndPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static haven.MCache.tilesz;

public class NomadOisterer implements Action {
    static NAlias oyster = new NAlias("oyster", "oystermushroom");
    static NAlias animals = new NAlias(new ArrayList<String>(
            Arrays.asList("/boar", "/badger", "/wolverine", "/adder", "/bat", "/moose", "/bear", "/wolf", "/lynx", "/walrus")));

    public static boolean alarmOyster() throws InterruptedException {
        if(!Finder.findGobs(oyster, 275).isEmpty())
            return true;
        return false;
    }

    public static boolean alarmAnimal() throws InterruptedException {
        ArrayList<Gob> ar = Finder.findGobs(animals, 220);
        if(!Finder.findGobs(animals, 275).isEmpty()){
            return true;
        }
        return false;
    }

    private boolean alarmFoe() {
        return false;
    }

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        path = "./oyster1.dat";

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

        Gob boat = Finder.findGob(new NAlias("rowboat"));
        if(gui.hand.isEmpty()){
            PathFinder pathFinder = new PathFinder(boat);
            pathFinder.isHardMode = true;
            pathFinder.run(gui);
            NUtils.rclickGob(boat);
            NUtils.getUI().core.addTask(new FollowAndPose(NUtils.player(),"gfx/borka/rowboat-d"));
            //(<gfx/borka/rowboat-d(v5)>, Message(>3f))
            //NUtils.waitEvent(()->NUtils.isPose(NUtils.getGameUI().getMap().player(),new NAlias("rowboat-d")),5000);
        }

        Gob gob = Finder.findGob (new NAlias ("pow") );
        Coord2d shift = gob.rc;

        for(Coord2d coord : coords){
            if(boat.getattr(GobHealth.class).hp <= 0.5){
                NUtils.getGameUI().msg("Fix the boat!");
            }

            Coord2d pos =  coord.add ( shift );
            Coord poscoord = pos.div ( MCache.tilesz ).floor ();
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
                //TODO logout and cry
            }
            if(alarmOyster()){
                ArrayList<Gob> oysters = Finder.findGobs(oyster, 275);
                NUtils.getGameUI().msg("Found " + oysters.size() + " oysters.");
                if(alarmAnimal()){
                    //TODO flee back
                    NUtils.getGameUI().msg("Continue!");
                    continue;
                }
                if(alarmFoe()){
                    //TODO logout and cry
                }
                for(Gob oyster : oysters){
                    PathFinder tpf = new PathFinder(oyster);
                    tpf.waterMode = true;
                    tpf.run(gui);
                    if(alarmAnimal()){
                        //TODO
                        NUtils.getGameUI().msg("Continue!");
                        continue;
                    }
                    if(alarmFoe()){

                    }
                    if(oyster != null){
                        new SelectFlowerAction("Pick", oyster).run(gui);
                    }
                }
            }

            pf.run(gui);

        }

        return null;
    }


    String path;
    Coord2d previousCoord = null;
    ArrayList<Coord2d> coords = new ArrayList<Coord2d>();
    NArea area;
}