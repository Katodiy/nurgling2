package nurgling.tasks;

import haven.Gob;
import haven.StaticSprite;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class IsOverlay extends NTask
{

    Gob gob;

    NAlias name = null;
    int count = 0;
    boolean found = false;

    public IsOverlay(Gob gob, NAlias name)
    {
        this.gob = gob;
        this.name = name;
        this.maxCounter = 500;
    }

    @Override
    public boolean check()
    {

        for (Gob.Overlay ol : gob.ols) {
            if(ol.spr instanceof StaticSprite) {
                if(NParser.checkName((ol.spr).res.name,name)){
                    found = true;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getResult(){
        return found;
    }
}