package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.overlays.NTexLabel;

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
                    ((NMapView) NUtils.getGameUI().map).changeAreaName(area.id, te.text());
                    item.text.settext(te.text());
                    NConfig.needAreasUpdate();
                    Gob dummy = ((NMapView) NUtils.getGameUI().map).dummys.get(area.gid);
                    if(dummy != null) {
                        NTexLabel tl = (NTexLabel) dummy.findol(NTexLabel.class).spr;
                        tl.label = new TexI(NStyle.openings.render(area.name).img);
                    }
                }
            }
        }, prev.pos("ur").adds(5, -6));
    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            hide();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    public static void changeName(NArea area, NAreasWidget.AreaItem item)
    {
        NUtils.getGameUI().nean.show();
        NUtils.getGameUI().nean.raise();
        NUtils.getGameUI().nean.area = area;
        NUtils.getGameUI().nean.item = item;
        NUtils.getGameUI().nean.te.settext(area.name);

    }

    public NArea area;
    NAreasWidget.AreaItem item;
}
