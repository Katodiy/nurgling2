package haven;

import java.awt.Color;

public abstract class Listbox<T> extends ListWidget<T> {
    public int h;
    public final Scrollbar sb;

    public Listbox(int w, int h, int itemh) {
        super(new Coord(w, h * itemh), itemh);
        this.h = h;
        this.sb = adda(new Scrollbar(sz.y, 0, 0), sz.x, 0, 1, 0);
    }

    protected void drawsel(GOut g) {
        g.chcolor(255, 255, 0, 128);
        g.frect(Coord.z, g.sz());
        g.chcolor();
    }

    protected void drawbg(GOut g) {
        g.chcolor(Color.BLACK);
        g.frect(Coord.z, sz);
        g.chcolor();
    }

    public void draw(GOut g) {
        sb.max = listitems() - h;
        drawbg(g);
        int n = listitems();
        for(int i = 0; (i * itemh) < sz.y; i++) {
            int idx = i + sb.val;
            if(idx >= n)
                break;
            T item = listitem(idx);
            int w = sz.x - (sb.vis()?sb.sz.x:0);
            GOut ig = g.reclip(new Coord(0, i * itemh), new Coord(w, itemh));
            if(item == sel)
                drawsel(ig);
            drawitem(ig, item, idx);
        }
        super.draw(g);
    }

    public boolean mousewheel(MouseWheelEvent ev) {
        sb.ch(ev.a);
        return(true);
    }

    protected void itemclick(T item, int button) {
        if(button == 1)
            change(item);
    }

    protected void itemclick(T item, Coord c, int button) {
        itemclick(item, button);
    }

    public Coord idxc(int idx) {
        return(new Coord(0, (idx - sb.val) * itemh));
    }

    public int idxat(Coord c) {
        return((c.y / itemh) + sb.val);
    }

    public T itemat(Coord c) {
        int idx = idxat(c);
        if(idx >= listitems())
            return(null);
        return(listitem(idx));
    }

    public boolean mousedown(MouseDownEvent ev) {
        if(super.mousedown(ev))
            return(true);
        int idx = idxat(ev.c);
        T item = (idx >= listitems()) ? null : listitem(idx);
        if((item == null) && (ev.b == 1))
            change(null);
        else if(item != null)
            itemclick(item, ev.c.sub(idxc(idx)), ev.b);
        return(true);
    }

    public void display(int idx) {
        if(idx < sb.val) {
            sb.val = idx;
        } else if(idx >= sb.val + h) {
            sb.val = Math.max(idx - (h - 1), 0);
        }
    }

    public void display(T item) {
        int p = find(item);
        if(p >= 0)
            display(p);
    }

    public void display() {
        display(sel);
    }

    public void resize(Coord sz) {
        super.resize(sz);
        this.h = Math.max(sz.y / itemh, 1);
        sb.resize(sz.y);
        sb.c = new Coord(sz.x - sb.sz.x, 0);
    }
}
