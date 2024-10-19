package nurgling.tasks;

import haven.Gob;
import haven.StaticSprite;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class IsOverlay implements NTask
{

    Gob gob;

    NAlias name = null;
    int count = 0;
    boolean found = false;

    public IsOverlay(Gob gob, NAlias name)
    {
        this.gob = gob;
        this.name = name;
    }

    @Override
    public boolean check()
    {
        count++;

        for (Gob.Overlay ol : gob.ols) {
            if(ol.spr instanceof StaticSprite) {
                if(NParser.checkName((ol.spr).res.name,name)){
                    found = true;
                    return true;
                }
            }
        }
        return count > 200;
    }

    public boolean getResult(){
        return found;
    }
}