package nurgling.widgets;

import haven.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Window displaying all saved custom icons in a scrollable grid.
 * Allows clicking an icon to select it for editing.
 */
public class SavedIconsWindow extends Window {

    private static final int WINDOW_WIDTH = UI.scale(400);
    private static final int WINDOW_HEIGHT = UI.scale(350);
    private static final int ICON_SIZE = UI.scale(40);
    private static final int ICON_GAP = UI.scale(8);
    private static final int MARGIN = UI.scale(15);

    private final Consumer<CustomIcon> onIconSelected;
    private IconGridList iconGrid;
    private String selectedIconId = null;
    private boolean pendingClose = false;

    public SavedIconsWindow(Consumer<CustomIcon> onIconSelected) {
        super(new Coord(WINDOW_WIDTH, WINDOW_HEIGHT), "Saved Icons", true);
        this.onIconSelected = onIconSelected;

        int y = UI.scale(10);

        // Instructions label
        add(new Label("Click an icon to edit it:"), MARGIN, y);
        y += UI.scale(25);

        // Icon grid
        Coord gridSize = new Coord(WINDOW_WIDTH - MARGIN * 2, WINDOW_HEIGHT - y - UI.scale(50));
        iconGrid = add(new IconGridList(gridSize), MARGIN, y);
        y += gridSize.y + UI.scale(10);

        // Close button
        int btnWidth = UI.scale(100);
        add(new Button(btnWidth, "Close") {
            @Override
            public void click() {
                close();
            }
        }, (WINDOW_WIDTH - btnWidth) / 2, y);

        pack();
        refresh();
    }

    public void refresh() {
        if (iconGrid != null) {
            iconGrid.refresh();
        }
    }

    public void setSelectedIconId(String iconId) {
        this.selectedIconId = iconId;
    }

    private void close() {
        hide();
        destroy();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (pendingClose) {
            pendingClose = false;
            close();
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            close();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    /**
     * Scrollable list that displays icons in a grid layout.
     * Each row contains multiple icons.
     */
    private class IconGridList extends SListBox<IconRow, Widget> {
        private List<IconRow> rows = new ArrayList<>();
        private final int iconsPerRow;

        IconGridList(Coord sz) {
            super(sz, ICON_SIZE + ICON_GAP);
            // Calculate how many icons fit per row
            iconsPerRow = Math.max(1, (sz.x - UI.scale(20)) / (ICON_SIZE + ICON_GAP));
        }

        void refresh() {
            rows.clear();

            List<CustomIcon> icons = CustomIconManager.getInstance().getIconList();
            IconRow currentRow = null;

            for (int i = 0; i < icons.size(); i++) {
                if (i % iconsPerRow == 0) {
                    currentRow = new IconRow();
                    rows.add(currentRow);
                }
                currentRow.icons.add(icons.get(i));
            }
        }

        @Override
        protected List<IconRow> items() {
            return rows;
        }

        @Override
        protected Widget makeitem(IconRow row, int idx, Coord sz) {
            return new IconRowWidget(this, sz, row);
        }
    }

    /**
     * Represents a row of icons.
     */
    private static class IconRow {
        final List<CustomIcon> icons = new ArrayList<>();
    }

    /**
     * Widget for displaying a row of icons.
     */
    private class IconRowWidget extends SListWidget.ItemWidget<IconRow> {
        private final List<IconSwatch> swatches = new ArrayList<>();

        IconRowWidget(SListBox<IconRow, ?> list, Coord sz, IconRow row) {
            super(list, sz, row);

            int x = 0;
            for (CustomIcon icon : row.icons) {
                IconSwatch swatch = add(new IconSwatch(icon), x, 0);
                swatches.add(swatch);
                x += ICON_SIZE + ICON_GAP;
            }
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);

            // Draw selection highlight around selected icon
            if (selectedIconId != null) {
                for (IconSwatch swatch : swatches) {
                    if (swatch.icon.getId().equals(selectedIconId)) {
                        Coord pos = swatch.c.sub(2, 2);
                        Coord size = swatch.sz.add(4, 4);
                        g.chcolor(100, 200, 100, 255);
                        g.rect(pos, size);
                        g.chcolor();
                    }
                }
            }
        }
    }

    /**
     * A clickable icon swatch.
     */
    private class IconSwatch extends Widget {
        final CustomIcon icon;
        private Tex previewTex;
        private Text.Line nameText;

        IconSwatch(CustomIcon icon) {
            super(new Coord(ICON_SIZE, ICON_SIZE));
            this.icon = icon;

            BufferedImage preview = icon.getPreviewImage();
            if (preview != null) {
                previewTex = new TexI(preview);
            }
        }

        @Override
        public void draw(GOut g) {
            // Background
            g.chcolor(40, 40, 40, 200);
            g.frect(Coord.z, sz);
            g.chcolor();

            if (previewTex != null) {
                // Center the icon
                Coord texSz = previewTex.sz();
                Coord pos = new Coord((sz.x - texSz.x) / 2, (sz.y - texSz.y) / 2);
                g.image(previewTex, pos);
            } else {
                g.chcolor(128, 128, 128, 255);
                g.frect(Coord.z, sz);
                g.chcolor();
            }

            // Border
            g.chcolor(80, 80, 80, 255);
            g.rect(Coord.z, sz);
            g.chcolor();
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            return icon.getName();
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                selectedIconId = icon.getId();
                if (onIconSelected != null) {
                    onIconSelected.accept(icon);
                }
                // Defer the close to next tick to avoid destroying during event propagation
                pendingClose = true;
                return true;
            }
            return super.mousedown(ev);
        }
    }
}
