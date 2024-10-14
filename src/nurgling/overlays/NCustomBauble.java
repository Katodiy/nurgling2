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
    }
}
