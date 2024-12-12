package haven;

import java.awt.Color;

public abstract class Dropbox<T> extends ListWidget<T> {
    public static final Tex drop = Resource.loadtex("gfx/hud/drop");
    public final int listh;
    private final Coord dropc;
    private Droplist dl;

    public Dropbox(int w, int listh, int itemh) {
        super(new Coord(w, itemh), itemh);
        this.listh = listh;
        dropc = new Coord(sz.x - drop.sz().x, 0);
    }

    private class Droplist extends Listbox<T> {
        private UI.Grab grab = null;

        private Droplist() {
            super(Dropbox.this.sz.x, Math.min(listh, Dropbox.this.listitems()), Dropbox.this.itemh);
            sel = Dropbox.this.sel;
            Dropbox.this.ui.root.add(this, Dropbox.this.rootpos().add(0, Dropbox.this.sz.y));
            grab = ui.grabmouse(this);
            display();
        }

        protected T listitem(int i) {return(Dropbox.this.listitem(i));}
        protected int listitems() {return(Dropbox.this.listitems());}
        protected void drawitem(GOut g, T item, int idx) {Dropbox.this.drawitem(g, item, idx);}

        public boolean mousedown(Coord c, int btn) {
            if(!c.isect(Coord.z, sz)) {
                reqdestroy();
                return(true);
            }
            return(super.mousedown(c, btn));
        }

        public void destroy() {
            grab.remove();
            super.destroy();
            dl = null;
        }

        public void change(T item) {
            Dropbox.this.change(item);
            reqdestroy();
        }
    }

    public void draw(GOut g) {
        g.chcolor(Color.BLACK);
        g.frect(Coord.z, sz);
        g.chcolor();
        if(sel != null)
            drawitem(g.reclip(Coord.z, new Coord(sz.x - drop.sz().x, itemh)), sel, 0);
        g.image(drop, dropc);
        super.draw(g);
    }

    public boolean mousedown(MouseDownEvent ev) {
        if(super.mousedown(ev))
            return(true);
        if((dl == null) && (ev.b == 1)) {
            dl = new Droplist();
            return(true);
        }
        return(true);
    }
}
