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

public class GetCurs extends NTask
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
        cursname = NUtils.getUI().root.cursorRes;
        return NParser.checkName(cursname, name);
    }

    public String getResult(){
        return cursname;
    }
}
