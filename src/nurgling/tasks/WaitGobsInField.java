package nurgling.tasks;

import haven.Area;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class WaitGobsInField implements NTask
{
    Area area;
    int count;
    public WaitGobsInField(Area area , int count)
    {
        this.area = area;
        this.count = count;
    }

    @Override
    public boolean check()
    {
        Area.Tile[][] tiles =  area.getTiles(area, new NAlias("gfx/terobjs/moundbed"));
        int num = 0;
        for (int i = 0; i <= area.br.x - area.ul.x; i++) {
            for (int j = 0; j <= area.br.y - area.ul.y; j++)
            {
                if(!tiles[i][j].isFree && NParser.checkName(tiles[i][j].name, "field"))
                {
                    num++;
                }
            }
        }
        return num==count;
    }

}
