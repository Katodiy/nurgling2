package nurgling.widgets;

import haven.*;

import java.util.ArrayList;
import java.util.List;
public abstract class SearchableDropbox<T> extends Dropbox<T> {
    public SearchableDropbox(int w, int listh, int itemh) {
        super(w, listh, itemh);
    }

    private class SearchableDroplist extends Listbox<T> {
        private UI.Grab grab = null;
        private TextEntry filter;
        private List<T> fullitems = new ArrayList<>(); // Stores all items
        private List<T> filtereditems = new ArrayList<>(); // Stores filtered items

        private SearchableDroplist() {
            // Call super() with 'listh' as a temporary value for 'h'
            super(SearchableDropbox.this.sz.x, listh, SearchableDropbox.this.itemh);
            sel = SearchableDropbox.this.sel;

            // Initialize fullitems
            for (int i = 0; i < SearchableDropbox.this.listitems(); i++) {
                fullitems.add(SearchableDropbox.this.listitem(i));
            }
            // Initially, all items are displayed
            filtereditems.addAll(fullitems);

            // Adjust 'h' based on the actual number of items
            this.h = Math.min(listh, filtereditems.size());

            // Adjust the size of the Listbox
            this.sz = new Coord(sz.x, h * itemh);

            // Initialize filter TextEntry
            filter = new TextEntry(sz.x, "") {
                @Override
                public boolean keydown(java.awt.event.KeyEvent ev) {
                    boolean ret = super.keydown(ev);
                    refilter();
                    return ret;
                }
            };

            // Adjust the size to account for the filter's height
            this.sz = this.sz.add(0, filter.sz.y);

            // Adjust scrollbar
            sb.resize(sz.y - filter.sz.y);
            sb.c = new Coord(sz.x - sb.sz.x, filter.sz.y);
            sb.max = filtereditems.size() - h;

            // Add the Listbox to the UI
            SearchableDropbox.this.ui.root.add(this, SearchableDropbox.this.rootpos().add(0, SearchableDropbox.this.sz.y));
            grab = ui.grabmouse(this);
            display();
        }

        private void refilter() {
            String text = filter.text().toLowerCase();
            filtereditems.clear();
            for (T item : fullitems) {
                if (item.toString().toLowerCase().contains(text)) {
                    filtereditems.add(item);
                }
            }
            // Adjust 'h' based on the new filtered items
            this.h = Math.min(listh, filtereditems.size());
            // Adjust the size of the Listbox
            this.sz = new Coord(sz.x, h * itemh + filter.sz.y);
            // Adjust scrollbar
            sb.max = Math.max(filtereditems.size() - h, 0);
            sb.val = Math.min(sb.val, sb.max);
        }

        @Override
        protected T listitem(int i) {
            if (i >= 0 && i < filtereditems.size()) {
                return filtereditems.get(i);
            }
            return null;
        }

        @Override
        protected int listitems() {
            return filtereditems.size();
        }

        @Override
        protected void drawitem(GOut g, T item, int idx) {
            SearchableDropbox.this.drawitem(g, item, idx);
        }

        @Override
        public void draw(GOut g) {
            int y = 0;
            // Draw the filter TextEntry
            filter.draw(g.reclip(new Coord(0, y), new Coord(sz.x, filter.sz.y)));
            y += filter.sz.y;
            // Adjust the clipping region for the list items
            GOut gItems = g.reclip(new Coord(0, y), sz.sub(0, y));
            super.draw(gItems);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (filter.c.isect(c, filter.sz)) {
                return filter.mousedown(c.sub(filter.c), button);
            }
            return super.mousedown(c.sub(0, filter.sz.y), button);
        }

        @Override
        public boolean mousewheel(Coord c, int amount) {
            return super.mousewheel(c.sub(0, filter.sz.y), amount);
        }

        public void destroy() {
            grab.remove();
            super.destroy();
        }

        public void change(T item) {
            SearchableDropbox.this.change(item);
            reqdestroy();
        }
    }

    // We'll override methods and inner classes in the next steps

}
