package nurgling.pf;

import haven.*;

public class AreaD implements Comparable<AreaD>, java.io.Serializable
{
// ul  0
//     _____
//   3|    |1
//    |____|
//       2   br

    public Coord2d ul, br;
    public Coord2d rc = Coord2d.of(0);
    public double a = 0;
    double sn, cs;
    public static Coord2d[] n = {Coord2d.of(0, 1), Coord2d.of(-1, 0), Coord2d.of(0, -1), Coord2d.of(1, 0)};
    public double[] d = {0, 0, 0, 0};
    public Coord2d[] c = new Coord2d[4];

    public AreaD(Coord2d ul, Coord2d br)
    {
        this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
        this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
        reCalc_n(Coord2d.of(0), 0);
    }

    public AreaD(Coord2d ul, Coord2d br, Coord2d r)
    {
        this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
        this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
        reCalc_n(r, 0);
    }

    public AreaD(Coord2d ul, Coord2d br, Coord2d r, double angle)
    {
        this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
        this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
        reCalc_n(r, angle);
    }



    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof AreaD))
        {
            return (false);
        }
        AreaD a = (AreaD) o;
        return (a.ul.equals(ul) && a.br.equals(br));
    }

    public int hashCode()
    {
        int X = ul.hashCode() / 2;
        int Y = br.hashCode() / 2;
        return X + Y;
    }

    public int compareTo(AreaD c)
    {
        return (br == c.br) ? ul.compareTo(c.ul) : br.compareTo(c.br);
    }

    //functionality
    public boolean reCalc_n(Coord2d NewShift, double newAngle)
    {
        if ((a != newAngle) || (NewShift != rc))
        {
            a = newAngle;
            sn = Math.sin(a);
            cs = Math.cos(a);

            n[0].x = -sn;
            n[0].y = cs;

            n[1].x = -cs;
            n[1].y = -sn;

            n[2].x = sn;
            n[2].y = -cs;

            n[3].x = cs;
            n[3].y = sn;

            rc.x = NewShift.x;
            rc.y = NewShift.y;

            c[0] = ul.rot(a).add(NewShift);
            c[1] = Coord2d.of(br.x, ul.y).rot(a).add(NewShift);
            c[2] = br.rot(a).add(NewShift);
            c[3] = Coord2d.of(ul.x, br.y).rot(a).add(NewShift);

            for (int ind = 0; ind < 4; ind++)
            {
                d[ind] = n[ind].dot(c[ind]);
            }
            return true;
        }
        return false;
    }

    public Coord2d getCircumscribedUL()
    {
        double mx = Float.MAX_VALUE;
        double my = Float.MAX_VALUE;
        for (int ind = 0; ind < 4; ind++)
        {
            if (c[ind].x < mx)
            {
                mx = c[ind].x;
            }
            if (c[ind].y < my)
            {
                my = c[ind].y;
            }
        }
        return new Coord2d(mx, my);
    }

    public Coord2d getCircumscribedBR()
    {
        double mx = -Float.MAX_VALUE;
        double my = -Float.MAX_VALUE;
        for (int ind = 0; ind < 4; ind++)
        {
            if (c[ind].x > mx)
            {
                mx = c[ind].x;
            }
            if (c[ind].y > my)
            {
                my = c[ind].y;
            }
        }
        return new Coord2d(mx, my);
    }

    public double diag()
    {
        return br.sub(ul).abs();
    }

    public Coord2d sz()
    {
        return br.sub(ul);
    }

    public boolean contains(Coord2d c)
    {
        return (n[0].dot(c) >= d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) >= d[3]);
    }

    public boolean containsGreedy(Coord2d c)
    {
        return (n[0].dot(c) >= d[0]) && (n[1].dot(c) >= d[1]) && (n[2].dot(c) >= d[2]) && (n[3].dot(c) >= d[3]);
    }

    public boolean containsLoosely(Coord2d c)
    {
        return (n[0].dot(c) > d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) > d[3]);
    }
}
