/* Preprocessed source code */
package haven.res.ui.stackinv;

import haven.*;
import java.util.*;
import static haven.Inventory.*;

/* >wdg: ItemStack */
@haven.FromResource(name = "ui/stackinv", version = 2)
public class ItemStack extends Widget implements DTarget {
    public final List<GItem> order = new ArrayList<>();
    public final Map<GItem, WItem> wmap = new HashMap<>();
    private boolean dirty;

    public static ItemStack mkwidget(UI ui, Object[] args) {
	return(new ItemStack());
    }

    public void tick(double dt) {
	super.tick(dt);
	if(dirty) {
	    int x = 0, y = 0;
	    for(GItem item : order) {
		WItem w = wmap.get(item);
		w.move(Coord.of(x, 0));
		x += w.sz.x;
		y = Math.max(y, w.sz.y);
	    }
	    resize(x, y);
	    dirty = false;
	}
    }

    public void addchild(Widget child, Object... args) {
	add(child);
	if(child instanceof GItem) {
	    GItem i = (GItem)child;
	    wmap.put(i, add(new WItem(i)));
	    order.add(i);
	    dirty = true;
	}
    }

    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    wmap.remove(i).reqdestroy();
	    order.remove(i);
	    dirty = true;
	}
    }

    public void cresize(Widget ch) {
	dirty = true;
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	if(ui.modshift) {
	    Inventory minv = getparent(GameUI.class).maininv;
	    if(ev.a < 0)
		wdgmsg("invxf", minv.wdgid(), 1);
	    else if(ev.a > 0)
		minv.wdgmsg("invxf", this.wdgid(), 1);
	}
	return(true);
    }
    
    public boolean drop(Coord cc, Coord ul) {
	wdgmsg("drop");
	return(true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }
}
