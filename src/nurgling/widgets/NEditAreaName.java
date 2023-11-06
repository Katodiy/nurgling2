package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;

public class NEditAreaName extends Window
{
    private final TextEntry te;
    public NEditAreaName()
    {
        super(UI.scale(new Coord(260, 25)), "Edit name");
        prev = add(te = new TextEntry(UI.scale(200), ""));
        add(new Button(UI.scale(60), "Save")
        {
            @Override
            public void click()
            {
                super.click();
                NEditAreaName.this.hide();
                if(!te.text().isEmpty())
                {
                    ((NMapView) NUtils.getGameUI().map).changeAreaName(area.name, te.text());
                    NConfig.needAreasUpdate();
                }
            }
        }, prev.pos("ur").adds(5, -6));
    }

    public static void changeName(NArea area)
    {
        NUtils.getGameUI().nean.show();
        NUtils.getGameUI().nean.area = area;
        NUtils.getGameUI().nean.te.settext(area.name);
    }

    public NArea area;
}
