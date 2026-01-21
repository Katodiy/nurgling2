package nurgling.widgets;

import haven.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A dropdown widget for selecting custom icons.
 * Shows a preview of the selected icon and allows selecting from saved icons.
 */
public class CustomIconDropdown extends Widget {

    private static final int ICON_SIZE = UI.scale(32);
    private static final int DROPDOWN_WIDTH = UI.scale(200);
    private static final int ROW_HEIGHT = UI.scale(36);

    private String selectedIconId = null;
    private Tex selectedIconTex = null;
    private boolean dropdownOpen = false;

    private final Consumer<String> onIconSelected;
    private DropdownList dropdownList = null;

    public CustomIconDropdown(Consumer<String> onIconSelected) {
        super(new Coord(DROPDOWN_WIDTH, ICON_SIZE + UI.scale(4)));
        this.onIconSelected = onIconSelected;
        updateSelectedIconTexture();
    }

    public void setSelectedIconId(String iconId) {
        this.selectedIconId = iconId;
        updateSelectedIconTexture();
    }

    public String getSelectedIconId() {
        return selectedIconId;
    }

    private void updateSelectedIconTexture() {
        if (selectedIconId != null) {
            CustomIcon icon = CustomIconManager.getInstance().getIcon(selectedIconId);
            if (icon != null) {
                BufferedImage img = icon.getPreviewImage();
                if (img != null) {
                    selectedIconTex = new TexI(img);
                    return;
                }
            }
        }
        selectedIconTex = null;
    }

    @Override
    public void draw(GOut g) {
        // Draw background
        g.chcolor(40, 40, 40, 200);
        g.frect(Coord.z, sz);
        g.chcolor();

        // Draw border
        g.chcolor(100, 100, 100, 255);
        g.rect(Coord.z, sz);
        g.chcolor();

        // Draw selected icon or "None" text
        int iconX = UI.scale(4);
        int iconY = (sz.y - ICON_SIZE) / 2;

        if (selectedIconTex != null) {
            g.image(selectedIconTex, new Coord(iconX, iconY), new Coord(ICON_SIZE, ICON_SIZE));
        } else {
            g.chcolor(80, 80, 80, 255);
            g.frect(new Coord(iconX, iconY), new Coord(ICON_SIZE, ICON_SIZE));
            g.chcolor();
        }

        // Draw icon name or "None"
        int textX = iconX + ICON_SIZE + UI.scale(8);
        int textY = (sz.y - UI.scale(12)) / 2;
        String displayName = "None (Default)";
        if (selectedIconId != null) {
            CustomIcon icon = CustomIconManager.getInstance().getIcon(selectedIconId);
            if (icon != null) {
                displayName = icon.getName();
            }
        }
        g.text(displayName, new Coord(textX, textY));

        // Draw dropdown arrow
        int arrowX = sz.x - UI.scale(16);
        int arrowY = sz.y / 2 - UI.scale(4);
        g.chcolor(200, 200, 200, 255);
        g.text(dropdownOpen ? "\u25B2" : "\u25BC", new Coord(arrowX, arrowY));
        g.chcolor();
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (ev.b == 1) {
            toggleDropdown();
            return true;
        }
        return super.mousedown(ev);
    }

    private void toggleDropdown() {
        if (dropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }

    private void openDropdown() {
        if (dropdownList != null) {
            dropdownList.destroy();
        }

        dropdownOpen = true;
        dropdownList = new DropdownList();

        // Position dropdown below this widget
        Coord dropPos = rootpos().add(0, sz.y);
        ui.root.add(dropdownList, dropPos);
    }

    private void closeDropdown() {
        dropdownOpen = false;
        if (dropdownList != null) {
            dropdownList.destroy();
            dropdownList = null;
        }
    }

    private void selectIcon(String iconId) {
        selectedIconId = iconId;
        updateSelectedIconTexture();
        closeDropdown();
        if (onIconSelected != null) {
            onIconSelected.accept(iconId);
        }
    }

    private class DropdownList extends Widget {
        private final List<IconOption> options = new ArrayList<>();
        private int hoveredIndex = -1;

        DropdownList() {
            // Build list of options
            options.add(new IconOption(null, "None (Default)", null));

            for (CustomIcon icon : CustomIconManager.getInstance().getIconList()) {
                BufferedImage img = icon.getPreviewImage();
                Tex tex = img != null ? new TexI(img) : null;
                options.add(new IconOption(icon.getId(), icon.getName(), tex));
            }

            int height = options.size() * ROW_HEIGHT;
            int maxHeight = UI.scale(200);
            this.sz = new Coord(DROPDOWN_WIDTH, Math.min(height, maxHeight));
        }

        @Override
        public void draw(GOut g) {
            // Draw background
            g.chcolor(30, 30, 30, 240);
            g.frect(Coord.z, sz);
            g.chcolor();

            // Draw border
            g.chcolor(100, 100, 100, 255);
            g.rect(Coord.z, sz);
            g.chcolor();

            // Draw options
            int y = 0;
            for (int i = 0; i < options.size(); i++) {
                IconOption option = options.get(i);

                // Highlight if hovered
                if (i == hoveredIndex) {
                    g.chcolor(60, 60, 100, 200);
                    g.frect(new Coord(1, y), new Coord(sz.x - 2, ROW_HEIGHT));
                    g.chcolor();
                }

                // Highlight if selected
                boolean isSelected = (selectedIconId == null && option.id == null) ||
                                     (selectedIconId != null && selectedIconId.equals(option.id));
                if (isSelected) {
                    g.chcolor(80, 100, 80, 150);
                    g.frect(new Coord(1, y), new Coord(sz.x - 2, ROW_HEIGHT));
                    g.chcolor();
                }

                // Draw icon
                int iconX = UI.scale(4);
                int iconY = y + (ROW_HEIGHT - ICON_SIZE) / 2;
                if (option.tex != null) {
                    g.image(option.tex, new Coord(iconX, iconY), new Coord(ICON_SIZE, ICON_SIZE));
                } else {
                    g.chcolor(60, 60, 60, 255);
                    g.frect(new Coord(iconX, iconY), new Coord(ICON_SIZE, ICON_SIZE));
                    g.chcolor();
                }

                // Draw name
                int textX = iconX + ICON_SIZE + UI.scale(8);
                int textY = y + (ROW_HEIGHT - UI.scale(12)) / 2;
                g.text(option.name, new Coord(textX, textY));

                y += ROW_HEIGHT;
            }
        }

        @Override
        public void mousemove(MouseMoveEvent ev) {
            hoveredIndex = ev.c.y / ROW_HEIGHT;
            if (hoveredIndex >= options.size()) {
                hoveredIndex = -1;
            }
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                int index = ev.c.y / ROW_HEIGHT;
                if (index >= 0 && index < options.size()) {
                    selectIcon(options.get(index).id);
                }
                return true;
            }
            return super.mousedown(ev);
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            return true;
        }

        @Override
        public void destroy() {
            super.destroy();
            dropdownList = null;
            dropdownOpen = false;
        }
    }

    private static class IconOption {
        final String id;
        final String name;
        final Tex tex;

        IconOption(String id, String name, Tex tex) {
            this.id = id;
            this.name = name;
            this.tex = tex;
        }
    }
}
