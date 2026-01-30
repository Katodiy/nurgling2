package nurgling.tasks;

import haven.Resource;
import nurgling.NUtils;
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
        if (NUtils.getUI().root.cursor == null || !NUtils.getUI().root.cursor.isReady())
            if (NUtils.getUI().root.cursorRes == null || NUtils.getUI().root.cursorRes.isEmpty())
                return false;
        if (NUtils.getUI().root.cursorRes != null && !NUtils.getUI().root.cursorRes.isEmpty())
            cursname = NUtils.getUI().root.cursorRes;
        else
        {
            Resource res = NUtils.getUI().root.cursor.get();
            if (res == null)
                return false;

            cursname = res.name;
        }
        return NParser.checkName(cursname, name);
    }

    public String getCursorName(){
        return cursname;
    }
}
