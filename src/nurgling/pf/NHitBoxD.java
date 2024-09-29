package nurgling.pf;

import haven.*;

public class NHitBoxD implements Comparable<NHitBoxD>, java.io.Serializable {
// ul  0
//     _____
//   3|    |1
//    |____|
//       2   br

    //core hitbox data
    public Coord2d ul, br;
    public Coord2d rc = Coord2d.of(Double.MAX_VALUE);
    public double angle = Double.MAX_VALUE;

    //secondary data
    double sn = 0, cs = 1;
    public static Coord2d[] n = {Coord2d.of(0, 1), Coord2d.of(-1, 0), Coord2d.of(0, -1), Coord2d.of(1, 0)};
    public double[] d = {0, 0, 0, 0};
    //corners ul=>ur=>br=>bl
    public Coord2d[] c = new Coord2d[4];
    boolean checkPointsInitiated = false;
    public Coord2d[] checkPoints;
    boolean ortho = true;
    boolean asymmetric = false;
    boolean primitive = false;

    private static boolean intersection_by_points = true;


    public NHitBoxD(Coord rc) {
        this.setUnitSquare(rc);
    }

    public NHitBoxD(Coord2d ul) {
        this.setUnitSquare(ul);

    }

    public NHitBoxD(Coord2d ul, Coord2d br) {
        //TODO empty hitbox center
        //TODO rotten log no hitbox
        this.setOrtho(ul, br, null, 0);
    }

    public NHitBoxD(Coord ul, Coord br) {
        this(Utils.pfGridToWorld(ul).sub(MCache.tileqsz), Utils.pfGridToWorld(br).add(MCache.tileqsz));
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r) {
        this.setOrtho(ul, br, r, 0);
    }

    public NHitBoxD(Gob gob) {
        this(gob.ngob.hitBox.begin, gob.ngob.hitBox.end, gob.rc, gob.a);
    }

    public NHitBoxD(Coord2d ul, Coord2d br, Coord2d r, double angle) {
        // TODO assymetric hitbox?
        double kPi = ((2 * angle) / Math.PI);
        this.ul = Coord2d.of(Math.min(ul.x, br.x), Math.min(ul.y, br.y));
        this.br = Coord2d.of(Math.max(ul.x, br.x), Math.max(ul.y, br.y));

        if (((kPi < 0) ? ((kPi % 1.0) + 1.0) : (kPi % 1.0)) > 0.0001) move(r, angle);
        else move_ortho(r, (int) Math.round(kPi));
    }

    public static NHitBoxD shaftBoxObjectFactory(Coord2d begin, Coord2d end, double halfWidth) {
        //TODO refactor
        double halfLength = begin.dist(end) / 2;
        return new NHitBoxD(Coord2d.of(-halfLength, -halfWidth), Coord2d.of(halfLength, halfWidth), begin.add(end).div(2), end.angle(begin));
    }

    public void setUnitSquare(Coord newRC) {
        primitive = true;
        this.setOrtho(MCache.tileqsz.sub(MCache.tilehsz), MCache.tileqsz, Utils.pfGridToWorld(newRC), 0);
    }

    public void setUnitSquare(Coord2d newUL) {
        primitive = true;
        this.setOrtho(newUL, newUL.add(MCache.tilehsz), null, 0);
    }

    public void setOrtho(Coord2d ul, Coord2d br, Coord2d r, int quarterTurns) {
        if (r == null)
            rc = Coord2d.of((ul.x + br.x) / 2, (ul.y + br.y) / 2);
        else {
            rc.x = r.x;
            rc.y = r.y;
        }
        this.ul = Coord2d.of(Math.min(ul.x, br.x) - ((r == null) ? rc.x : 0), Math.min(ul.y, br.y) - ((r == null) ? rc.y : 0));
        this.br = Coord2d.of(Math.max(ul.x, br.x) - ((r == null) ? rc.x : 0), Math.max(ul.y, br.y) - ((r == null) ? rc.y : 0));
        move_ortho(rc, quarterTurns);
    }

