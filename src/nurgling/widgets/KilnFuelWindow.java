package nurgling.widgets;

import haven.UI;
import nurgling.i18n.L10n;
import nurgling.tools.KilnFuelCatalog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Read-only kiln firing table: item, branch count, wiki times. */
public class KilnFuelWindow extends StationTimesWindow {
    private static final String KEY = "kiln_fuel";
    private static final int WIDTH = UI.scale(460);
    private static final int[] COLUMNS = {8, 205, 268, 350};
    private static final Color FUEL = new Color(255, 196, 92);

    private KilnFuelWindow() {
        super(KEY, L10n.get("kiln_fuel.title"), WIDTH, COLUMNS, FUEL,
                new String[]{
                        L10n.get("kiln_fuel.col.item"),
                        L10n.get("kiln_fuel.col.fuel"),
                        L10n.get("kiln_fuel.col.real"),
                        L10n.get("kiln_fuel.col.ingame")},
                rows());
    }

    public static void open() {
        open(KEY, KilnFuelWindow::new);
    }

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (KilnFuelCatalog.Entry entry : KilnFuelCatalog.all()) {
            String fuel = Integer.toString(entry.fuelUnits);
            rows.add(new Row(entry.item + " — " + fuel,
                    entry.item, fuel, entry.realTime, entry.inGameTime));
        }
        return rows;
    }
}
