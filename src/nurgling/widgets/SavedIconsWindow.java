package nurgling.widgets;

import haven.*;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Window displaying all saved custom icons in a scrollable grid.
 * Allows clicking an icon to select it for editing.
 */
public class SavedIconsWindow extends Window {

    public static final int ICON_SIZE = UI.scale(34);
    public static final int GRID_PADDING = UI.scale(10);
    public static final int COLS = 6;

    private final Consumer<CustomIcon> onIconSelected;
    private boolean pendingClose = false;

    public SavedIconsWindow(Consumer<CustomIcon> onIconSelected) {
        super(new Coord(ICON_SIZE * COLS + GRID_PADDING * 2, UI.scale(350)), "Saved Icons");
        this.onIconSelected = onIconSelected;

        List<CustomIcon> icons = CustomIconManager.getInstance().getIconList();

        int contentWidth = ICON_SIZE * COLS + GRID_PADDING * 2;
        Widget contentPanel = new Widget(new Coord(contentWidth, 10000));

        int y = GRID_PADDING;

        if (icons.isEmpty()) {
            Label emptyLabel = new Label("No saved icons yet");
            contentPanel.add(emptyLabel, new Coord(GRID_PADDING, y));
            y += emptyLabel.sz.y + UI.scale(10);
        } else {
            Label label = new Label("Click an icon to edit it:");
            contentPanel.add(label, new Coord(GRID_PADDING, y));
            y += label.sz.y + UI.scale(6);

            int gridStartY = y;
            for (int i = 0; i < icons.size(); i++) {
                CustomIcon icon = icons.get(i);
                int col = i % COLS;
                int row = i / COLS;

                BufferedImage up = padIcon(icon.getImage(0), ICON_SIZE);
                BufferedImage down = padIcon(icon.getImage(1), ICON_SIZE);
                BufferedImage hover = padIcon(icon.getImage(2), ICON_SIZE);

                IButton btn = new IButton(up, down, hover) {
                    @Override
                    public void click() {
                        if (onIconSelected != null) {
                            onIconSelected.accept(icon);
                        }
                        pendingClose = true;
                    }

                    @Override
                    public Object tooltip(Coord c, Widget prev) {
                        return Text.render(icon.getName()).tex();
                    }
                };
                btn.resize(new Coord(ICON_SIZE, ICON_SIZE));
                contentPanel.add(btn, new Coord(GRID_PADDING + col * ICON_SIZE, gridStartY + row * ICON_SIZE));
            }

            int rows = (int) Math.ceil(icons.size() / (double) COLS);
            y = gridStartY + rows * ICON_SIZE + UI.scale(18);
        }

        int contentHeight = y + UI.scale(8);
        contentPanel.resize(new Coord(contentWidth, contentHeight));

        int maxGridHeight = sz.y - UI.scale(60);

        if (contentHeight > maxGridHeight) {
            Scrollport scroll = new Scrollport(new Coord(contentWidth, maxGridHeight));
            scroll.cont.add(contentPanel, Coord.z);
            add(scroll, Coord.z);
            add(new Button(UI.scale(120), "Close", this::close),
                    new Coord((contentWidth - UI.scale(120)) / 2, maxGridHeight + UI.scale(16)));
        } else {
            add(contentPanel, Coord.z);
            add(new Button(UI.scale(120), "Close", this::close),
                    new Coord((contentWidth - UI.scale(120)) / 2, contentHeight + UI.scale(16)));
        }

        pack();
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

    private static BufferedImage padIcon(BufferedImage img, int size) {
        if (img == null) {
            return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        }
        if (img.getWidth() == size && img.getHeight() == size) {
            return img;
        }
        BufferedImage padded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int x = (size - img.getWidth()) / 2;
        int y = (size - img.getHeight()) / 2;
        padded.getGraphics().drawImage(img, x, y, null);
        return padded;
    }
}