    public void move_ortho(Coord2d new_rc, int quarterTurns) {
        rc.x = new_rc.x;
        rc.y = new_rc.y;
        angle = quarterTurns * Math.PI / 2.0;

        switch (quarterTurns % 4) {
            case 0:
                c[0] = this.ul.add(rc);
                c[1] = Coord2d.of(this.br.x, this.ul.y).add(rc);
                c[2] = this.br.add(rc);
                c[3] = Coord2d.of(this.ul.x, this.br.y).add(rc);
                break;

            case 1:
                c[0] = Coord2d.of(-this.br.y, this.ul.x).add(rc);
                c[1] = Coord2d.of(-this.ul.y, this.ul.x).add(rc);
                c[2] = Coord2d.of(-this.ul.y, this.br.x).add(rc);
                c[3] = Coord2d.of(-this.br.y, this.br.x).add(rc);
                break;

            case 2:
                c[0] = Coord2d.of(-this.br.x, -this.br.y).add(rc);
                c[1] = Coord2d.of(-this.ul.x, -this.br.y).add(rc);
                c[2] = Coord2d.of(-this.ul.x, -this.ul.y).add(rc);
                c[3] = Coord2d.of(-this.br.x, -this.ul.y).add(rc);
                break;

            case 3:
                c[0] = Coord2d.of(this.ul.y, -this.br.x).add(rc);
                c[1] = Coord2d.of(this.br.y, -this.br.x).add(rc);
                c[2] = Coord2d.of(this.br.y, -this.ul.x).add(rc);
                c[3] = Coord2d.of(this.ul.y, -this.ul.x).add(rc);
                break;
        }

        ortho = true;
    }

    public boolean move(Coord2d NewShift, double newAngle) {
        if ((angle != newAngle) || (NewShift != rc)) {
            angle = newAngle;
            rc.x = NewShift.x;
            rc.y = NewShift.y;

            sn = Math.sin(angle);
            cs = Math.cos(angle);

            n[0].x = -sn;
            n[0].y = cs;

            n[1].x = -cs;
            n[1].y = -sn;

            n[2].x = sn;
            n[2].y = -cs;

            n[3].x = cs;
            n[3].y = sn;


            c[0] = ul.rot(angle).add(rc);
            c[1] = Coord2d.of(br.x, ul.y).rot(angle).add(rc);
            c[2] = br.rot(angle).add(rc);
            c[3] = Coord2d.of(ul.x, br.y).rot(angle).add(rc);

            for (int ind = 0; ind < 4; ind++) {
                d[ind] = n[ind].dot(c[ind]);
            }
            this.ortho = false;
            setUpCheckpoints();
            return true;
        }
        return false;
    }

