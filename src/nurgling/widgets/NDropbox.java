package nurgling.widgets;

import haven.*;

import java.awt.Color;

/**
 * A fixed version of Dropbox that properly handles scrollbar drag.
 * The original Dropbox uses Listbox which doesn't propagate mousedown
 * events to child widgets (like the scrollbar), preventing drag functionality.
 */
public abstract class NDropbox<T> extends ListWidget<T> {
    public static final Tex drop = Resource.loadtex("gfx/hud/drop");
    public final int listh;
    private final Coord dropc;
    private NDroplist dl;

    public NDropbox(int w, int listh, int itemh) {
        super(new Coord(w, itemh), itemh);
        this.listh = listh;
        dropc = new Coord(sz.x - drop.sz().x, 0);
    }

    private class NDroplist extends Listbox<T> {
        private UI.Grab grab = null;

        private NDroplist() {
            super(NDropbox.this.sz.x, Math.min(listh, NDropbox.this.listitems()), NDropbox.this.itemh);
            sel = NDropbox.this.sel;
            NDropbox.this.ui.root.add(this, NDropbox.this.rootpos().add(0, NDropbox.this.sz.y));
            z(1000);
            raise();
            grab = ui.grabmouse(this);
            display();
        }

        protected T listitem(int i) {return(NDropbox.this.listitem(i));}
        protected int listitems() {return(NDropbox.this.listitems());}
        protected void drawitem(GOut g, T item, int idx) {NDropbox.this.drawitem(g, item, idx);}

        public void destroy() {
            grab.remove();
            super.destroy();
            dl = null;
        }

        public void change(T item) {
            NDropbox.this.change(item);
            reqdestroy();
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            // Check if click is on scrollbar first - this is the fix
            if(sb.vis() && ev.c.isect(sb.c, sb.sz)) {
                return(sb.mousedown(ev.derive(ev.c.sub(sb.c))));
            }
            return(super.mousedown(ev));
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
            dl = new NDroplist();
            return(true);
        }
        return(true);
    }
}
