package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

public class LeatherAction implements Action {

    NAlias notraw = new NAlias(new ArrayList<>(Arrays.asList("hide", "Scale", "skin", "Hide", "Fur", "fur")), new ArrayList<>(Arrays.asList("Fresh", "Raw", "water")));
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation rdframe = new NArea.Specialisation(Specialisation.SpecName.ttub.toString());
        NArea.Specialisation rrawhides = new NArea.Specialisation(Specialisation.SpecName.readyHides.toString());
        NArea.Specialisation rtanning = new NArea.Specialisation(Specialisation.SpecName.tanning.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rdframe);
        req.add(rtanning);
        req.add(rrawhides);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        if (new Validator(req, opt).run(gui).IsSuccess()) {


            ArrayList<Container> containers = new ArrayList<>();

            for (Gob ttube : Finder.findGobs(NContext.findSpec(Specialisation.SpecName.ttub.toString()),
                    new NAlias("gfx/terobjs/ttub"))) {
                Container cand = new Container(ttube , "Tub");

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

            new FillFluid(containers, NContext.findSpec(Specialisation.SpecName.tanning.toString()).getRCArea(), new NAlias("tanfluid"), 2).run(gui);
            new FreeContainers(containers, new NAlias("Leather")).run(gui);
            new FillContainersFromPiles(containers, NContext.findSpec(Specialisation.SpecName.readyHides.toString()).getRCArea(), notraw).run(gui);
            new TransferToPiles(NContext.findSpec(Specialisation.SpecName.readyHides.toString()).getRCArea(), notraw).run(gui);

            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
