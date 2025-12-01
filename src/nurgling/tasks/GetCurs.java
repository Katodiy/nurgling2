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
        this.maxCounter = 300;
    }

    @Override
    public boolean check()
    {
        if(NUtils.getUI().root.cursor==null || !NUtils.getUI().root.cursor.isReady())
            return false;
        Resource res = NUtils.getUI().root.cursor.get();
        if(res == null)
            return false;
        cursname = res.name;
        return NParser.checkName(cursname, name);
    }

    public String getResult(){
        return cursname;
    }
}
