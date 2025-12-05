package nurgling.widgets;

import haven.*;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.tools.ExploredArea;

/**
 * FlowerMenu-style popup for explored area session management.
 * Appears on right-click of the explored area toggle button.
 */
public class ExploredAreaMenu extends Widget {
    public static final Tex bl = Resource.loadtex("nurgling/hud/flower/left");
    public static final Tex bm = Resource.loadtex("nurgling/hud/flower/mid");
    public static final Tex br = Resource.loadtex("nurgling/hud/flower/right");

    public static final Tex bhl = Resource.loadtex("nurgling/hud/flower/hleft");
    public static final Tex bhm = Resource.loadtex("nurgling/hud/flower/hmid");
    public static final Tex bhr = Resource.loadtex("nurgling/hud/flower/hright");

    private final ExploredArea exploredArea;
    private UI.Grab mg;
    private UI.Grab kg;
    private Petal[] petals;
    private int len = 0;

    public ExploredAreaMenu(ExploredArea exploredArea) {
        super(Coord.z);
        z(100);  // Ensure menu renders on top of other widgets
        this.exploredArea = exploredArea;

        // Create menu items based on session state
        String[] opts;
        if (exploredArea.isSessionActive()) {
            opts = new String[]{"Delete Session Layer"};
        } else {
            opts = new String[]{"Create Session Layer"};
        }

        petals = new Petal[opts.length];
        int y = 0;

        for (int i = 0; i < opts.length; i++) {
            add(petals[i] = new Petal(opts[i], i + 1), new Coord(0, y));
            petals[i].num = i;
            y += bl.sz().y + UI.scale(2);
            len = Math.max(petals[i].sz.x, len);
        }
        for (int i = 0; i < opts.length; i++) {
            petals[i].resize(len, bl.sz().y);
        }
        // Set menu size to fit all petals
        resize(len, y);
    }

    @Override
    protected void added() {
        if (c.equals(-1, -1))
            c = parent.ui.lcc;
        mg = ui.grabmouse(this);
        kg = ui.grabkeys(this);
    }

    @Override
    public void destroy() {
        if (mg != null) mg.remove();
        if (kg != null) kg.remove();
        super.destroy();
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (!ev.propagate(this)) {
            // Click outside - close menu
            close();
        }
        return true;
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
        char key = ev.c;
        if ((key >= '0') && (key <= '9')) {
            int opt = (key == '0') ? 10 : (key - '1');
            if (opt < petals.length) {
                choose(petals[opt]);
            }
            return true;
        } else if (key_esc.match(ev)) {
            close();
            return true;
        }
        return false;
    }

    private void choose(Petal petal) {
        if (petal.name.equals("Create Session Layer")) {
            exploredArea.startSession();
        } else if (petal.name.equals("Delete Session Layer")) {
            exploredArea.endSession();
        }
        close();
    }

    private void close() {
        ui.destroy(this);
    }

    public class Petal extends Widget {
        public String name;
        public int num;
        private Text text;
        private Text textnum;
        private boolean isHighlighted = false;

        public Petal(String name, int num) {
            super(Coord.z);
            this.name = name;
            this.num = num;
            text = NStyle.flower.render(name);
            textnum = NStyle.flower.render(String.valueOf(num));
            resize(text.sz().x + bl.sz().x + br.sz().x + UI.scale(30), FlowerMenu.ph);
        }

        @Override
        public void draw(GOut g) {
            g.image(isHighlighted ? bhl : bl, new Coord(0, 0));

            Coord pos = new Coord(0, 0);
            for (pos.x = bl.sz().x; pos.x + bm.sz().x <= len - br.sz().x; pos.x += bm.sz().x) {
                g.image(isHighlighted ? bhm : bm, pos);
            }
            g.image(isHighlighted ? bhm : bm, pos, new Coord(sz.x - pos.x - br.sz().x, br.sz().y));
            g.image(textnum.tex(), new Coord(bl.sz().x / 2 - textnum.tex().sz().x / 2 - UI.scale(1), br.sz().y / 2 - textnum.tex().sz().y / 2));
            g.image(text.tex(), new Coord(br.sz().x + bl.sz().x + UI.scale(10), br.sz().y / 2 - text.tex().sz().y / 2));
            g.image(isHighlighted ? bhr : br, new Coord(len - br.sz().x, 0));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            choose(this);
            return true;
        }

        @Override
        public void mousemove(MouseMoveEvent ev) {
            isHighlighted = ev.c.isect(Coord.z, sz);
            super.mousemove(ev);
        }
    }
}
