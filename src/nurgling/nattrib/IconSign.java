package nurgling.nattrib;

import haven.*;

public class IconSign implements NAttrib
{
    ResDrawable rd;
    Gob parent;
    String name;
    public IconSign(Gob parent, ResDrawable rd)
    {
        this.parent = parent;
        this.rd = rd;
        this.name = ResDrawable.getTexResName(parent,rd.sdt);
    }

    @Override
    public void tick(double dt)
    {

    }

    public String getName()
    {
        return name;
    }
}
