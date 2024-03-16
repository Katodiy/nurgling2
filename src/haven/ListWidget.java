
package haven;

public abstract class ListWidget<T> extends Widget {
    public final int itemh;
    public T sel;

    public ListWidget(Coord sz, int itemh) {
        super(sz);
        this.itemh = itemh;
    }

    protected abstract T listitem(int i);
    protected abstract int listitems();
    protected abstract void drawitem(GOut g, T item, int i);

    public int find(T item) {
        for(int i = 0; i < listitems(); i++) {
            if(listitem(i) == item)
                return(i);
        }
        return(-1);
    }

    public void change(T item) {
        this.sel = item;
    }
}
