package nurgling.tasks;

import haven.Coord;
import haven.Resource;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class GetCurs implements NTask
{
    String name;
    String cursname;
    public GetCurs(String name)
    {
        this.name = name;
    }

    @Override
    public boolean check()
    {
        Resource res = NUtils.getUI().getcurs(Coord.z);
        if(res!=null) {
            cursname = res.name;
            return NParser.checkName(res.name, name);
        }
        return false;
    }

    public String getResult(){
        return cursname;
    }
}
