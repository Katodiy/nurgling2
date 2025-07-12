package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class TabacoAction implements Action {

    NAlias raw = new NAlias("Fresh Leaf of Pipeweed");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation rtables = new NArea.Specialisation(Specialisation.SpecName.htable.toString(), "Pipeweed");

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rtables);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        if(new Validator(req, opt).run(gui).IsSuccess()) {

            NArea npile_area = NContext.findIn(raw.getDefault());
            Pair<Coord2d,Coord2d> pile_area = npile_area!=null?npile_area.getRCArea():null;
            if(pile_area==null)
            {
                return Results.ERROR("Fresh Leaf of Pipeweed not found");
            }

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob htable : Finder.findGobs(NContext.findSpec(rtables.name),
                    new NAlias("gfx/terobjs/htable"))) {
                Container cand = new Container(htable, "Herbalist Table");

                cand.initattr(Container.Space.class);
                cand.initattr(Container.Tetris.class);
                Container.Tetris tetris = cand.getattr(Container.Tetris.class);
                ArrayList<Coord> coords = new ArrayList<>();

                coords.add(new Coord(2, 1));

                tetris.getRes().put(Container.Tetris.TARGET_COORD, coords);

                containers.add(cand);
            }


            new FreeContainers(containers, new NAlias("Cured Pipeweed")).run(gui);
            new FillContainersFromPiles(containers, pile_area, raw).run(gui);

            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
