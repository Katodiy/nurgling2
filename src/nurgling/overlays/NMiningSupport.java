package nurgling.overlays;

import haven.*;
import haven.render.*;
import haven.res.lib.tree.*;
import nurgling.*;
import nurgling.overlays.map.*;
import nurgling.pf.Utils;

public class NMiningSupport extends Sprite implements RenderTree.Node
{
    Gob gob;
    public Coord begin;
    public Coord end;

    private boolean [][] data;

    public boolean[][] getData()
    {
        if(isDynamic)
        {
            calcData();
        }
        return data;
    }

    void calcData()
    {
        if(isTree)
        {
            TreeScale ts = gob.getattr(TreeScale.class);
            if(ts!=null)
            {
                this.r = (int) Math.round(baser * (ts.scale - 0.1) / 0.9);
            }
            else
            {
                this.r = baser;
                isTree = false;
                isDynamic = false;
            }
        }
        Coord a = gob.rc.sub(r, 0).div(MCache.tilesz).round();
        Coord b = gob.rc.sub(0, r).div(MCache.tilesz).round();
        Coord c = gob.rc.add(r, 0).div(MCache.tilesz).round();
        Coord d = gob.rc.add(0, r).div(MCache.tilesz).round();
        begin = new Coord(a.x,b.y);
        end = new Coord(c.x,d.y);

        data = new boolean[c.x-a.x+1][d.y-b.y+1];
        for(int i = 0; i<=c.x-a.x; i++)
        {
            for (int j = 0; j <= d.y-b.y; j++)
            {
                data[i][j] = (gob.rc.dist(new Coord2d(i+begin.x,j+begin.y).mul(MCache.tilesz).add(MCache.tilehsz))<r);
            }
        }
    }

    public NMiningSupport(Owner owner, int r)
    {
        super(owner, null);
        this.gob = (Gob)owner;
        this.r = r;
        calcData();
        isDynamic = gob.id == -1;
        TreeScale ts = gob.getattr(TreeScale.class);
        if(ts!=null)
        {
            this.baser = r;
            isDynamic = true;
            isTree = true;
        }
    }
    int r;
    int baser;
    boolean isTree = false;
    boolean isDynamic = false;
    NMiningOverlay mo = null;

    @Override
    public boolean tick(double dt)
    {
        if(mo == null)
        {
            mo = NMapView.getMiningOl();
            if(mo!=null)
                if(gob.id!=-1)
                {
                    mo.addMineSupp(gob.id);
                }
                else
                {
                    mo.addDummySupp(gob);
                }
        }
        return false;
    }

    @Override
    public void removed(RenderTree.Slot slot)
    {
        super.removed(slot);
        if(gob.id == -1)
        {
            mo.dummy = null;
        }
    }
}