    public void setUpCheckpoints() {
        if (primitive || ortho) return;

        int xCnt = 0;
        int yCnt = 0;
        double xRange, yRange;
        if (!checkPointsInitiated) {
            xRange = c[0].dist(c[1]);
            yRange = c[0].dist(c[3]);
            xCnt = (int) Math.floor(xRange / MCache.tilehsz.x);
            yCnt = (int) Math.floor(yRange / MCache.tilehsz.x);
            if ((checkPoints == null) || (checkPoints.length != (2 * (xCnt + yCnt))) || ((xCnt + yCnt) == 0))
                checkPoints = new Coord2d[2 * (xCnt + yCnt)];
            checkPointsInitiated = true;
        }

        for (int i = 0; i < xCnt; i++) {
            checkPoints[i] = c[0].mul((double) (i + 1) / (xCnt + 1)).add(c[1].mul((double) (xCnt - i) / (xCnt + 1)));
            checkPoints[i + xCnt] = c[3].mul((double) (i + 1) / (xCnt + 1)).add(c[2].mul((double) (xCnt - i) / (xCnt + 1)));
        }
        for (int i = 0; i < yCnt; i++) {
            checkPoints[2 * xCnt + i] = c[0].mul((double) (i + 1) / (xCnt + 1)).add(c[3].mul((double) (xCnt - i) / (xCnt + 1)));
            checkPoints[2 * xCnt + i + yCnt] = c[1].mul((double) (i + 1) / (xCnt + 1)).add(c[2].mul((double) (xCnt - i) / (xCnt + 1)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NHitBoxD)) {
            return (false);
        }
        NHitBoxD a = (NHitBoxD) o;
        return (a.ul.equals(ul) && a.br.equals(br) && a.rc.equals(rc) && (a.angle == this.angle));
    }

    public int hashCode() {
        int X = ul.hashCode() / 2;
        int Y = br.hashCode() / 2;
        return X + Y;
    }

    public int compareTo(NHitBoxD c) {
        return (br == c.br) ? ul.compareTo(c.ul) : br.compareTo(c.br);
    }


    public void reCalc_dv() {

    }

    public Coord2d projectCenter(Coord2d direction) {
        //TODO refactor
        double tau = Float.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            if (Math.abs(direction.dot(this.n[k])) > 0.0001) {
                double tauTemp = Math.min(tau, (d[k] - rc.dot(n[k])) / direction.dot(n[k]));
                if (tauTemp > 0) tau = tauTemp;
            }
        }
        return rc.add(direction.mul(tau));
    }

    public Coord2d getCircumscribedUL() {
        //TODO refactor
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
        //TODO refactor
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
        return br.dist(ul);
    }

    public Coord2d sz() {
        return br.sub(ul);
    }

    public boolean containsSemiOpen(Coord2d c) {
        if (!ortho) {
            return (n[0].dot(c) >= d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) >= d[3]);
        } else {
            return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean contains(Coord2d c, boolean includeBorder) {
        if (!ortho) {
            if (includeBorder)
                return (n[0].dot(c) >= d[0]) && (n[1].dot(c) >= d[1]) && (n[2].dot(c) >= d[2]) && (n[3].dot(c) >= d[3]);
            else
                return (n[0].dot(c) > d[0]) && (n[1].dot(c) > d[1]) && (n[2].dot(c) > d[2]) && (n[3].dot(c) > d[3]);
        } else {
            if (includeBorder)
                return ((c.x >= this.c[0].x) && (c.y >= this.c[0].y) && (c.x <= this.c[2].x) && (c.y <= this.c[2].y));
            else
                return ((c.x > this.c[0].x) && (c.y > this.c[0].y) && (c.x < this.c[2].x) && (c.y < this.c[2].y));
        }
    }

    public boolean intersects(NHitBoxD other, boolean includeBorder) {
        if (this.ortho) {
            if (other.ortho) {
                if (includeBorder)
                    return ((other.c[2].x >= this.c[0].x) &&
                            (other.c[0].x <= this.c[2].x) &&
                            (other.c[2].y >= this.c[0].y) &&
                            (other.c[0].y <= this.c[2].y));
                else
                    return ((other.c[2].x > this.c[0].x) &&
                            (other.c[0].x < this.c[2].x) &&
                            (other.c[2].y > this.c[0].y) &&
                            (other.c[0].y < this.c[2].y));
            }
        } else {
            for (int k = 0; k < 4; k++)
                if (other.contains(this.c[k], includeBorder))
                    return true;
            for (Coord2d checkPoint : this.checkPoints)
                if (other.contains(checkPoint, includeBorder))
                    return true;
        }

        if (!other.ortho) {
            for (int k = 0; k < 4; k++)
                if (this.contains(other.c[k], includeBorder))
                    return true;
            for (Coord2d checkPoint : other.checkPoints)
                if (this.contains(checkPoint, includeBorder))
                    return true;
        }

        return false;
    }
}
