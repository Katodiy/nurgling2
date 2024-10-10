package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.NUtils;

public class NTexLabel extends Sprite implements RenderTree.Node, PView.Render2D{
    protected Coord3f pos;
    public TexI label = null;
    protected TexI img = null;
    boolean forced = false;
    public NTexLabel(Owner owner) {
        super(owner, null);
        pos = new Coord3f(0,0,2);
    }

    @Override
    public boolean tick(double dt) {
        return NUtils.findGob(((Gob)owner).id)==null;
    }

    @Override
    public void draw(GOut g, Pipe state) {
        Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
        if (label != null)
            g.aimage(label, sc, 0.5, 0.5);
    }
}
