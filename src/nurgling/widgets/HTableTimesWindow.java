package nurgling.widgets;

import haven.UI;
import nurgling.i18n.L10n;
import nurgling.tools.HTableTimesCatalog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Read-only herbalist table drying times: item, product, wiki times. */
public class HTableTimesWindow extends StationTimesWindow {
    private static final String KEY = "htable_times";
    private static final int WIDTH = UI.scale(560);
    private static final int[] COLUMNS = {8, 188, 372, 456};
    private static final Color PRODUCT = new Color(164, 214, 150);

    private HTableTimesWindow() {
        super(KEY, L10n.get("htable_times.title"), WIDTH, COLUMNS, PRODUCT,
                new String[]{
                        L10n.get("htable_times.col.item"),
                        L10n.get("htable_times.col.product"),
                        L10n.get("htable_times.col.real"),
                        L10n.get("htable_times.col.ingame")},
                rows());
    }

    public static void open() {
        open(KEY, HTableTimesWindow::new);
    }

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (HTableTimesCatalog.Entry entry : HTableTimesCatalog.all()) {
            rows.add(new Row(entry.item + " → " + entry.product,
                    entry.item, entry.product, entry.realTime, entry.inGameTime));
        }
        return rows;
    }
}
