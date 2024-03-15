package nurgling.actions;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.bots.SelectArea;
import nurgling.areas.NArea;
import nurgling.tasks.GetDistance;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

import static haven.MCache.tilesz;

public class NomadWalker implements Action {

    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        path = "./nomad.dat";
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

        Gob gob = Finder.findGob (new NAlias ("pow") );
        Coord2d shift = gob.rc;
        for(Coord2d coord : coords){
            Coord2d pos =  coord.add ( shift );
            Coord poscoord = pos.div ( MCache.tilesz ).floor ();
            pos = new Coord2d ( ( poscoord ).x * tilesz.x + tilesz.x/2, ( poscoord ).y * tilesz.y + tilesz.y/2);
            new PathFinder(pos).run(gui);
        }

        return null;
    }


    String path;
    ArrayList<Coord2d> coords = new ArrayList<Coord2d>();
    NArea area;
}