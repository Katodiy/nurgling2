package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;

import java.awt.image.BufferedImage;
import java.util.List;

import static nurgling.actions.bots.registry.BotDescriptor.BotType.*;

public class ScenarioBotSelectionDialog extends Window {
    public static final int ICON_SIZE = UI.scale(34);
    public static final int GRID_PADDING = UI.scale(10);
    public static final int COLS = 6;

    public ScenarioBotSelectionDialog(java.util.function.Consumer<BotDescriptor> onSelect) {
        super(new Coord(ICON_SIZE * COLS + GRID_PADDING * 2, UI.scale(350)), "Select Bot");

        List<BotDescriptor.BotType> groupOrder = List.of(UTILS, PRODUCTIONS, FARMING, LIVESTOCK);
        int contentWidth = ICON_SIZE * COLS + GRID_PADDING * 2;
        Widget contentPanel = new Widget(new Coord(contentWidth, 10000)); // Height will be fixed below

        int y = GRID_PADDING;
        for (BotDescriptor.BotType type : groupOrder) {
            List<BotDescriptor> group = BotRegistry.byType(type).stream()
                    .filter(b -> b.allowedAsStepInScenario)
                    .toList();
            if (group.isEmpty()) continue;

            String title;
            switch (type) {
                case PRODUCTIONS: title = "Production";  break;
                case FARMING:  title = "Farmers";   break;
                case LIVESTOCK: title = "Livestock"; break;
                case UTILS:    title = "Utils";     break;
                default:       title = "Other";
            }

            Label label = new Label(title);
            contentPanel.add(label, new Coord(GRID_PADDING, y));
            y += label.sz.y + UI.scale(6);

            int groupStartY = y;
            int i = 0;
            for (BotDescriptor bot : group) {
                int col = i % COLS;
                int row = i / COLS;

                BufferedImage up    = padIcon(Resource.loadsimg(bot.getUpIconPath()), ICON_SIZE);
                BufferedImage down  = padIcon(Resource.loadsimg(bot.getDownIconPath()), ICON_SIZE);
                BufferedImage hover = padIcon(Resource.loadsimg(bot.getHoverIconPath()), ICON_SIZE);

                IButton btn = new IButton(up, down, hover) {
                    public void click() { onSelect.accept(bot); }
                    @Override public Object tooltip(Coord c, Widget prev) {
                        return Text.render(bot.displayName).tex();
                    }
                };
                btn.resize(new Coord(ICON_SIZE, ICON_SIZE));
                contentPanel.add(btn, new Coord(GRID_PADDING + col * ICON_SIZE, groupStartY + row * ICON_SIZE));
                i++;
            }
            int groupRows = (int) Math.ceil(group.size() / (double) COLS);
            y = groupStartY + groupRows * ICON_SIZE + UI.scale(18);
        }

        int contentHeight = y + UI.scale(8);
        contentPanel.resize(new Coord(contentWidth, contentHeight));

        int maxGridHeight = sz.y - UI.scale(60);

        if (contentHeight > maxGridHeight) {
            Scrollport scroll = new Scrollport(new Coord(contentWidth, maxGridHeight));
            scroll.cont.add(contentPanel, Coord.z);
            add(scroll, Coord.z);
            add(new Button(UI.scale(120), "Cancel", this::reqdestroy),
                    new Coord((contentWidth - UI.scale(120)) / 2, maxGridHeight + UI.scale(16)));
        } else {
            add(contentPanel, Coord.z);
            add(new Button(UI.scale(120), "Cancel", this::reqdestroy),
                    new Coord((contentWidth - UI.scale(120)) / 2, contentHeight + UI.scale(16)));
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
