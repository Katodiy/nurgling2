package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScenarioBotSelectionDialog extends Window {
    public static final int ICON_SIZE = UI.scale(34);
    public static final int GRID_PADDING = UI.scale(10);
    public static final int COLS = 6; // Number of icons per row

    public ScenarioBotSelectionDialog(java.util.function.Consumer<BotDescriptor> onSelect) {
        // Window width: enough for all icons plus equal left/right margin
        super(new Coord(ICON_SIZE * COLS + GRID_PADDING * 2, UI.scale(350)), "Select Bot");

        Collection<BotDescriptor> bots = BotRegistry.listBots();
        int y0 = GRID_PADDING;
        int x0 = GRID_PADDING;

        List<IButton> buttons = new ArrayList<>();
        int i = 0;
        for (BotDescriptor bot : bots) {
            int col = i % COLS;
            int row = i / COLS;

            String iconBase = bot.iconPath.replaceAll("/[udh]$", "");
            BufferedImage up = padIcon(Resource.loadsimg(iconBase + "/u"), ICON_SIZE);
            BufferedImage down = padIcon(Resource.loadsimg(iconBase + "/d"), ICON_SIZE);
            BufferedImage hover = padIcon(Resource.loadsimg(iconBase + "/h"), ICON_SIZE);

            IButton btn = new IButton(up, down, hover) {
                public void click() {
                    onSelect.accept(bot);
                }
                @Override
                public Object tooltip(Coord c, Widget prev) {
                    return Text.render(bot.displayName).tex();
                }
            };

            btn.resize(new Coord(ICON_SIZE, ICON_SIZE));
            add(btn, new Coord(x0 + col * ICON_SIZE, y0 + row * ICON_SIZE));
            buttons.add(btn);
            i++;
        }

        int rows = (int) Math.ceil(bots.size() / (double) COLS);
        int gridHeight = rows * ICON_SIZE + y0;
        int contentWidth = ICON_SIZE * COLS + GRID_PADDING * 2;

        int maxGridHeight = sz.y - UI.scale(60);
        if (gridHeight > maxGridHeight) {
            Scrollport scroll = new Scrollport(new Coord(contentWidth, maxGridHeight));
            for (IButton btn : buttons) {
                btn.unlink();
                scroll.cont.add(btn, btn.c);
            }
            add(scroll, Coord.z);
            add(new Button(UI.scale(120), "Cancel", this::reqdestroy), new Coord((contentWidth - UI.scale(120)) / 2, maxGridHeight + UI.scale(16)));
        } else {
            add(new Button(UI.scale(120), "Cancel", this::reqdestroy), new Coord((contentWidth - UI.scale(120)) / 2, gridHeight + UI.scale(16)));
        }

        pack();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                NUtils.getGameUI().map.glob.oc.paths.pflines = null;
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private static BufferedImage padIcon(BufferedImage img, int size) {
        if (img.getWidth() == size && img.getHeight() == size)
            return img;
        BufferedImage padded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int x = (size - img.getWidth()) / 2;
        int y = (size - img.getHeight()) / 2;
        padded.getGraphics().drawImage(img, x, y, null);
        return padded;
    }
}
