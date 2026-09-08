package nurgling.widgets;

import haven.Coord;
import haven.GOut;
import haven.SListBox;
import haven.UI;
import haven.Widget;
import haven.Window;
import nurgling.NGameUI;
import nurgling.NStyle;
import nurgling.NUtils;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

/**
 * Read-only reference table for a workstation: four text columns, one of them accented.
 * Subclasses supply the title, column layout and rows; the data itself lives in the
 * matching catalog under {@code nurgling.tools}.
 */
public class StationTimesWindow extends Window {
    private static final int HEIGHT = UI.scale(380);
    private static final int ROW_HEIGHT = UI.scale(20);
    private static final Color HEADER_BG = new Color(55, 65, 62, 220);
    /** Column the accent color is applied to; every table highlights its second column. */
    private static final int ACCENT_COLUMN = 1;

    /** One table line: four already-formatted cells plus the text shown on hover. */
    public static final class Row {
        public final String[] cells;
        public final String tooltip;

        public Row(String tooltip, String... cells) {
            this.cells = cells;
            this.tooltip = tooltip;
        }
    }

    /** Identifies the table so {@link #open} raises the existing window instead of stacking copies. */
    private final String key;

    protected StationTimesWindow(String key, String title, int width, int[] columns,
                                 Color accent, String[] headers, List<Row> rows) {
        super(new Coord(width, HEIGHT), title, true);
        this.key = key;
        int inner = width - UI.scale(16);
        add(new Header(new Coord(inner, ROW_HEIGHT), columns, accent, headers), UI.scale(8, 4));
        add(new EntryList(new Coord(inner, HEIGHT - UI.scale(36)), columns, accent, rows), UI.scale(8, 26));
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            ui.destroy(this);
        } else {
            super.wdgmsg(msg, args);
        }
    }

    /** Raises the table already on screen, or centers a new one. */
    protected static void open(String key, Supplier<StationTimesWindow> factory) {
        NGameUI gui = NUtils.getGameUI();
        if (gui == null)
            return;
        for (Widget w = gui.child; w != null; w = w.next) {
            if (w instanceof StationTimesWindow && key.equals(((StationTimesWindow) w).key)) {
                w.raise();
                return;
            }
        }
        StationTimesWindow wnd = factory.get();
        Coord pos = gui.sz.sub(wnd.sz).div(2);
        gui.add(wnd, new Coord(Math.max(0, pos.x), Math.max(0, pos.y)));
        wnd.raise();
    }

    private static void drawCells(GOut g, int[] columns, Color accent, String[] cells) {
        for (int i = 0; i < cells.length && i < columns.length; i++) {
            if (i == ACCENT_COLUMN)
                g.chcolor(accent);
            g.text(cells[i], new Coord(UI.scale(columns[i]), UI.scale(3)));
            if (i == ACCENT_COLUMN)
                g.chcolor();
        }
    }

    private static class Header extends Widget {
        private final int[] columns;
        private final Color accent;
        private final String[] headers;

        private Header(Coord sz, int[] columns, Color accent, String[] headers) {
            super(sz);
            this.columns = columns;
            this.accent = accent;
            this.headers = headers;
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(HEADER_BG);
            g.frect(Coord.z, sz);
            g.chcolor();
            drawCells(g, columns, accent, headers);
        }
    }

    private static class EntryList extends SListBox<Row, Widget> {
        private final int[] columns;
        private final Color accent;
        private final List<Row> rows;

        private EntryList(Coord sz, int[] columns, Color accent, List<Row> rows) {
            super(sz, ROW_HEIGHT);
            this.columns = columns;
            this.accent = accent;
            this.rows = rows;
        }

        @Override
        protected List<Row> items() {
            return rows;
        }

        @Override
        protected Widget makeitem(Row row, int idx, Coord sz) {
            return new EntryRow(row, idx, sz, columns, accent);
        }
    }

    private static class EntryRow extends Widget {
        private final Row row;
        private final int idx;
        private final int[] columns;
        private final Color accent;

        private EntryRow(Row row, int idx, Coord sz, int[] columns, Color accent) {
            super(sz);
            this.row = row;
            this.idx = idx;
            this.columns = columns;
            this.accent = accent;
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(idx % 2 == 0 ? NStyle.rowOdd : NStyle.rowEven);
            g.frect(Coord.z, sz);
            g.chcolor();
            drawCells(g, columns, accent, row.cells);
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            return row.tooltip;
        }
    }
}
