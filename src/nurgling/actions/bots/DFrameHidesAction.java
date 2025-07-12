package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class DFrameHidesAction implements Action {

    NAlias raw = new NAlias("Fresh");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation rdframe = new NArea.Specialisation(Specialisation.SpecName.dframe.toString());
        NArea.Specialisation rrawhides = new NArea.Specialisation(Specialisation.SpecName.rawhides.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rdframe);
        req.add(rrawhides);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        if(new Validator(req, opt).run(gui).IsSuccess()) {

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob dframe : Finder.findGobs(NContext.findSpec(Specialisation.SpecName.dframe.toString()),
                    new NAlias("gfx/terobjs/dframe"))) {
                Container cand = new Container(dframe,"Frame" );

                cand.initattr(Container.Space.class);
                cand.initattr(Container.Tetris.class);
                Container.Tetris tetris = cand.getattr(Container.Tetris.class);
                ArrayList<Coord> coords = new ArrayList<>();

                coords.add(new Coord(2, 2));
                coords.add(new Coord(2, 1));
                coords.add(new Coord(1, 1));

                tetris.getRes().put(Container.Tetris.TARGET_COORD, coords);

                containers.add(cand);
            }
            Pair<Coord2d,Coord2d> rca = NContext.findSpec(Specialisation.SpecName.dframe.toString()).getRCArea();
            boolean dir = rca.b.x - rca.a.x > rca.b.y - rca.a.y;
            containers.sort(new Comparator<Container>() {
                @Override
                public int compare(Container o1, Container o2) {
                    Gob gob1 = Finder.findGob(o1.gobid);
                    Gob gob2 = Finder.findGob(o2.gobid);
                    if(dir)
                    {
                        int res = Double.compare(gob1.rc.y,gob2.rc.y);
                        if(res == 0)
                            return Double.compare(gob1.rc.x,gob2.rc.x);
                        else
                            return res;
                    }
                    else
                    {
                        int res = Double.compare(gob1.rc.x,gob2.rc.x);
                        if(res == 0)
                            return Double.compare(gob1.rc.y,gob2.rc.y);
                        else
                            return res;
                    }
                }
            });


            new FreeContainers(containers, new NAlias(new ArrayList<>(Arrays.asList("Fur", "Hide", "Scale", "Tail", "skin", "hide")), new ArrayList<>(Arrays.asList("Fresh", "Raw")))).run(gui);
            new FillContainersFromPiles(containers, NContext.findSpec(Specialisation.SpecName.rawhides.toString()).getRCArea(), raw).run(gui);
            new TransferToPiles(NContext.findSpec(Specialisation.SpecName.rawhides.toString()).getRCArea(), new NAlias("Fresh")).run(gui);

            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
