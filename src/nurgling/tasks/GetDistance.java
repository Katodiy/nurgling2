package nurgling.tasks;

import haven.Coord2d;
import nurgling.NUtils;

public class GetDistance implements NTask
{
    public GetDistance(Coord2d coord)
    {
        this.next = coord;
    }
    Coord2d next;
    @Override
    public boolean check()
    {
        Coord2d current = NUtils.getGameUI().map.player().rc;
        if (next.dist(current) >= 100) {
            next = current;
            return true;

        }
        return false;
    }

    public Coord2d getNext(){
        return next;
    }

}