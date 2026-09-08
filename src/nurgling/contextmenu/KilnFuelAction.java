package nurgling.contextmenu;

import haven.Gob;
import nurgling.actions.Action;
import nurgling.i18n.L10n;
import nurgling.widgets.KilnFuelWindow;

/** Lookup-only kiln firing table. Does not start a bot or add fuel. */
public class KilnFuelAction implements GobContextAction {
    private static final String KILN = "gfx/terobjs/kiln";

    @Override
    public boolean appliesTo(Gob gob) {
        return gob != null && gob.ngob != null && KILN.equals(gob.ngob.name);
    }

    @Override
    public String label() {
        return L10n.get("context.kiln_fuel");
    }

    @Override
    public Action create(Gob gob) {
        return null;
    }

    @Override
    public boolean isUiAction() {
        return true;
    }

    @Override
    public void performUi(Gob gob) {
        KilnFuelWindow.open();
    }
}
