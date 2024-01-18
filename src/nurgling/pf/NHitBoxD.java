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
    public Coord2d[] checkPoints; //= new Coord2d[0];
    boolean ortho = false;
    boolean primitive = false;


    public NHitBoxD(Coord rc)
    {
        primitive = true;
        this.move(rc);
    }

    public NHitBoxD(Coord2d ul) {
        primitive = true;
        this.move(ul);
    }

    public NHitBoxD(Coord2d ul, Coord2d br) {
        this.setOrtho(ul, br, Coord2d.of(0), false);
        setUpCheckpoints();
    }

    public NHitBoxD(Coord ul, Coord br) {
        this(Utils.pfGridToWorld(ul), Utils.pfGridToWorld(br.add(1,1)));
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r) {
        this.setOrtho(ul, br, r, false);
        setUpCheckpoints();
    }

    public NHitBoxD(Gob gob) {
        this(gob.ngob.hitBox.begin, gob.ngob.hitBox.end, gob.rc, gob.a);
    }
    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r, double angle)
    {
        if (Math.abs(((4 * angle) / Math.PI) % 2.0) > 0.0001 || (ul.x != -br.x) || (ul.y != -br.y))
        {
            this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
            this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
            reCalc_n(r, angle);
        } else {

            if (Math.abs(((2 * angle) / Math.PI) % 2.0 - 1) < 0.0001) {
                this.a = Math.PI / 2;
                this.setOrtho(ul, br, r, true);
            } else {
                this.setOrtho(ul, br, r, false);
            }
        }
        setUpCheckpoints();
    }

    public static NHitBoxD shaftBoxObjectFactory(Coord2d begin, Coord2d end, double halfWidth) {
        double halfLength = begin.dist(end) / 2;
        return new NHitBoxD(
                Coord2d.of(-halfLength, -halfWidth),
                Coord2d.of(halfLength, halfWidth),
                begin.add(end).div(2),
                end.angle(begin)
        );
    }

    public void move(Coord newRC) {
        if (primitive) {
            this.setOrtho(MCache.tileqsz.sub(MCache.tilehsz), MCache.tileqsz, Utils.pfGridToWorld(newRC), false);
            setUpCheckpoints();
        }
    }

    public void move(Coord2d newUL) {
        if (primitive) {
            this.setOrtho(newUL, newUL.add(MCache.tilehsz), Coord2d.of(0), false);
            setUpCheckpoints();
        }
    }

    public void setOrtho(Coord2d ul, Coord2d br, Coord2d r, boolean halfPi) {
        rc = Coord2d.of(r.x, r.y);
        if (halfPi) {
            this.ul = Coord2d.of(Math.min(ul.y, br.y), Math.min(ul.x, br.x));
            this.br = Coord2d.of(Math.max(ul.y, br.y), Math.max(ul.x, br.x));
        } else {
            this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
            this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));
        }
        c[0] = this.ul.add(rc);
        c[1] = Coord2d.of(this.br.x, this.ul.y).add(rc);
        c[2] = this.br.add(rc);
        c[3] = Coord2d.of(this.ul.x, this.br.y).add(rc);
        ortho = true;
    }

    public void setUpCheckpoints() {
        if (primitive) {
            if (checkPoints == null) {
                checkPoints = new Coord2d[1];
            }
            checkPoints[0] = this.rc;
            return;
        }

        double xRange = br.x - ul.x;
        double yRange = br.y - ul.y;
        Coord2d startMax1, endMax1, axisMax1;
        Coord2d startMax2, endMax2, axisMax2;
        Coord2d startMin1, endMin1, axisMin1;
        Coord2d startMin2, endMin2, axisMin2;
        startMax1 = c[0];
        startMin1 = c[0];
        endMax2 = c[2];
        endMin2 = c[2];
        if (xRange > yRange) {
            startMin2 = c[1];
            endMin1 = c[3];
            startMax2 = c[3];
            endMax1 = c[1];
        } else {
            startMin2 = c[3];
            endMin1 = c[1];
            startMax2 = c[1];
            endMax1 = c[3];
        }
        axisMax1 = endMax1.sub(startMax1);
        axisMax2 = endMax2.sub(startMax2);
        axisMin1 = endMin1.sub(startMin1);
        axisMin2 = endMin2.sub(startMin2);
        int amountMax = (int) Math.ceil(Math.max(xRange, yRange) / MCache.tilehsz.x);
        int amountMin = (int) Math.ceil(Math.min(xRange, yRange) / MCache.tilehsz.x);
        checkPoints = new Coord2d[amountMax * 2 + amountMin * 2];
        for (int i = 0; i < amountMax; i++) {
            checkPoints[i] = startMax1.add(axisMax1.mul((i + 1) / (double) (amountMax + 1)));
            checkPoints[amountMax + i] = startMax2.add(axisMax2.mul((i + 1) / (double) (amountMax + 1)));
        }
        for (int i = 0; i < amountMin; i++) {
            checkPoints[amountMax * 2 + i] = startMin1.add(axisMin1.mul((i + 1) / (double) (amountMin + 1)));
            checkPoints[amountMax * 2 + amountMin + i] = startMin2.add(axisMin2.mul((i + 1) / (double) (amountMin + 1)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NHitBoxD)) {
            return (false);
        }
        NHitBoxD a = (NHitBoxD) o;
        return (a.ul.equals(ul) && a.br.equals(br));
    }

    public int hashCode() {
        int X = ul.hashCode() / 2;
        int Y = br.hashCode() / 2;
        return X + Y;
    }

    public int compareTo(NHitBoxD c) {
        return (br == c.br) ? ul.compareTo(c.ul) : br.compareTo(c.br);
    }

    //functionality
    public boolean reCalc_n(Coord2d NewShift, double newAngle) {
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

            for (int ind = 0; ind < 4; ind++) {
                d[ind] = n[ind].dot(c[ind]);
            }
            return true;
        }
        //  return false;
    }

    public Coord2d projectCenter( Coord2d direction){
        double tau =  Float.MAX_VALUE;
        for(int k = 0; k<4 ;k++) {
            if (Math.abs(direction.dot(this.n[k])) > 0.0001 ) {
                double tauTemp = Math.min(tau, (d[k] - rc.dot(n[k])) / direction.dot(n[k]));
                if (tauTemp>0)
                    tau = tauTemp;
            }
        }
        return rc.add(direction.mul(tau));
    }

    public Coord2d getCircumscribedUL() {
        double mx = Float.MAX_VALUE;
        double my = Float.MAX_VALUE;
        for (int ind = 0; ind < 4; ind++) {
            if (c[ind].x < mx) {
                mx = c[ind].x;
            }
            if (c[ind].y < my) {
                my = c[ind].y;
            }
        }
        return new Coord2d(mx, my);
    }

    public Coord2d getCircumscribedBR() {
        double mx = -Float.MAX_VALUE;
        double my = -Float.MAX_VALUE;
        for (int ind = 0; ind < 4; ind++) {
            if (c[ind].x > mx) {
                mx = c[ind].x;
            }
            if (c[ind].y > my) {
                my = c[ind].y;
            }
        }
        return new Coord2d(mx, my);
    }

    public double diag() {
        return br.sub(ul).abs();
    }

    public Coord2d sz() {
        return br.sub(ul);
    }

    public boolean contains(Coord2d c) {
        if (!ortho) {
            return (n[0].dot(c) >= d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) >= d[3]);
        } else {
            return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean containsGreedy(Coord2d c) {
        if (!ortho) {
            return (n[0].dot(c) >= d[0]) && (n[1].dot(c) >= d[1]) && (n[2].dot(c) >= d[2]) && (n[3].dot(c) >= d[3]);
        } else {
            return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x <= this.c[2].x) && (c.y <= this.c[2].y));
        }
    }

    public boolean containsLoosely(Coord2d c) {
        if (!ortho) {
            return (n[0].dot(c) > d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) > d[3]);
        } else {
            return ((c.x > this.c[0].x) && (c.y > this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean intersects(NHitBoxD other) {
        for (int k = 0; k < 4; k++) {
            if (this.contains(other.c[k]) || other.contains(this.c[k])) {
                return true;
            }
        }


        for (Coord2d checkPoint : other.checkPoints) {
            if (this.contains(checkPoint)) {
                return true;
            }
        }
        for (Coord2d checkPoint : this.checkPoints) {
            if (other.contains(checkPoint)) {
                return true;
            }
        }
        return false;
    }

    public boolean intersectsGreedy(NHitBoxD other) {
        for (int k = 0; k < 4; k++) {
            if (this.containsGreedy(other.c[k]) || other.containsGreedy(this.c[k])) {
                return true;
            }
        }


        for (Coord2d checkPoint : other.checkPoints) {
            if (this.containsGreedy(checkPoint)) {
                return true;
            }
        }
        for (Coord2d checkPoint : this.checkPoints) {
            if (other.containsGreedy(checkPoint)) {
                return true;
            }
        }
        return false;
    }

    public boolean intersectsLoosely(NHitBoxD other) {
        for (int k = 0; k < 4; k++) {
            if (this.containsLoosely(other.c[k]) || other.containsLoosely(this.c[k])) {
                return true;
            }
        }


        for (Coord2d checkPoint : other.checkPoints) {
            if (this.containsLoosely(checkPoint)) {
                return true;
            }
        }
        for (Coord2d checkPoint : this.checkPoints) {
            if (other.containsLoosely(checkPoint)) {
                return true;
            }
        }
        return false;
    }
}
