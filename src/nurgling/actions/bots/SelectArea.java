package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;

public class SelectArea implements Action {

    public SelectArea() {

    }

    NArea.Space result;

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if (!((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.get()) {
            ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.set(true);

            nurgling.tasks.SelectArea sa;
            NUtils.getUI().core.addTask(sa = new nurgling.tasks.SelectArea());
            if (sa.getResult() != null) {
                result = sa.getResult();
            }
        }
        else
        {
            return Results.FAIL();
        }
        return Results.SUCCESS();
    }

    public Pair<Coord2d,Coord2d> getRCArea() {

        Coord begin = null;
        Coord end = null;
        for (Long id : result.space.keySet()) {
            MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(id);
            Area area = result.space.get(id).area;
            Coord b = area.ul.add(grid.ul);
            Coord e = area.br.add(grid.ul);
            begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
            end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
        }
        if (begin != null)
            return new Pair<Coord2d, Coord2d>(begin.mul(MCache.tilesz), end.sub(1, 1).mul(MCache.tilesz).add(MCache.tilesz));
        return null;
    }

}
