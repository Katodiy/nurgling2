package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class OysterFinder implements Action {
    @Override
    public Results run ( NGameUI gui )
            throws InterruptedException {
        try {
            DataInputStream in =
                    new DataInputStream(new FileInputStream(nomadFilePath));
            while (true) {
                try {
                    if (!(in.available() > 0))
                        break;
                    marks.add(new Coord2d(in.readInt(), in.readInt()));
                } catch (IOException e) {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return Results.ERROR("No gob for Lift");
    }

    public OysterFinder(
            Gob gob

    ) {
        this.gob = gob;
    }

    Gob gob = null;
    ArrayList<Coord2d> marks = new ArrayList<>();
    String nomadFilePath = null;
}