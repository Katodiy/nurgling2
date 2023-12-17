package nurgling.widgets;


import haven.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NTabStrip<T> extends Widget {
    public static final IBox frame = new IBox("nurgling/hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    private final List<Button<T>> buttons = new ArrayList<Button<T>>();
    private Button<T> selected;
    private Color selectedColor = null;

    public int getSelectedButtonIndex() {
        return buttons.indexOf(selected);
    }

    public int getButtonCount() {
        return buttons.size();
    }

    public Button<T> insert(int index, Tex image, String tooltip, MenuGrid.Pagina pag) {
        final Button<T> button = add(new Button<T>(image) {
            public void click() {
                select(this);
                pag.button().use(new MenuGrid.Interaction(1, 0));
            }
        });
        button.tooltip = tooltip;
        if(selectedColor != null) {
            button.bg = selectedColor;
        }
        buttons.add(index, button);
        updateLayout();
        return button;
    }

    public void select(T tag) {
        select(tag, false);
    }

    public void select(T tag, boolean skipSelected) {
        for(Button<T> btn : buttons) {
            if(Objects.equals(btn.tag, tag)) {
                select(btn, skipSelected);
                return;
            }
        }
    }

    public void select(int buttonIndex) {
        select(buttons.get(buttonIndex));
    }

    public void select(int buttonIndex, boolean skipSelected) {
        select(buttons.get(buttonIndex), skipSelected);
    }

    public void select(Button<T> button) {
        select(button, false);
    }

    public void select(Button<T> button, boolean skipSelected) {
        if(selected != button) {
            for (Button<T> b : buttons) {
                b.setActive(b == button);
            }
            selected = button;
        }
    }

    public Button<T> remove(int buttonIndex) {
        Button<T> button = buttons.remove(buttonIndex);
        button.destroy();
        updateLayout();
        return button;
    }

    public void remove(Button<T> button) {
        if(buttons.remove(button)) {
            button.destroy();
            updateLayout();
        }
    }

    private void updateLayout() {
        int x = 0;
        for (Button<T> button : buttons) {
            button.c = new Coord(x, 0);
            x += button.sz.x - 1;
        }

        pack();
    }

    public boolean contains(T lastPagina)
    {
        for (Button<T> button : buttons) {
            if(button.tag == lastPagina)
                return true;
        }
        return false;
    }

    public abstract static class Button<T> extends Widget {
        public static final Coord padding = new Coord(5, 2);
        public static final Text.Foundry font = new Text.Foundry(Text.serif, 14).aa(true);
        private Color bg = new Color(0, 0, 0, 128);
        private Tex image;
        private boolean active;
        public T tag;

        Button(Tex image) {
            this.image = image;
            int w = imgsz().y + padding.x * 2;
            int h = imgsz().y + padding.y * 2;
            resize(w, h);
        }

        private Coord imgsz() { return image != null ? image.sz() : Coord.z; }

        public abstract void click();

        @Override
        public void draw(GOut g) {
            if(active) {
                g.chcolor(bg);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
            frame.draw(g, Coord.z, sz);
            if(image != null) {g.image(image, padding);}
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if(button == 1) {
                click();
                return true;
            }
            return false;
        }

        void setActive(boolean value) {
            this.active = value;
        }
    }
}