/* Preprocessed source code */
package haven.res.lib.obst;

import haven.*;
import java.util.*;

@haven.FromResource(name = "lib/obst", version = 1)
public class Obstacle {
    public static final Obstacle nil = new Obstacle(new Coord2d[0][]);
    public final Coord2d[][] p;

    public Obstacle(Coord2d[][] p) {
	this.p = p;
    }

    public static Obstacle parse(Message sdt) {
	int h = sdt.uint8();
	int t = (h >> 4) & 0xf;
	double ext = 1 << (h & 0xf);
	ext *= 11;
	Coord2d[][] p;
	switch(t) {
	case 0: {
	    p = new Coord2d[sdt.uint8()][];
	    for(int i = 0; i < p.length; i++)
		p[i] = new Coord2d[sdt.uint8()];
	    for(int i = 0; i < p.length; i++) {
		for(int o = 0; o < p[i].length; o++)
		    p[i][o] = Coord2d.of(sdt.snorm8() * ext, sdt.snorm8() * ext);
	    }
	    break;
	}
	case 1: {
	    return(nil);
	}
	case 2: {
	    p = new Coord2d[1][sdt.uint8()];
	    for(int i = 0; i < p[0].length; i++)
		p[0][i] = Coord2d.of(sdt.snorm8() * ext, sdt.snorm8() * ext);
	    break;
	}
	case 3: {
	    double l = ext * sdt.snorm8();
	    double u = ext * sdt.snorm8();
	    double r = ext * sdt.snorm8();
	    double b = ext * sdt.snorm8();
	    p = new Coord2d[][] {{
		Coord2d.of(l, u),
		Coord2d.of(r, u),
		Coord2d.of(r, b),
		Coord2d.of(l, b),
	    }};
	    break;
	}
	default:
	    throw(new Message.FormatError("unknown obstacle type: " + t));
	}
	return(new Obstacle(p));
    }

    class Vertices extends AbstractCollection<Coord2d> {
	public int size() {
	    int n = 0;
	    for(Coord2d[] f : p)
		n += f.length;
	    return(n);
	}

	public Iterator<Coord2d> iterator() {
	    return(new Iterator<Coord2d>() {
		    int f = 0, v = 0;

		    public boolean hasNext() {
			return(f < p.length);
		    }

		    public Coord2d next() {
			if(!hasNext())
			    throw(new NoSuchElementException());
			Coord2d ret = p[f][v];
			if(++v >= p[f].length) {
			    f++;
			    v = 0;
			}
			return(ret);
		    }
		});
	}
    }

    public Collection<Coord2d> verts() {
	return(new Vertices());
    }
}
