/* Preprocessed source code */
/* $use: lib/vertspr */

package haven.res.gfx.fx.rain;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.res.lib.vertspr.*;
import java.util.*;
import java.nio.*;
import haven.render.VertexArray.Layout;
import static haven.render.sl.Cons.*;
import static haven.Utils.sb;

/* XXX: Remove me as soon as custom clients can be expected to have
 * merged the fixes from mainline. */
@haven.FromResource(name = "gfx/fx/rain", version = 2)
public class FastArrayList2 <E> extends AbstractList<E> {
    private Object[] bk = null;
    private int n = 0;

    public FastArrayList2() {
    }

    public FastArrayList2(int sz) {
	bk = new Object[sz];
    }

    private Object[] ensure(int sz) {
	if((bk == null) || (bk.length < sz)) {
	    int ns = (bk == null) ? 8 : bk.length;
	    while(ns < sz)
		ns <<= 1;
	    bk = (bk == null) ? new Object[ns] : Arrays.copyOf(bk, ns);
	}
	return(bk);
    }

    public int size() {
	return(n);
    }

    @SuppressWarnings("unchecked")
    public E get(int i) {
	if(i >= n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	return((E)bk[i]);
    }

    public boolean add(E e) {
	ensure(n + 1)[n++] = e;
	return(true);
    }

    @SuppressWarnings("unchecked")
    public E set(int i, E el) {
	if(i >= n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	E ret = (E)bk[i];
	bk[i] = el;
	return(ret);
    }

    public void add(int i, E el) {
	if(i > n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	ensure(n + 1)[n++] = bk[i];
	bk[i] = el;
    }

    @SuppressWarnings("unchecked")
    public E remove(int i) {
	if(i >= n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	E ret = (E)bk[i];
	n--;
	bk[i] = bk[n];
	bk[n] = null;
	return(ret);
    }

    public void clear() {
	bk = null;
	n = 0;
    }

    public Iterator<E> iterator() {
	return(listIterator());
    }

    public ListIterator<E> listIterator() {
	return(listIterator(0));
    }

    public ListIterator<E> listIterator(int start) {
	return(new ListIterator<E>() {
		private int cur = start, last = -1;

		public boolean hasPrevious() {return(cur > 0);}
		public boolean hasNext() {return(cur < n);}
		public int nextIndex() {return(cur);}
		public int previousIndex() {return(cur - 1);}

		@SuppressWarnings("unchecked")
		public E previous() {
		    if((cur <= 0) || (cur > n))
			throw(new NoSuchElementException());
		    return((E)bk[last = --cur]);
		}

		@SuppressWarnings("unchecked")
		public E next() {
		    if((cur < 0) || (cur >= n))
			throw(new NoSuchElementException());
		    return((E)bk[last = cur++]);
		}

		public void remove() {
		    if(last < 0)
			throw(new IllegalStateException());
		    FastArrayList2.this.remove(last);
		    if(last < cur)
			cur--;
		    last = -1;
		}

		public void set(E e) {
		    if(last < 0)
			throw(new IllegalStateException());
		    FastArrayList2.this.set(cur, e);
		}

		public void add(E e) {
		    FastArrayList2.this.add(last = cur++, e);
		}
	    });
    }
}

/* >wtr: Rain */
