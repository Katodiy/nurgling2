package nurgling.widgets.nsettings;

import haven.UI;
import nurgling.NConfig;
import nurgling.i18n.L10n;
import nurgling.widgets.DropContainer;

public class Dropper extends Panel {

    DropContainer dc;
    public Dropper() {
        super(L10n.get("dropper.title"));
        int margin = UI.scale(5);

        add(dc = new DropContainer(), UI.scale(margin, 30));
    }

    @Override
    public void load() {
        dc.load();
    }

    @Override
    public void save() {
        NConfig.set(NConfig.Key.dropConf, dc.getDropJson());
    }

}