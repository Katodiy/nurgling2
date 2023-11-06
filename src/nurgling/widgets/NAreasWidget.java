package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tools.*;

import java.util.*;

public class NAreasWidget extends Window
{
    public Dropbox adrop;
    public NAreasWidget()
    {
        super(UI.scale(new Coord(600,500)), "Areas Settings");
        prev = add(new Button(UI.scale(150), "Create area"){
            @Override
            public void click()
            {
                super.click();
                NUtils.getGameUI().msg("Please, select area");
                new Thread(new NAreaSelector(NAreaSelector.Mode.CREATE)).start();
            }
        });

        Widget change = add(new Button(UI.scale(150), "Change area"){
            @Override
            public void click()
            {
                super.click();
                if(adrop.sel!=null)
                {
                    ((NMapView)NUtils.getGameUI().map).changeArea((String) adrop.sel);
                }
            }
        }, prev.pos("ur").adds(5, 0));

        add(new Button(UI.scale(150), "Remove area"){
            @Override
            public void click()
            {
                super.click();
                if(adrop.sel!=null)
                {
                    ((NMapView)NUtils.getGameUI().map).removeArea((String) adrop.sel);
                    NConfig.needAreasUpdate();
                    adrop.sel = null;
                }
            }
        }, change.pos("ur").adds(5, 0));


        prev = adrop = add(new Dropbox<String>(UI.scale(200), 5, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return new LinkedList<>(((NMapView)NUtils.getGameUI().map).areas()).get(i);
            }

            @Override
            protected int listitems() {
                return ((NMapView)NUtils.getGameUI().map).areas().size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                ((NMapView)NUtils.getGameUI().map).selectArea(item);
            }
        }, prev.pos("bl").adds(0, 10));

        add(new Button(UI.scale(100), "Edit name"){
            @Override
            public void click()
            {
                super.click();
                if(adrop.sel!=null)
                {
                    NArea area = ((NMapView)NUtils.getGameUI().map).findArea((String)adrop.sel);
                    NEditAreaName.changeName(area);
                }
            }
        }, prev.pos("ur").adds(5, -10));

        pack();
    }
}
