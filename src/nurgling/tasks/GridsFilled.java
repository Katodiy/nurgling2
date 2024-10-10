package nurgling.tasks;

import haven.Coord;
import nurgling.NUtils;

public class GridsFilled implements NTask {
    public GridsFilled(Coord coord) {
    this.coord = coord;
    }
    Coord coord;

    @Override
    public boolean check() {
        if(NUtils.getGameUI().map.glob.map.grids.size()==9)
        {
            if(NUtils.getGameUI().map.glob.map.grids.get(coord)==null)
            {
                return true;
            }
            for(Coord gc : NUtils.getGameUI().map.glob.map.grids.keySet())
            {
                Coord pos = gc.sub(coord.sub(1,1));
                if(pos.x<0||pos.x>=3||pos.y<0||pos.y>=3)
                {
                    return false;
                }
            }

            return true;
        }
        return false;
    }
}
