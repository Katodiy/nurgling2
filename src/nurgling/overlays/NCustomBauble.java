package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class NCustomBauble extends NObjectTexLabel {

    public NCustomBauble(Gob player, BufferedImage image, AtomicBoolean result) {
        super(player);
        gob = (Gob) owner;
        pos = new Coord3f(0,0, 25);
        this.img = new TexI(image);
        this.result = result;
    }

    public NCustomBauble(Gob player, BufferedImage image, BufferedImage image2, AtomicBoolean result) {
        super(player);
        gob = (Gob) owner;
        pos = new Coord3f(0,0, 25);
        this.img = new TexI(image);
        this.img2 = new TexI(image2);
        this.result = result;
    }

    TexI img2 = null;
    Gob gob;
    AtomicBoolean result;

    @Override
    public boolean tick(double dt) {
        return !result.get();
    }


    @Override
    public void draw(GOut g, Pipe state)
    {
        Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();

        g.aimage(img, sc, 0.5,0.5);
        if(img2 != null)
            g.aimage(img2, sc.sub(UI.scale(40,7)), 0.5,0.5);
    }
}
