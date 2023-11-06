package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class NAreasWidget extends Window
{

    public NAreasWidget()
    {
        super(UI.scale(new Coord(600,500)), "Areas Settings");
        add(new Button(UI.scale(150), "Create area"){
            @Override
            public void click()
            {
                super.click();
                NUtils.getGameUI().msg("Please, select area");
                new Thread(new NAreaSelector(true)).start();
            }
        });
        pack();
    }
}
