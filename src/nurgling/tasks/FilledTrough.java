package nurgling.tasks;

import haven.GItem;
import haven.Gob;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class FilledTrough implements NTask
{
    NAlias name = null;
    Gob trough;

    GItem target = null;
    public FilledTrough(Gob trough, NAlias name)
    {
        this.name = name;
        this.trough = trough;
    }

    @Override
    public boolean check() {
        if (target != null)
            if (((NGItem) target).name() != null)
                name = new NAlias(((NGItem) target).name());
            else
                return false;
        if (trough.ngob.getModelAttribute() == 7 && NUtils.getGameUI().vhand == null)
            return true;
        for (Widget widget = NUtils.getGameUI().getInventory().child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                String item_name;
                if ((item_name = ((NGItem) item.item).name()) == null) {
                    return false;
                } else {
                    if (NParser.checkName(item_name, name)) {
                        return false;
                    }
                }
            }
        }
        return NUtils.getGameUI().vhand == null;

    }

}
