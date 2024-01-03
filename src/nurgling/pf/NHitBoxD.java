package nurgling.pf;

import haven.*;

public class NHitBoxD implements Comparable<NHitBoxD>, java.io.Serializable
{
// ul  0
//     _____
//   3|    |1
//    |____|
//       2   br

    public Coord2d ul, br;
    public Coord2d rc = Coord2d.of(0);
    public double a = 0;
    double sn = 0, cs = 1;
    public static Coord2d[] n = {Coord2d.of(0, 1), Coord2d.of(-1, 0), Coord2d.of(0, -1), Coord2d.of(1, 0)};
    public double[] d = {0, 0, 0, 0};
    public Coord2d[] c = new Coord2d[4];
    boolean ortho = false;

    public NHitBoxD(Coord2d ul, Coord2d br)
    {
        this.setOrtho(ul, br, Coord2d.of(0));
    }

    public NHitBoxD(Coord ul, Coord br)
    {
        this(Coord2d.of(ul),Coord2d.of(br));
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r)
    {
        this.setOrtho(ul, br, r);
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r, double angle)
    {
        if(Math.abs(((4 * angle) / Math.PI) % 2.0) > 0.0001)
        {
            this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
            this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
            reCalc_n(r, angle);
        }
        else
        {
            this.setOrtho(ul, br, r);
        }
    }

    public void setOrtho(Coord2d ul, Coord2d br, Coord2d r)
    {
        this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
        this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
        rc = Coord2d.of(r.x, r.y);
        c[0] = this.ul.add(r);
        c[1] = Coord2d.of(this.br.x, this.ul.y).add(r);
        c[2] = this.br.add(r);
        c[3] = Coord2d.of(this.ul.x, this.br.y).add(r);
        ortho = true;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof NHitBoxD))
        {
            return (false);
        }
        NHitBoxD a = (NHitBoxD) o;
        return (a.ul.equals(ul) && a.br.equals(br));
    }

    public int hashCode()
    {
        int X = ul.hashCode() / 2;
        int Y = br.hashCode() / 2;
        return X + Y;
    }

    public int compareTo(NHitBoxD c)
    {
        return (br == c.br) ? ul.compareTo(c.ul) : br.compareTo(c.br);
    }

    //functionality
    public boolean reCalc_n(Coord2d NewShift, double newAngle)
    {
     //   if ((a != newAngle) || (NewShift != rc))
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
      //  return false;
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
        if (!ortho)
        {
            return (n[0].dot(c) >= d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) >= d[3]);
        }
        else
        {
            return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean containsGreedy(Coord2d c)
    {
        if (!ortho)
        {
            return (n[0].dot(c) >= d[0]) && (n[1].dot(c) >= d[1]) && (n[2].dot(c) >= d[2]) && (n[3].dot(c) >= d[3]);
        }
        else
        {
            return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x <= this.c[2].x) && (c.y <= this.c[2].y));
        }
    }

    public boolean containsLoosely(Coord2d c)
    {
        if (!ortho)
        {
            return (n[0].dot(c) > d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) > d[3]);
        }
        else
        {
            return ((c.x > this.c[0].x) && (c.y > this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean intersects(NHitBoxD other)
    {
        for (int k = 0; k < 4; k++)
            if (this.contains(other.c[k]) || other.contains(this.c[k]))
                return true;
        return false;
    }

    public boolean intersectsGreedy(NHitBoxD other)
    {
        for (int k = 0; k < 4; k++)
            if (this.containsGreedy(other.c[k]) || other.containsGreedy(this.c[k]))
                return true;
        return false;
    }

    public boolean intersectsLoosely(NHitBoxD other)
    {
        for (int k = 0; k < 4; k++)
            if (this.containsLoosely(other.c[k]) || other.containsLoosely(this.c[k]))
                return true;
        return false;
    }
}
