package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NDMGOverlay extends Sprite implements PView.Render2D {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);
    Color[] colt = new Color[]{Color.RED, Color.YELLOW, Color.GREEN};
    TexI[] dmgt = new TexI[3];
    int[] dmg = new int[3];
    BufferedImage curOl = null;

    public NDMGOverlay(Owner owner) {
        super(owner, null);
    }

    public static void IsDMG(Message sdt, Gob g) {
        if (sdt.rt == 7) {
            MessageBuf buf = new MessageBuf(sdt);
            int dmg = buf.int32();
            buf.uint8();
            int type = buf.uint16();

        }
    }

    public static void IsDMG(int col, int num, Gob owner) {
        if (col == 64527 || col == 36751 || col == 61455) {
            NDMGOverlay ol;
            Gob.Overlay gol = owner.findol(NDMGOverlay.class);
            if(gol != null) {
                ol = (NDMGOverlay)gol.spr;
            }
            else
            {
                ol = new NDMGOverlay(owner);
                ((Gob)owner).addcustomol(ol);
            }
            if (col == 64527) {
                ol.updDmg(num,1);
            }
            else if (col == 36751) {
                ol.updDmg(num,2);
            }
            else {
                ol.updDmg(num,0);
            }
        }
    }

    public void updDmg(int dmg, int type) {
        this.dmg[type] += dmg;
        dmgt[type] = new TexI(Utils.outline2(fnd.render(Integer.toString(this.dmg[type]), colt[type]).img, Utils.contrast(colt[type])));
        int w = 0;
        int h = 0;
        for(int i = 0; i < 3; i++) {
            if (dmgt[i] != null) {
                w += dmgt[i].sz().x + UI.scale(2);
                h = dmgt[i].sz().y + UI.scale(2);
            }
        }
        BufferedImage ret = TexI.mkbuf(new Coord(w, h));
        Graphics g = ret.getGraphics();
        Coord pos = new Coord(0, 0);
        for(int i = 0; i < 3; i++) {
            if(dmgt[i] != null) {
                g.drawImage(dmgt[i].back, pos.x, pos.y, null);
                pos.x += dmgt[i].sz().x + UI.scale(2);
            }
        }
        g.dispose();
        curOl = ret;
    }

    public void draw(GOut g, Pipe state) {
        Coord sc = Homo3D.obj2view(Coord3f.zu.add(0,0, 16), state, Area.sized(Coord.z, g.sz())).round2();
        if(sc == null || curOl == null) {
            return;
        }
        g.chcolor(new Color(0, 0, 0, 64));
        Coord start = new Coord(curOl.getWidth(),curOl.getHeight()).div(2);
        g.frect2(sc.sub(start), sc.add(new Coord(curOl.getWidth(),curOl.getHeight())).sub(start));
        g.chcolor();
        g.image(curOl,sc.add(UI.scale(1,0)).sub(start));
    }

    @Override
    public boolean tick(double dt) {
        return super.tick(dt);
    }
}
