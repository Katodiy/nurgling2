package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.NMapView;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.widgets.Specialisation;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NAreaLabel extends Sprite implements RenderTree.Node, PView.Render2D{
    private boolean isSelected = false;
    protected Coord3f pos;
    public TexI label = null;
    public TexI sellabel = null;
    protected TexI img = null;
    NArea area;
    public Coord sc;
    boolean forced = false;
    int sizeSpec;
    public NAreaLabel(Owner owner, NArea area) {
        super(owner, null);
        pos = new Coord3f(0,0,2);
        this.area = area;
        sizeSpec = area.spec.size();
        update();
    }


    public void update()
    {
        BufferedImage img = NStyle.openings.render(area.name).img;
        BufferedImage selimg = NStyle.selopenings.render(area.name).img;
        if(!area.spec.isEmpty()) {
            BufferedImage first = Specialisation.findSpecialisation(area.spec.get(0).name).image;
            BufferedImage ret = TexI.mkbuf(new Coord(32, 32));
            Graphics g = ret.getGraphics();
            g.drawImage(first, 0, 0, UI.scale(32), UI.scale(32), null);
            first = ret;
            if (area.spec.size() > 1) {

                for (int i = 1; i < area.spec.size(); i++) {
                    first = ItemInfo.catimgsh(UI.scale(5), first, UI.scale(32, 32), Specialisation.findSpecialisation(area.spec.get(i).name).image);
                }
            }
            img = ItemInfo.catimgsh(UI.scale(5), img, first);
            selimg = ItemInfo.catimgsh(UI.scale(5), selimg, first);
        }
        label = new TexI(img);
        sellabel = new TexI(selimg);
    }

    @Override
    public boolean tick(double dt) {
        if(NUtils.getGameUI()!=null) {
            isSelected = NUtils.getGameUI().areas.al.sel.area == area;
            if (NUtils.getGameUI() != null) {
                if (area.spec.size() != sizeSpec) {
                    sizeSpec = area.spec.size();
                    update();

                }
                return NUtils.findGob(((Gob) owner).id) == null;
            }
        }
        return true;
    }

    @Override
    public void draw(GOut g, Pipe state) {
        sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
        if (label != null)
            if(isSelected)
            {
                g.aimage(sellabel, sc, 0.5, 0.5);
            }
            else {
                g.aimage(label, sc, 0.5, 0.5);
            }
    }

    public boolean isect(Coord pc) {
        if(sc == null)
            return false;
        Coord ul = sc.sub(label.sz().div(2));
        return pc.isect(ul, label.sz());
    }
}
